package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val subjectTitle: String,
    val date: String, // YYYY-MM-DD
    val status: String = AttendanceStatus.PRESENT.name, // PRESENT, ABSENT, EXCUSED
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class AttendanceStatus(val labelVi: String, val colorHex: String) {
    PRESENT("Có mặt", "#10B981"),
    ABSENT("Vắng mặt", "#EF4444"),
    EXCUSED("Có phép", "#F59E0B")
}
