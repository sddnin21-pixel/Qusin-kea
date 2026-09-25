package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_items ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllSchedules(): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE dayOfWeek = :day ORDER BY startTime ASC")
    fun getSchedulesByDay(day: Int): Flow<List<ScheduleItem>>

    @Query("SELECT * FROM schedule_items WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): ScheduleItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(item: ScheduleItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(items: List<ScheduleItem>): List<Long>

    @Update
    suspend fun updateSchedule(item: ScheduleItem)

    @Delete
    suspend fun deleteSchedule(item: ScheduleItem)

    @Query("DELETE FROM schedule_items WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("DELETE FROM schedule_items")
    suspend fun clearAll()
}
