package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import com.example.util.NotificationHelper
import com.example.util.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ScheduleRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val scheduleDao = database.scheduleDao()
    private val notificationHelper = NotificationHelper(context)
    private val prefs = PreferencesManager(context)

    val allSchedules: Flow<List<ScheduleItem>> = scheduleDao.getAllSchedules()

    fun getSchedulesByDay(day: Int): Flow<List<ScheduleItem>> {
        return scheduleDao.getSchedulesByDay(day)
    }

    suspend fun insert(item: ScheduleItem): Long {
        val id = scheduleDao.insertSchedule(item)
        val insertedItem = item.copy(id = id)
        if (insertedItem.isReminderEnabled && prefs.isNotificationsEnabled()) {
            notificationHelper.scheduleAlarm(insertedItem)
        }
        return id
    }

    suspend fun insertAll(items: List<ScheduleItem>) {
        val ids = scheduleDao.insertSchedules(items)
        if (prefs.isNotificationsEnabled()) {
            items.forEachIndexed { index, item ->
                val id = ids.getOrNull(index) ?: item.id
                val insertedItem = item.copy(id = id)
                if (insertedItem.isReminderEnabled) {
                    notificationHelper.scheduleAlarm(insertedItem)
                }
            }
        }
    }

    suspend fun update(item: ScheduleItem) {
        scheduleDao.updateSchedule(item)
        if (item.isReminderEnabled && prefs.isNotificationsEnabled()) {
            notificationHelper.scheduleAlarm(item)
        } else {
            notificationHelper.cancelAlarm(item)
        }
    }

    suspend fun delete(item: ScheduleItem) {
        notificationHelper.cancelAlarm(item)
        scheduleDao.deleteSchedule(item)
    }

    suspend fun clearAll() {
        val currentItems = scheduleDao.getAllSchedules().firstOrNull() ?: emptyList()
        currentItems.forEach { notificationHelper.cancelAlarm(it) }
        scheduleDao.clearAll()
    }

    suspend fun seedSampleDataIfEmpty() {
        val existing = scheduleDao.getAllSchedules().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val samples = listOf(
                ScheduleItem(
                    title = "Giải Tích 1",
                    lecturer = "TS. Trần Văn Hải",
                    room = "A2-301",
                    dayOfWeek = 1, // Thứ 2
                    startTime = "07:30",
                    endTime = "09:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#2563EB",
                    notes = "Mang theo giáo trình bài tập",
                    reminderMinutesBefore = 15,
                    isReminderEnabled = true
                ),
                ScheduleItem(
                    title = "Lập Trình Di Động (Android)",
                    lecturer = "ThS. Lê Hoàng Nam",
                    room = "Lab 402 CNTT",
                    dayOfWeek = 1, // Thứ 2
                    startTime = "13:30",
                    endTime = "16:00",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#10B981",
                    notes = "Thực hành Jetpack Compose & Gemini AI",
                    reminderMinutesBefore = 15,
                    isReminderEnabled = true
                ),
                ScheduleItem(
                    title = "Cấu Trúc Dữ Liệu & Giải Thuật",
                    lecturer = "PGS. TS. Nguyễn Đình Quân",
                    room = "B1-105",
                    dayOfWeek = 2, // Thứ 3
                    startTime = "08:00",
                    endTime = "10:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#8B5CF6",
                    notes = "Kiểm tra 15 phút đầu giờ",
                    reminderMinutesBefore = 30,
                    isReminderEnabled = true
                ),
                ScheduleItem(
                    title = "Tiếng Anh Chuyên Ngành",
                    lecturer = "Ms. Emily Parker",
                    room = "C3-202",
                    dayOfWeek = 3, // Thứ 4
                    startTime = "09:00",
                    endTime = "11:15",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#F59E0B",
                    notes = "Thuyết trình chủ đề AI in Education",
                    reminderMinutesBefore = 15,
                    isReminderEnabled = true
                ),
                ScheduleItem(
                    title = "Hệ Quản Trị Cơ Sở Dữ Liệu",
                    lecturer = "TS. Phạm Minh Tuấn",
                    room = "Phòng Máy 3",
                    dayOfWeek = 4, // Thứ 5
                    startTime = "13:00",
                    endTime = "15:30",
                    repeatType = RepeatType.WEEKLY.name,
                    colorHex = "#EC4899",
                    notes = "SQL, Room Database & indexing",
                    reminderMinutesBefore = 15,
                    isReminderEnabled = true
                ),
                ScheduleItem(
                    title = "Họp Định Kỳ & Báo Cáo Đồ Án",
                    lecturer = "GVHD Khoa CNTT",
                    room = "Hội trường Thư Viện",
                    dayOfWeek = 5, // Thứ 6
                    startTime = "15:00",
                    endTime = "17:00",
                    repeatType = RepeatType.MONTHLY.name,
                    colorHex = "#06B6D4",
                    notes = "Lặp lại hàng tháng để tổng kết tiến độ",
                    reminderMinutesBefore = 30,
                    isReminderEnabled = true
                )
            )
            insertAll(samples)
        }
    }
}
