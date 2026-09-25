package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.example.data.model.DayOfWeekHelper
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GoogleCalendarHelper {

    fun openInsertInCalendarIntent(context: Context, item: ScheduleItem): Boolean {
        val (startMillis, endMillis) = calculateNextOccurrence(item)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, item.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, item.room)
            val desc = buildString {
                if (item.lecturer.isNotBlank()) append("Giảng viên: ${item.lecturer}\n")
                if (item.notes.isNotBlank()) append("Ghi chú: ${item.notes}\n")
                append("Tạo tự động từ Thời Khóa Biểu AI")
            }
            putExtra(CalendarContract.Events.DESCRIPTION, desc)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.HAS_ALARM, if (item.isReminderEnabled) 1 else 0)

            val rrule = when (item.repeatType) {
                RepeatType.WEEKLY.name -> "FREQ=WEEKLY;BYDAY=${DayOfWeekHelper.getRruleDayCode(item.dayOfWeek)}"
                RepeatType.MONTHLY.name -> "FREQ=MONTHLY"
                RepeatType.YEARLY.name -> "FREQ=YEARLY"
                else -> null
            }
            if (rrule != null) {
                putExtra(CalendarContract.Events.RRULE, rrule)
            }
        }

        // Try explicitly with Google Calendar first, then generic fallback
        try {
            val googleCalIntent = Intent(intent).apply {
                setPackage("com.google.android.calendar")
            }
            if (googleCalIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(googleCalIntent)
                return true
            }
        } catch (_: Exception) {}

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Direct sync into Android/Google Calendar Provider using READ_CALENDAR and WRITE_CALENDAR permissions
     */
    fun syncDirectToGoogleCalendar(context: Context, items: List<ScheduleItem>): Pair<Int, String> {
        val resolver = context.contentResolver

        // Find best calendar (prefer Google account calendar)
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY
        )

        var targetCalendarId: Long = -1
        try {
            val cursor = resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                var foundGoogle = false
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                    val accountType = it.getString(it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)) ?: ""

                    if (accountType.contains("google", ignoreCase = true)) {
                        targetCalendarId = id
                        foundGoogle = true
                        break
                    }
                    if (targetCalendarId == -1L) {
                        targetCalendarId = id
                    }
                }
            }
        } catch (e: SecurityException) {
            return Pair(0, "Cần cấp quyền truy cập Lịch trong Cài đặt để đồng bộ trực tiếp.")
        } catch (e: Exception) {
            return Pair(0, "Lỗi kiểm tra lịch: ${e.message}")
        }

        if (targetCalendarId == -1L) {
            return Pair(0, "Không tìm thấy lịch Google nào trên thiết bị.")
        }

        var syncedCount = 0
        val timeZoneId = TimeZone.getDefault().id

        for (item in items) {
            val (startMillis, endMillis) = calculateNextOccurrence(item)
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.TITLE, item.title)
                put(CalendarContract.Events.DESCRIPTION, buildString {
                    if (item.lecturer.isNotBlank()) append("Giảng viên: ${item.lecturer}\n")
                    if (item.notes.isNotBlank()) append("Ghi chú: ${item.notes}\n")
                    append("Đồng bộ từ Thời Khóa Biểu AI")
                })
                put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, timeZoneId)
                put(CalendarContract.Events.EVENT_LOCATION, item.room)
                put(CalendarContract.Events.HAS_ALARM, if (item.isReminderEnabled) 1 else 0)

                val rrule = when (item.repeatType) {
                    RepeatType.WEEKLY.name -> "FREQ=WEEKLY;BYDAY=${DayOfWeekHelper.getRruleDayCode(item.dayOfWeek)}"
                    RepeatType.MONTHLY.name -> "FREQ=MONTHLY"
                    RepeatType.YEARLY.name -> "FREQ=YEARLY"
                    else -> null
                }
                if (rrule != null) {
                    put(CalendarContract.Events.RRULE, rrule)
                }
            }

            try {
                val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (uri != null) {
                    syncedCount++
                    // Add reminder
                    if (item.isReminderEnabled && item.reminderMinutesBefore > 0) {
                        val eventId = uri.lastPathSegment?.toLongOrNull()
                        if (eventId != null) {
                            val reminderValues = ContentValues().apply {
                                put(CalendarContract.Reminders.EVENT_ID, eventId)
                                put(CalendarContract.Reminders.MINUTES, item.reminderMinutesBefore)
                                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                            }
                            resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return Pair(syncedCount, "Đã đồng bộ thành công $syncedCount môn học vào Lịch Google!")
    }

    /**
     * Export all items into an iCalendar (.ics) file that can be shared or imported directly
     */
    fun shareIcsExport(context: Context, items: List<ScheduleItem>): Boolean {
        if (items.isEmpty()) return false

        val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }

        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//Thoi Khoa Bieu AI//VN\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")

        for (item in items) {
            val (startMillis, endMillis) = calculateNextOccurrence(item)
            val uid = "${System.currentTimeMillis()}-${item.id}@thoikhoabieu.ai"

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:$uid\r\n")
            sb.append("DTSTAMP:${dateFormat.format(Date())}\r\n")
            sb.append("DTSTART:${dateFormat.format(Date(startMillis))}\r\n")
            sb.append("DTEND:${dateFormat.format(Date(endMillis))}\r\n")
            sb.append("SUMMARY:${item.title}\r\n")
            if (item.room.isNotBlank()) {
                sb.append("LOCATION:${item.room}\r\n")
            }
            sb.append("DESCRIPTION:Giảng viên: ${item.lecturer} | Ghi chú: ${item.notes}\r\n")

            when (item.repeatType) {
                RepeatType.WEEKLY.name -> {
                    sb.append("RRULE:FREQ=WEEKLY;BYDAY=${DayOfWeekHelper.getRruleDayCode(item.dayOfWeek)}\r\n")
                }
                RepeatType.MONTHLY.name -> {
                    sb.append("RRULE:FREQ=MONTHLY\r\n")
                }
                RepeatType.YEARLY.name -> {
                    sb.append("RRULE:FREQ=YEARLY\r\n")
                }
            }

            if (item.isReminderEnabled) {
                sb.append("BEGIN:VALARM\r\n")
                sb.append("TRIGGER:-PT${item.reminderMinutesBefore}M\r\n")
                sb.append("ACTION:DISPLAY\r\n")
                sb.append("DESCRIPTION:Nhắc nhở học: ${item.title}\r\n")
                sb.append("END:VALARM\r\n")
            }

            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")

        return try {
            val cacheDir = context.cacheDir
            val icsFile = File(cacheDir, "thoi_khoa_bieu.ics")
            icsFile.writeText(sb.toString(), Charsets.UTF_8)

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                icsFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Thời khóa biểu học tập (.ics)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Nhập vào Google Calendar hoặc chia sẻ lịch"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun calculateNextOccurrence(item: ScheduleItem): Pair<Long, Long> {
        val parts = item.startTime.split(":")
        val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val startMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val endParts = item.endTime.split(":")
        val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: (startHour + 2)
        val endMin = endParts.getOrNull(1)?.toIntOrNull() ?: startMin

        val calDayOfWeek = when (item.dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        val startCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_WEEK, calDayOfWeek)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val endCal = Calendar.getInstance().apply {
            timeInMillis = startCal.timeInMillis
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMin)
            if (timeInMillis <= startCal.timeInMillis) {
                add(Calendar.MINUTE, 90)
            }
        }

        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }
}
