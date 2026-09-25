package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import com.example.receiver.ScheduleAlarmReceiver
import java.util.Calendar

class NotificationHelper(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(item: ScheduleItem) {
        if (!item.isReminderEnabled) {
            cancelAlarm(item)
            return
        }

        val triggerTimeMs = calculateNextTriggerTime(item)
        if (triggerTimeMs <= System.currentTimeMillis()) return

        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(ScheduleAlarmReceiver.EXTRA_ITEM_ID, item.id)
            putExtra(ScheduleAlarmReceiver.EXTRA_TITLE, item.title)
            putExtra(ScheduleAlarmReceiver.EXTRA_ROOM, item.room)
            putExtra(ScheduleAlarmReceiver.EXTRA_START_TIME, item.startTime)
            putExtra(ScheduleAlarmReceiver.EXTRA_MINUTES_BEFORE, item.reminderMinutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // In Android 12+ if exact alarm permission is restricted, fallback to standard alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAlarm(item: ScheduleItem) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNextTriggerTime(item: ScheduleItem): Long {
        val parts = item.startTime.split(":")
        val startHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val startMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // Subtract reminder minutes
            add(Calendar.MINUTE, -item.reminderMinutesBefore)
        }

        // Convert dayOfWeek (1: Mon .. 7: Sun) to Calendar day (Calendar.MONDAY = 2 ... Calendar.SUNDAY = 1)
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

        when (item.repeatType) {
            RepeatType.WEEKLY.name -> {
                target.set(Calendar.DAY_OF_WEEK, calDayOfWeek)
                // If it's already past this week, jump to next week
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            RepeatType.MONTHLY.name -> {
                // If past this month, jump to next month
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.MONTH, 1)
                }
            }
            RepeatType.YEARLY.name -> {
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.YEAR, 1)
                }
            }
            else -> {
                // NONE: just find the next upcoming day of week
                target.set(Calendar.DAY_OF_WEEK, calDayOfWeek)
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
        }

        return target.timeInMillis
    }
}
