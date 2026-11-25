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
    ],
    version = 1,
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
                // Reserved no-op scaffold for future schema version 2 additions
            }
        }

        fun getInstance(context: Context): AralDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AralDatabase::class.java,
                    DB_NAME,
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
