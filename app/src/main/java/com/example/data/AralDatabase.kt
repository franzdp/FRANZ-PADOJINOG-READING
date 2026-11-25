package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LearnerEntity::class,
        SessionProgressEntity::class,
        WrongItemEntity::class,
        InProgressSessionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AralDatabase : RoomDatabase() {
    abstract fun aralDao(): AralDao

    companion object {
        private const val DB_NAME = "aral_reading_bagacay_es.db"

        @Volatile
        private var INSTANCE: AralDatabase? = null

        /**
         * Upgrade-safe migration chain. Never call fallbackToDestructiveMigration()
         * so learner mastery badges and repeated-wrong histories survive APK updates.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `in_progress_sessions` (
                        `learnerId` TEXT NOT NULL,
                        `moduleId` TEXT NOT NULL,
                        `trackId` TEXT NOT NULL,
                        `currentIndex` INTEGER NOT NULL,
                        `attemptNumber` INTEGER NOT NULL,
                        `selectedOption` TEXT,
                        `isCurrentSolved` INTEGER NOT NULL,
                        `isCurrentFailedBothTries` INTEGER NOT NULL,
                        `showHintBanner` INTEGER NOT NULL,
                        `showMiniLessonBanner` INTEGER NOT NULL,
                        `correctCount` INTEGER NOT NULL,
                        `firstTryCorrectCount` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`learnerId`, `moduleId`),
                        FOREIGN KEY(`learnerId`) REFERENCES `learners`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_in_progress_sessions_learnerId` ON `in_progress_sessions` (`learnerId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_in_progress_sessions_trackId` ON `in_progress_sessions` (`trackId`)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Reserved no-op scaffold for future schema version 3 additions
            }
        }

        fun getInstance(context: Context): AralDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AralDatabase::class.java,
                    DB_NAME,
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
