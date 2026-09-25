package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class TimetableCountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TimetableCountdownWidgetProvider::class.java)
            )
            updateWidgetsInternal(context, appWidgetManager, ids)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.widget.ACTION_UPDATE_WIDGET"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, TimetableCountdownWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun updateWidgetsInternal(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppDatabase.getDatabase(context)
                val allSchedules = db.scheduleDao().getAllSchedules().firstOrNull() ?: emptyList()
                val nextClassInfo = findNextUpcomingClass(allSchedules)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_timetable_countdown)

                    // Open App on widget click
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                    if (nextClassInfo != null) {
                        val (item, remainingMs) = nextClassInfo

                        views.setTextViewText(R.id.widget_header_title, "⏳ TIẾT HỌC TIẾP THEO")
                        views.setTextViewText(R.id.widget_class_title, item.title)
                        views.setTextViewText(
                            R.id.widget_room,
                            if (item.room.isNotBlank()) item.room else "Lớp học"
                        )
                        val gv = if (item.lecturer.isNotBlank()) " • GV: ${item.lecturer}" else ""
                        views.setTextViewText(R.id.widget_time_lecturer, "${item.startTime} - ${item.endTime}$gv")

                        views.setViewVisibility(R.id.widget_countdown_chronometer, View.VISIBLE)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            views.setChronometerCountDown(R.id.widget_countdown_chronometer, true)
                        }
                        val baseTime = SystemClock.elapsedRealtime() + remainingMs
                        views.setChronometer(
                            R.id.widget_countdown_chronometer,
                            baseTime,
                            "%tM:%tS",
                            true
                        )
                    } else {
                        views.setTextViewText(R.id.widget_header_title, "✨ THỜI KHÓA BIỂU")
                        views.setTextViewText(R.id.widget_class_title, "Hôm nay không còn tiết học")
                        views.setTextViewText(R.id.widget_room, "Nghỉ ngơi")
                        views.setTextViewText(R.id.widget_time_lecturer, "Chạm vào để mở lịch học đầy đủ")
                        views.setViewVisibility(R.id.widget_countdown_chronometer, View.GONE)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        private fun findNextUpcomingClass(schedules: List<ScheduleItem>): Pair<ScheduleItem, Long>? {
            if (schedules.isEmpty()) return null

            val now = Calendar.getInstance()
            val currentDayOfWeek = when (now.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            val currentSeconds = now.get(Calendar.SECOND)

            // Look for today's upcoming classes first
            val todayClasses = schedules.filter { it.dayOfWeek == currentDayOfWeek }
            val nextToday = todayClasses
                .mapNotNull { item ->
                    val parts = item.startTime.split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val classStartMinutes = h * 60 + m
                    if (classStartMinutes > currentMinutes) {
                        val diffMinutes = classStartMinutes - currentMinutes
                        val remainingMs = (diffMinutes * 60 - currentSeconds) * 1000L
                        Pair(item, remainingMs)
                    } else {
                        null
                    }
                }
                .minByOrNull { it.second }

            if (nextToday != null) return nextToday

            // Otherwise check next days
            for (dayOffset in 1..7) {
                val nextDay = ((currentDayOfWeek - 1 + dayOffset) % 7) + 1
                val dayClasses = schedules.filter { it.dayOfWeek == nextDay }
                val earliest = dayClasses.minByOrNull { it.startTime }
                if (earliest != null) {
                    val parts = earliest.startTime.split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val targetTime = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val remainingMs = targetTime.timeInMillis - now.timeInMillis
                    if (remainingMs > 0) {
                        return Pair(earliest, remainingMs)
                    }
                }
            }

            return null
        }
    }
}
