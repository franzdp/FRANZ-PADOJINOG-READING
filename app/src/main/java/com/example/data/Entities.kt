package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "learners")
data class LearnerEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val gradeLevel: Int,
    val section: String,
    val keyStage: String, // "KS1" (Grades 1-3) or "KS2" (Grades 4-6)
    val preferredTrackId: String,
    val lastModuleId: String? = null,
    val createdAt: Long,
    val lastActiveAt: Long,
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "session_progress",
    primaryKeys = ["learnerId", "moduleId"],
    foreignKeys = [
        ForeignKey(
            entity = LearnerEntity::class,
            parentColumns = ["id"],
            childColumns = ["learnerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("learnerId"), Index("trackId")],
)
data class SessionProgressEntity(
    val learnerId: String,
    val moduleId: String,
    val trackId: String,
    val weekNumber: Int,
    val sessionOrder: Int,
    val totalQuestions: Int,
    val bestScore: Int,
    val lastScore: Int,
    val firstTryCorrectCount: Int,
    val attemptsCount: Int,
    val badgeTier: Int,
    val completedAt: Long,
    val updatedAt: Long,
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "wrong_items",
    primaryKeys = ["learnerId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = LearnerEntity::class,
            parentColumns = ["id"],
            childColumns = ["learnerId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("learnerId"), Index("trackId")],
)
data class WrongItemEntity(
    val learnerId: String,
    val itemId: String,
    val trackId: String,
    val moduleId: String,
    val moduleTitle: String,
    val promptSummary: String,
    val expectedAnswer: String,
    val lastWrongAnswer: String,
    val wrongCount: Int,
    val repeatedWrongCount: Int,
    val lastMissedAt: Long,
)
