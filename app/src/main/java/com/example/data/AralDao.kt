package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AralDao {

    @Query("SELECT * FROM learners ORDER BY lastActiveAt DESC")
    fun observeLearners(): Flow<List<LearnerEntity>>

    @Query("SELECT * FROM learners WHERE id = :learnerId LIMIT 1")
    fun observeLearner(learnerId: String): Flow<LearnerEntity?>

    @Query("SELECT * FROM learners WHERE id = :learnerId LIMIT 1")
    suspend fun getLearnerOnce(learnerId: String): LearnerEntity?

    @Query("SELECT * FROM learners ORDER BY lastActiveAt DESC")
    suspend fun getAllLearnersOnce(): List<LearnerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearner(learner: LearnerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearners(learners: List<LearnerEntity>)

    @Query(
        "UPDATE learners SET lastModuleId = :moduleId, preferredTrackId = :trackId, lastActiveAt = :timestamp WHERE id = :learnerId",
    )
    suspend fun updateResumePoint(
        learnerId: String,
        trackId: String,
        moduleId: String?,
        timestamp: Long,
    )

    @Query("SELECT * FROM session_progress WHERE learnerId = :learnerId ORDER BY trackId, weekNumber, sessionOrder")
    fun observeProgressForLearner(learnerId: String): Flow<List<SessionProgressEntity>>

    @Query("SELECT * FROM session_progress WHERE learnerId = :learnerId")
    suspend fun getProgressForLearnerOnce(learnerId: String): List<SessionProgressEntity>

    @Query("SELECT * FROM session_progress")
    fun observeAllProgress(): Flow<List<SessionProgressEntity>>

    @Query("SELECT * FROM session_progress")
    suspend fun getAllProgressOnce(): List<SessionProgressEntity>

    @Query("SELECT * FROM session_progress WHERE learnerId = :learnerId AND moduleId = :moduleId LIMIT 1")
    suspend fun getSessionProgressOnce(learnerId: String, moduleId: String): SessionProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSessionProgress(progress: SessionProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllSessionProgress(items: List<SessionProgressEntity>)

    @Query(
        "SELECT * FROM wrong_items WHERE learnerId = :learnerId ORDER BY repeatedWrongCount DESC, wrongCount DESC, lastMissedAt DESC",
    )
    fun observeWrongItemsForLearner(learnerId: String): Flow<List<WrongItemEntity>>

    @Query(
        "SELECT * FROM wrong_items WHERE learnerId = :learnerId ORDER BY repeatedWrongCount DESC, wrongCount DESC, lastMissedAt DESC",
    )
    suspend fun getWrongItemsForLearnerOnce(learnerId: String): List<WrongItemEntity>

    @Query("SELECT * FROM wrong_items")
    fun observeAllWrongItems(): Flow<List<WrongItemEntity>>

    @Query("SELECT * FROM wrong_items")
    suspend fun getAllWrongItemsOnce(): List<WrongItemEntity>

    @Query("SELECT * FROM wrong_items WHERE learnerId = :learnerId AND itemId = :itemId LIMIT 1")
    suspend fun getWrongItemOnce(learnerId: String, itemId: String): WrongItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWrongItem(item: WrongItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWrongItems(items: List<WrongItemEntity>)

    @Query("SELECT * FROM in_progress_sessions")
    fun observeAllInProgress(): Flow<List<InProgressSessionEntity>>

    @Query("SELECT * FROM in_progress_sessions")
    suspend fun getAllInProgressOnce(): List<InProgressSessionEntity>

    @Query("SELECT * FROM in_progress_sessions WHERE learnerId = :learnerId AND moduleId = :moduleId LIMIT 1")
    suspend fun getInProgressOnce(learnerId: String, moduleId: String): InProgressSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInProgress(session: InProgressSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllInProgress(items: List<InProgressSessionEntity>)

    @Query("DELETE FROM in_progress_sessions WHERE learnerId = :learnerId AND moduleId = :moduleId")
    suspend fun deleteInProgress(learnerId: String, moduleId: String)

    @Query(
        "UPDATE learners SET fullName = :fullName, gradeLevel = :gradeLevel, section = :section, keyStage = :keyStage WHERE id = :learnerId",
    )
    suspend fun updateLearnerProfile(
        learnerId: String,
        fullName: String,
        gradeLevel: Int,
        section: String,
        keyStage: String,
    )

    @Query("DELETE FROM learners WHERE id = :learnerId")
    suspend fun deleteLearner(learnerId: String)

    @Transaction
    suspend fun restoreMerge(
        learners: List<LearnerEntity>,
        progressList: List<SessionProgressEntity>,
        wrongItems: List<WrongItemEntity>,
        inProgressList: List<InProgressSessionEntity> = emptyList(),
    ) {
        if (learners.isNotEmpty()) {
            upsertLearners(learners)
        }
        for (incoming in progressList) {
            val existing = getSessionProgressOnce(incoming.learnerId, incoming.moduleId)
            if (existing == null) {
                upsertSessionProgress(incoming)
            } else {
                val merged = existing.copy(
                    bestScore = maxOf(existing.bestScore, incoming.bestScore),
                    lastScore = if (incoming.updatedAt >= existing.updatedAt) incoming.lastScore else existing.lastScore,
                    firstTryCorrectCount = maxOf(existing.firstTryCorrectCount, incoming.firstTryCorrectCount),
                    attemptsCount = maxOf(existing.attemptsCount, incoming.attemptsCount),
                    badgeTier = maxOf(existing.badgeTier, incoming.badgeTier),
                    completedAt = maxOf(existing.completedAt, incoming.completedAt),
                    updatedAt = maxOf(existing.updatedAt, incoming.updatedAt),
                )
                upsertSessionProgress(merged)
            }
        }
        for (incomingWrong in wrongItems) {
            val existing = getWrongItemOnce(incomingWrong.learnerId, incomingWrong.itemId)
            if (existing == null) {
                upsertWrongItem(incomingWrong)
            } else {
                val merged = existing.copy(
                    wrongCount = maxOf(existing.wrongCount, incomingWrong.wrongCount),
                    repeatedWrongCount = maxOf(existing.repeatedWrongCount, incomingWrong.repeatedWrongCount),
                    lastWrongAnswer = if (incomingWrong.lastMissedAt >= existing.lastMissedAt) {
                        incomingWrong.lastWrongAnswer
                    } else {
                        existing.lastWrongAnswer
                    },
                    lastMissedAt = maxOf(existing.lastMissedAt, incomingWrong.lastMissedAt),
                )
                upsertWrongItem(merged)
            }
        }
        for (incomingInProgress in inProgressList) {
            val existing = getInProgressOnce(incomingInProgress.learnerId, incomingInProgress.moduleId)
            if (existing == null || incomingInProgress.updatedAt >= existing.updatedAt) {
                upsertInProgress(incomingInProgress)
            }
        }
    }
}
