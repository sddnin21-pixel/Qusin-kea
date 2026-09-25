package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ScheduleAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "schedule_reminder_channel_v1"
        const val CHANNEL_NAME = "Nhắc nhở Thời Khóa Biểu"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ROOM = "extra_room"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all alarms on device reboot
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val allSchedules = db.scheduleDao().getAllSchedules().firstOrNull() ?: emptyList()
                val helper = NotificationHelper(context)
                allSchedules.forEach { item ->
                    if (item.isReminderEnabled) {
                        helper.scheduleAlarm(item)
                    }
                }
            }
            return
        }

        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Lớp học"
        val room = intent.getStringExtra(EXTRA_ROOM) ?: ""
        val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
        val minutesBefore = intent.getIntExtra(EXTRA_MINUTES_BEFORE, 15)

        showNotification(context, itemId, title, room, startTime, minutesBefore)

        // Reschedule for next occurrence
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val item = db.scheduleDao().getScheduleById(itemId)
            if (item != null && item.isReminderEnabled) {
                val helper = NotificationHelper(context)
                helper.scheduleAlarm(item)
            }
        }
    }

    private fun showNotification(
        context: Context,
        itemId: Long,
        title: String,
        room: String,
        startTime: String,
        minutesBefore: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở giờ vào lớp và sự kiện học tập"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            itemId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomInfo = if (room.isNotBlank()) " tại $room" else ""
        val contentText = "Bắt đầu lúc $startTime$roomInfo (còn $minutesBefore phút nữa)"

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 Nhắc học: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Môn học $title sẽ diễn ra vào lúc $startTime$roomInfo. Hãy chuẩn bị bài vở và đồ dùng học tập!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSoundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(itemId.toInt(), notification)
    }
}
