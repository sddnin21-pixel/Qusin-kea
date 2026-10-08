package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.GradeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grade_records ORDER BY subjectTitle ASC")
    fun getAllGrades(): Flow<List<GradeRecord>>

    @Query("SELECT * FROM grade_records WHERE scheduleId = :scheduleId LIMIT 1")
    suspend fun getGradeForSchedule(scheduleId: Long): GradeRecord?

    @Query("SELECT * FROM grade_records WHERE scheduleId = :scheduleId LIMIT 1")
    fun observeGradeForSchedule(scheduleId: Long): Flow<GradeRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGrade(grade: GradeRecord): Long

    @Update
    suspend fun updateGrade(grade: GradeRecord)

    @Delete
    suspend fun deleteGrade(grade: GradeRecord)

    @Query("DELETE FROM grade_records WHERE scheduleId = :scheduleId")
    suspend fun deleteForSchedule(scheduleId: Long)

    @Query("DELETE FROM grade_records")
    suspend fun clearAllGrades()
}
