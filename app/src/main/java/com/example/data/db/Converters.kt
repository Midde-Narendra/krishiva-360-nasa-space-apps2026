package com.example.data.db

import androidx.room.TypeConverter
import com.example.domain.models.AlertPriority
import com.example.domain.models.AlertType
import com.example.domain.models.DataSource
import com.example.domain.models.NutrientRating

class Converters {
    @TypeConverter
    fun fromDataSource(source: DataSource?): String? = source?.name

    @TypeConverter
    fun toDataSource(value: String?): DataSource {
        if (value == null) return DataSource.ESP32_HARDWARE
        if (value == "DEMO") return DataSource.LEGACY_SAMPLE
        return try {
            DataSource.valueOf(value)
        } catch (e: Exception) {
            DataSource.LEGACY_SAMPLE
        }
    }

    @TypeConverter
    fun fromNutrientRating(rating: NutrientRating?): String? = rating?.name

    @TypeConverter
    fun toNutrientRating(value: String?): NutrientRating? {
        if (value.isNullOrBlank()) return null
        return try {
            NutrientRating.valueOf(value)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromAlertType(type: AlertType): String = type.name

    @TypeConverter
    fun toAlertType(value: String): AlertType = try {
        AlertType.valueOf(value)
    } catch (e: Exception) {
        AlertType.SOIL
    }

    @TypeConverter
    fun fromAlertPriority(priority: AlertPriority): String = priority.name

    @TypeConverter
    fun toAlertPriority(value: String): AlertPriority = try {
        AlertPriority.valueOf(value)
    } catch (e: Exception) {
        AlertPriority.INFO
    }
}
