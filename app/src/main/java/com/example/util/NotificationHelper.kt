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

    fun showInstantTestNotification() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                ScheduleAlarmReceiver.CHANNEL_ID,
                ScheduleAlarmReceiver.CHANNEL_NAME,
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo chuông nhắc nhở thời khóa biểu"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            99999,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        val notification = androidx.core.app.NotificationCompat.Builder(context, ScheduleAlarmReceiver.CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle("🔔 [Thử Nghiệm] Nhắc nhở giờ học!")
            .setContentText("Môn: Lập Trình Di Động (Android Kotlin) - Phòng Lab 4.2 lúc 08:00")
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("Môn học Lập Trình Di Động (Android Kotlin) sắp bắt đầu lúc 08:00 tại Phòng Lab 4.2. Hãy chuẩn bị bài vở và laptop!"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(99999, notification)
    }
}
