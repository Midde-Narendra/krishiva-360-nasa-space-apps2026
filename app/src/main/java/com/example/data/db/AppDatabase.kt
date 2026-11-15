package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.domain.models.FarmDecision
import com.example.domain.models.SmartAlert
import com.example.domain.models.SoilReading

@Database(
    entities = [SoilReading::class, SmartAlert::class, FarmDecision::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun soilReadingDao(): SoilReadingDao
    abstract fun alertDao(): AlertDao
    abstract fun farmDecisionDao(): FarmDecisionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Explicit non-destructive migration from version 1 to version 2:
         * - Recreates soil_readings with nullable non-moisture columns and new ESP32 telemetry columns.
         * - Converts any old 'DEMO' rows to 'LEGACY_SAMPLE' (never converting them to ESP32_HARDWARE)
         *   so legacy sample rows are excluded from live state while preserving any non-demo history.
         * - Updates farm_decisions so suitabilityPercent is nullable.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `soil_readings_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `moistureRaw` INTEGER,
                        `moisturePercent` REAL,
                        `calibrated` INTEGER NOT NULL DEFAULT 0,
                        `deviceId` TEXT,
                        `sequenceNumber` INTEGER,
                        `wifiRssiDbm` INTEGER,
                        `firmwareVersion` TEXT,
                        `ph` REAL,
                        `temperatureCelsius` REAL,
                        `nitrogen` TEXT,
                        `phosphorus` TEXT,
                        `potassium` TEXT,
                        `sulfur` TEXT,
                        `ecConductivity` REAL,
                        `isValid` INTEGER NOT NULL DEFAULT 1,
                        `source` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `soil_readings_new` (
                        `id`, `timestamp`, `moistureRaw`, `moisturePercent`, `calibrated`,
                        `deviceId`, `sequenceNumber`, `wifiRssiDbm`, `firmwareVersion`,
                        `ph`, `temperatureCelsius`, `nitrogen`, `phosphorus`, `potassium`, `sulfur`,
                        `ecConductivity`, `isValid`, `source`, `note`
                    )
                    SELECT
                        `id`,
                        `timestamp`,
                        NULL,
                        CAST(`moisturePercent` AS REAL),
                        0,
                        NULL,
                        NULL,
                        NULL,
                        NULL,
                        CASE WHEN `source` = 'DEMO' THEN NULL ELSE `ph` END,
                        CASE WHEN `source` = 'DEMO' THEN NULL ELSE `temperatureCelsius` END,
                        CASE WHEN `source` = 'DEMO' THEN NULL ELSE `nitrogen` END,
                        CASE WHEN `source` = 'DEMO' THEN NULL ELSE `phosphorus` END,
                        CASE WHEN `source` = 'DEMO' THEN NULL ELSE `potassium` END,
                        CASE WHEN `source` = 'DEMO' THEN NULL ELSE `sulfur` END,
                        CASE WHEN `source` = 'DEMO' THEN NULL ELSE `ecConductivity` END,
                        1,
                        CASE WHEN `source` = 'DEMO' THEN 'LEGACY_SAMPLE' ELSE `source` END,
                        `note`
                    FROM `soil_readings`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `soil_readings`")
                db.execSQL("ALTER TABLE `soil_readings_new` RENAME TO `soil_readings`")

                // Migrate farm_decisions to allow nullable suitabilityPercent and remove seeded demo decisions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `farm_decisions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `farmName` TEXT NOT NULL,
                        `recommendedCrop` TEXT NOT NULL,
                        `suitabilityPercent` INTEGER,
                        `soilSummary` TEXT NOT NULL,
                        `waterSummary` TEXT NOT NULL,
                        `nutrientSummary` TEXT NOT NULL,
                        `pestSummary` TEXT NOT NULL,
                        `demandSummary` TEXT NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `farm_decisions_new` (
                        `id`, `timestamp`, `farmName`, `recommendedCrop`, `suitabilityPercent`,
                        `soilSummary`, `waterSummary`, `nutrientSummary`, `pestSummary`, `demandSummary`, `notes`
                    )
                    SELECT
                        `id`, `timestamp`, `farmName`, `recommendedCrop`, `suitabilityPercent`,
                        `soilSummary`, `waterSummary`, `nutrientSummary`, `pestSummary`, `demandSummary`, `notes`
                    FROM `farm_decisions`
                    WHERE `farmName` != 'Green Valley Farm'
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `farm_decisions`")
                db.execSQL("ALTER TABLE `farm_decisions_new` RENAME TO `farm_decisions`")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "krishiva360.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
