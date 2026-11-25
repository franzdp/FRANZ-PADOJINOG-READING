package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID
import kotlinx.coroutines.flow.Flow

@JsonClass(generateAdapter = true)
data class AralBackupPayload(
    val schemaVersion: Int = 2,
    val appName: String = "ARAL Reading Bagacay ES",
    val school: String = "Bagacay Elementary School",
    val exportedAt: Long,
    val learners: List<LearnerEntity>,
    val progress: List<SessionProgressEntity>,
    val wrongItems: List<WrongItemEntity>,
    val inProgressSessions: List<InProgressSessionEntity> = emptyList(),
)

data class RestoreSummary(
    val learnersRestored: Int,
    val sessionsRestored: Int,
    val wrongItemsRestored: Int,
)

class AralRepository(
    private val dao: AralDao,
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(AralBackupPayload::class.java).indent("  ")

    fun observeLearners(): Flow<List<LearnerEntity>> = dao.observeLearners()

    fun observeLearner(learnerId: String): Flow<LearnerEntity?> = dao.observeLearner(learnerId)

    fun observeProgress(learnerId: String): Flow<List<SessionProgressEntity>> =
        dao.observeProgressForLearner(learnerId)

    fun observeWrongItems(learnerId: String): Flow<List<WrongItemEntity>> =
        dao.observeWrongItemsForLearner(learnerId)

    fun observeAllProgress(): Flow<List<SessionProgressEntity>> = dao.observeAllProgress()

    fun observeAllWrongItems(): Flow<List<WrongItemEntity>> = dao.observeAllWrongItems()

    fun observeAllInProgress(): Flow<List<InProgressSessionEntity>> = dao.observeAllInProgress()

    suspend fun getInProgressSession(learnerId: String, moduleId: String): InProgressSessionEntity? =
        dao.getInProgressOnce(learnerId, moduleId)

    suspend fun saveInProgressSession(session: InProgressSessionEntity) {
        dao.upsertInProgress(session)
        dao.updateResumePoint(
            learnerId = session.learnerId,
            trackId = session.trackId,
            moduleId = session.moduleId,
            timestamp = session.updatedAt,
        )
    }

    suspend fun clearInProgressSession(learnerId: String, moduleId: String) {
        dao.deleteInProgress(learnerId, moduleId)
    }

    suspend fun checkInLearner(
        fullName: String,
        gradeLevel: Int,
        section: String,
        preferredTrackId: String? = null,
    ): LearnerEntity {
        val cleanName = fullName.trim().replace(Regex("\\s+"), " ")
        val cleanSection = section.trim().replace(Regex("\\s+"), " ").ifBlank { "Section A" }
        val clampedGrade = gradeLevel.coerceIn(1, 6)
        val keyStage = if (clampedGrade <= 3) "KS1" else "KS2"
        val defaultTrack = preferredTrackId ?: if (keyStage == "KS1") "ks1_english" else "ks2_basic"
        val now = System.currentTimeMillis()

        // Match returning learner if same name + grade + section exists (case-insensitive)
        val existing = dao.getAllLearnersOnce().firstOrNull {
            it.fullName.equals(cleanName, ignoreCase = true) &&
                it.gradeLevel == clampedGrade &&
                it.section.equals(cleanSection, ignoreCase = true)
        }

        val learner = existing?.copy(
            preferredTrackId = preferredTrackId ?: existing.preferredTrackId,
            lastActiveAt = now,
        ) ?: LearnerEntity(
            id = UUID.randomUUID().toString(),
            fullName = cleanName,
            gradeLevel = clampedGrade,
            section = cleanSection,
            keyStage = keyStage,
            preferredTrackId = defaultTrack,
            lastModuleId = null,
            createdAt = now,
            lastActiveAt = now,
        )
        dao.upsertLearner(learner)
        return learner
    }

    suspend fun touchLearnerResume(learnerId: String, trackId: String, moduleId: String?) {
        dao.updateResumePoint(
            learnerId = learnerId,
            trackId = trackId,
            moduleId = moduleId,
            timestamp = System.currentTimeMillis(),
        )
    }

    suspend fun recordItemMissedAfterTwoTries(
        learnerId: String,
        trackId: String,
        moduleId: String,
        moduleTitle: String,
        itemId: String,
        promptSummary: String,
        expectedAnswer: String,
        learnerAnswer: String,
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.getWrongItemOnce(learnerId, itemId)
        val updated = if (existing == null) {
            WrongItemEntity(
                learnerId = learnerId,
                itemId = itemId,
                trackId = trackId,
                moduleId = moduleId,
                moduleTitle = moduleTitle,
                promptSummary = promptSummary,
                expectedAnswer = expectedAnswer,
                lastWrongAnswer = learnerAnswer,
                wrongCount = 1,
                repeatedWrongCount = 1, // Missed both tries (Try 1 & Try 2)
                lastMissedAt = now,
            )
        } else {
            existing.copy(
                moduleTitle = moduleTitle,
                promptSummary = promptSummary,
                expectedAnswer = expectedAnswer,
                lastWrongAnswer = learnerAnswer,
                wrongCount = existing.wrongCount + 1,
                repeatedWrongCount = existing.repeatedWrongCount + 1,
                lastMissedAt = now,
            )
        }
        dao.upsertWrongItem(updated)
    }

    suspend fun recordSessionCompletion(
        learnerId: String,
        trackId: String,
        moduleId: String,
        weekNumber: Int,
        sessionOrder: Int,
        totalQuestions: Int,
        correctCount: Int,
        firstTryCorrectCount: Int,
    ): SessionProgressEntity {
        val now = System.currentTimeMillis()
        val existing = dao.getSessionProgressOnce(learnerId, moduleId)
        val newAttempts = (existing?.attemptsCount ?: 0) + 1
        val newBestScore = maxOf(existing?.bestScore ?: 0, correctCount)
        val newFirstTryBest = maxOf(existing?.firstTryCorrectCount ?: 0, firstTryCorrectCount)
        val computedTier = MasteryBadgeTier.fromScore(
            correctCount = newBestScore,
            totalCount = totalQuestions,
            attemptsCount = newAttempts,
        ).tierIndex
        val bestTier = maxOf(existing?.badgeTier ?: 0, computedTier)

        val entity = SessionProgressEntity(
            learnerId = learnerId,
            moduleId = moduleId,
            trackId = trackId,
            weekNumber = weekNumber,
            sessionOrder = sessionOrder,
            totalQuestions = totalQuestions,
            bestScore = newBestScore,
            lastScore = correctCount,
            firstTryCorrectCount = newFirstTryBest,
            attemptsCount = newAttempts,
            badgeTier = bestTier,
            completedAt = existing?.completedAt ?: now,
            updatedAt = now,
        )
        dao.upsertSessionProgress(entity)
        dao.updateResumePoint(
            learnerId = learnerId,
            trackId = trackId,
            moduleId = moduleId,
            timestamp = now,
        )
        return entity
    }

    suspend fun updateLearnerProfile(
        learnerId: String,
        fullName: String,
        gradeLevel: Int,
        section: String,
    ) {
        val cleanName = fullName.trim().replace(Regex("\\s+"), " ")
        if (cleanName.isBlank()) return
        val cleanSection = section.trim().replace(Regex("\\s+"), " ").ifBlank { "Section A" }
        val clampedGrade = gradeLevel.coerceIn(1, 6)
        val keyStage = if (clampedGrade <= 3) "KS1" else "KS2"
        dao.updateLearnerProfile(
            learnerId = learnerId,
            fullName = cleanName,
            gradeLevel = clampedGrade,
            section = cleanSection,
            keyStage = keyStage,
        )
    }

    suspend fun deleteLearner(learnerId: String) {
        dao.deleteLearner(learnerId)
    }

    suspend fun exportBackupJson(): String {
        val payload = AralBackupPayload(
            exportedAt = System.currentTimeMillis(),
            learners = dao.getAllLearnersOnce(),
            progress = dao.getAllProgressOnce(),
            wrongItems = dao.getAllWrongItemsOnce(),
            inProgressSessions = dao.getAllInProgressOnce(),
        )
        return backupAdapter.toJson(payload)
    }

    suspend fun restoreFromBackupJson(json: String): Result<RestoreSummary> {
        return runCatching {
            val payload = backupAdapter.fromJson(json)
                ?: throw IllegalArgumentException("Invalid or empty backup JSON file.")
            if (payload.learners.isEmpty() &&
                payload.progress.isEmpty() &&
                payload.wrongItems.isEmpty() &&
                payload.inProgressSessions.isEmpty()
            ) {
                throw IllegalArgumentException("Backup file contains no learner records.")
            }
            dao.restoreMerge(
                learners = payload.learners,
                progressList = payload.progress,
                wrongItems = payload.wrongItems,
                inProgressList = payload.inProgressSessions,
            )
            RestoreSummary(
                learnersRestored = payload.learners.size,
                sessionsRestored = payload.progress.size,
                wrongItemsRestored = payload.wrongItems.size,
            )
        }
    }
}
