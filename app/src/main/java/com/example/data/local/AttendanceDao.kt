package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE scheduleId = :scheduleId ORDER BY timestamp DESC")
    fun getAttendanceForSchedule(scheduleId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE scheduleId = :scheduleId AND status = 'ABSENT'")
    fun getAbsentCount(scheduleId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM attendance_records WHERE scheduleId = :scheduleId")
    suspend fun deleteForSchedule(scheduleId: Long)
}
