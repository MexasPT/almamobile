package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.Attendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance_records WHERE timestamp >= :dayStartTimestamp ORDER BY timestamp ASC")
    suspend fun getTodayPunchesSync(dayStartTimestamp: Long): List<Attendance>

    @Query("SELECT * FROM attendance_records WHERE timestamp >= :dayStartTimestamp ORDER BY timestamp ASC")
    fun getTodayPunches(dayStartTimestamp: Long): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendance(id: Long)
}
