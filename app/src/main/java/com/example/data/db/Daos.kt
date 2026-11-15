package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domain.models.FarmDecision
import com.example.domain.models.SmartAlert
import com.example.domain.models.SoilReading
import kotlinx.coroutines.flow.Flow

@Dao
interface SoilReadingDao {
    @Query("SELECT * FROM soil_readings WHERE source != 'LEGACY_SAMPLE' AND isValid = 1 ORDER BY timestamp DESC")
    fun getAllReadings(): Flow<List<SoilReading>>

    @Query("SELECT * FROM soil_readings WHERE source != 'LEGACY_SAMPLE' AND isValid = 1 ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<SoilReading?>

    @Query("SELECT * FROM soil_readings WHERE deviceId = :deviceId AND sequenceNumber = :sequence LIMIT 1")
    suspend fun findByDeviceAndSequence(deviceId: String, sequence: Long): SoilReading?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: SoilReading): Long

    @Query("DELETE FROM soil_readings")
    suspend fun clearAll()
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM smart_alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<SmartAlert>>

    @Query("SELECT COUNT(*) FROM smart_alerts WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: SmartAlert): Long

    @Query("UPDATE smart_alerts SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    @Query("UPDATE smart_alerts SET isRead = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM smart_alerts")
    suspend fun clearAll()
}

@Dao
interface FarmDecisionDao {
    @Query("SELECT * FROM farm_decisions ORDER BY timestamp DESC")
    fun getAllDecisions(): Flow<List<FarmDecision>>

    @Query("SELECT * FROM farm_decisions ORDER BY timestamp DESC LIMIT 1")
    fun getLatestDecision(): Flow<FarmDecision?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(decision: FarmDecision): Long

    @Query("DELETE FROM farm_decisions")
    suspend fun clearAll()
}
