package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.MotivationQuoteAlarmReceiver
import java.util.Calendar

object MotivationNotificationHelper {

    const val CHANNEL_ID = "motivation_quote_channel_3h"
    const val CHANNEL_NAME = "Động Lực Học Tập & THPTQG (Mỗi 3h)"
    const val NOTIFICATION_ID = 607300
    const val ACTION_TRIGGER_3H_MOTIVATION = "com.example.receiver.ACTION_TRIGGER_3H_MOTIVATION"
    private const val REQUEST_CODE_ALARM = 607301

    /**
     * Tạo Kênh Thông Báo cho lời nhắn động lực mỗi 3 giờ
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo câu nói động lực học tập và thi THPTQG định kỳ mỗi 3 giờ (14/10/2026 - 10/06/2028)"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
                enableLights(true)
                lightColor = Color.parseColor("#F59E0B")
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Lập lịch thông báo tự động mỗi 3 giờ tiếp theo trong khoảng 14/10/2026 đến 10/06/2028
     */
    fun scheduleNext3HourAlarm(context: Context) {
        val prefs = PreferencesManager(context)
        if (!prefs.isMotivation3hNotificationEnabled()) {
            cancel3HourAlarm(context)
            return
        }

        val nextTriggerMs = calculateNextTriggerTime()
        if (nextTriggerMs <= 0) {
            // Đã quá ngày 10/06/2028, không đặt lịch nữa
            cancel3HourAlarm(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MotivationQuoteAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_3H_MOTIVATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback nếu quyền báo thức chính xác bị hạn chế trên Android 12+
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMs,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Hủy lịch thông báo mỗi 3 giờ
     */
    fun cancel3HourAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MotivationQuoteAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_3H_MOTIVATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Tính toán thời điểm tiếp theo cho chu kỳ 3 giờ (06:00, 09:00, 12:00, 15:00, 18:00, 21:00...)
     * Giới hạn trong khoảng: 14/10/2026 -> 10/06/2028
     */
    fun calculateNextTriggerTime(nowMs: Long = System.currentTimeMillis()): Long {
        val startCal = Calendar.getInstance().apply {
            set(MotivationQuotesHelper.START_YEAR, MotivationQuotesHelper.START_MONTH, MotivationQuotesHelper.START_DAY, 6, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(MotivationQuotesHelper.END_YEAR, MotivationQuotesHelper.END_MONTH, MotivationQuotesHelper.END_DAY, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }

        // Nếu đã qua mốc 10/06/2028
        if (nowMs > endCal.timeInMillis) {
            return -1L
        }

        // Nếu chưa đến ngày 14/10/2026, đặt lịch phát đầu tiên lúc 06:00 ngày 14/10/2026
        if (nowMs < startCal.timeInMillis) {
            return startCal.timeInMillis
        }

        // Tính mốc tròn 3 tiếng tiếp theo (0, 3, 6, 9, 12, 15, 18, 21)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val nextHour = ((currentHour / 3) + 1) * 3

        cal.set(Calendar.HOUR_OF_DAY, nextHour % 24)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        if (nextHour >= 24 || cal.timeInMillis <= nowMs) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, nextHour % 24)
        }

        return if (cal.timeInMillis <= endCal.timeInMillis) {
            cal.timeInMillis
        } else {
            -1L
        }
    }

    /**
     * Phát thông báo câu nói của ngày hôm nay
     */
    fun showMotivationNotification(context: Context) {
        createNotificationChannel(context)

        val quote = MotivationQuotesHelper.getTodayQuote(context)
        val daysUntilExam = MotivationQuotesHelper.getDaysUntilThptExam()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🌟 [${quote.displayDayTitle}/607] Động Lực Hôm Nay"
        val body = quote.content
        val bigText = "“${quote.content}”\n\n🎯 Chúc bạn một buổi học tràn đầy tập trung và bứt phá! (Hành trình THPTQG 14/10/2026 – 10/06/2028 • Còn $daysUntilExam ngày)"

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .setColor(Color.parseColor("#F59E0B"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Bắn thông báo thử nghiệm ngay lập tức để người dùng kiểm tra âm thanh và hiển thị
     */
    fun showInstantTestNotification(context: Context) {
        showMotivationNotification(context)
    }
}
