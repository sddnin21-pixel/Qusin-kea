package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_items")
data class ScheduleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val lecturer: String = "",
    val room: String = "",
    val dayOfWeek: Int = 1, // 1: Thứ 2, 2: Thứ 3, ..., 6: Thứ 7, 7: Chủ Nhật
    val startTime: String = "08:00", // HH:mm (24h)
    val endTime: String = "09:30",   // HH:mm (24h)
    val repeatType: String = RepeatType.WEEKLY.name, // WEEKLY, MONTHLY, YEARLY, NONE
    val colorHex: String = "#2563EB",
    val notes: String = "",
    val reminderMinutesBefore: Int = 15,
    val isReminderEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RepeatType(val labelVi: String, val rruleFreq: String) {
    WEEKLY("Hàng tuần", "WEEKLY"),
    MONTHLY("Hàng tháng", "MONTHLY"),
    YEARLY("Hàng năm", "YEARLY"),
    NONE("Không lặp lại", "")
}

object DayOfWeekHelper {
    fun getDayNameVi(day: Int): String {
        return when (day) {
            1 -> "Thứ Hai"
            2 -> "Thứ Ba"
            3 -> "Thứ Tư"
            4 -> "Thứ Năm"
            5 -> "Thứ Sáu"
            6 -> "Thứ Bảy"
            7 -> "Chủ Nhật"
            else -> "Thứ Hai"
        }
    }

    fun getShortDayNameVi(day: Int): String {
        return when (day) {
            1 -> "T2"
            2 -> "T3"
            3 -> "T4"
            4 -> "T5"
            5 -> "T6"
            6 -> "T7"
            7 -> "CN"
            else -> "T2"
        }
    }

    fun getRruleDayCode(day: Int): String {
        return when (day) {
            1 -> "MO"
            2 -> "TU"
            3 -> "WE"
            4 -> "TH"
            5 -> "FR"
            6 -> "SA"
            7 -> "SU"
            else -> "MO"
        }
    }
}
