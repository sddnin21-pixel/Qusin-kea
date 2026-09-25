package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class TimetableCountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        // 1. Immediately push default view so Android Launcher never marks widget as uninitialized
        for (appWidgetId in appWidgetIds) {
            val placeholder = buildInitialViews(context)
            appWidgetManager.updateAppWidget(appWidgetId, placeholder)
        }

        // 2. Asynchronously query Room DB and bind live schedule data
        val pendingResult = try { goAsync() } catch (e: Exception) { null }
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        // Note: AppWidgetManager.ACTION_APPWIDGET_UPDATE is already handled by super.onReceive -> onUpdate
        if (action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TimetableCountdownWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val pendingResult = try { goAsync() } catch (e: Exception) { null }
                updateWidgetsAsync(context, appWidgetManager, ids, pendingResult)
            }
        }
    }

    companion object {
        private const val TAG = "TimetableWidget"
        const val ACTION_UPDATE_WIDGET = "com.example.widget.ACTION_UPDATE_WIDGET"

        fun updateAllWidgets(context: Context) {
            try {
                val intent = Intent(context, TimetableCountdownWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_WIDGET
                }
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send update broadcast", e)
            }
        }

        private fun buildInitialViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_timetable_countdown)
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
            views.setTextViewText(R.id.widget_header_title, "⏳ THỜI KHÓA BIỂU")
            views.setTextViewText(R.id.widget_room, "Đang tải...")
            views.setTextViewText(R.id.widget_class_title, "Đang nạp thời khóa biểu...")
            views.setTextViewText(R.id.widget_time_lecturer, "Chạm để mở ứng dụng")
            views.setTextViewText(R.id.widget_flipper_title2, "Thời khóa biểu AI")
            views.setTextViewText(R.id.widget_flipper_sub2, "Tự động đếm ngược giờ học")
            views.setViewVisibility(R.id.widget_progress_bar, View.GONE)
            views.setViewVisibility(R.id.widget_countdown_container, View.GONE)
            return views
        }

        private fun updateWidgetsAsync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            pendingResult: BroadcastReceiver.PendingResult?
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val allSchedules = try {
                        db.scheduleDao().getAllSchedulesList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching schedules", e)
                        emptyList()
                    }
                    val widgetState = computeWidgetState(allSchedules)

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

                        views.setTextViewText(R.id.widget_header_title, widgetState.header)
                        views.setTextViewText(R.id.widget_room, widgetState.room)
                        views.setTextViewText(R.id.widget_class_title, widgetState.title)
                        views.setTextViewText(R.id.widget_time_lecturer, widgetState.subtitle)
                        views.setTextViewText(R.id.widget_flipper_title2, widgetState.secondaryTitle)
                        views.setTextViewText(R.id.widget_flipper_sub2, widgetState.secondarySubtitle)

                        if (widgetState.progressPercent >= 0) {
                            views.setViewVisibility(R.id.widget_progress_bar, View.VISIBLE)
                            views.setProgressBar(R.id.widget_progress_bar, 100, widgetState.progressPercent, false)
                        } else {
                            views.setViewVisibility(R.id.widget_progress_bar, View.GONE)
                        }

                        if (widgetState.hasCountdown && widgetState.remainingMs > 0) {
                            views.setViewVisibility(R.id.widget_countdown_container, View.VISIBLE)
                            views.setTextViewText(R.id.widget_countdown_label, widgetState.countdownLabel)

                            views.setViewVisibility(R.id.widget_countdown_chronometer, View.VISIBLE)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                views.setChronometerCountDown(R.id.widget_countdown_chronometer, true)
                            }
                            val baseTime = SystemClock.elapsedRealtime() + widgetState.remainingMs
                            views.setChronometer(
                                R.id.widget_countdown_chronometer,
                                baseTime,
                                null,
                                true
                            )
                        } else {
                            views.setViewVisibility(R.id.widget_countdown_container, View.GONE)
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating widgets", e)
                } finally {
                    try {
                        pendingResult?.finish()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error finishing pendingResult", e)
                    }
                }
            }
        }

        private data class WidgetDisplayData(
            val header: String,
            val room: String,
            val title: String,
            val subtitle: String,
            val secondaryTitle: String = "Thông tin chi tiết",
            val secondarySubtitle: String = "",
            val hasCountdown: Boolean,
            val countdownLabel: String = "",
            val remainingMs: Long = 0L,
            val progressPercent: Int = -1
        )

        private fun computeWidgetState(schedules: List<ScheduleItem>): WidgetDisplayData {
            if (schedules.isEmpty()) {
                return WidgetDisplayData(
                    header = "✨ THỜI KHÓA BIỂU",
                    room = "Trống",
                    title = "Chưa có môn học nào",
                    subtitle = "Chạm để thêm hoặc quét lịch bằng AI",
                    secondaryTitle = "Nhận diện bằng AI",
                    secondarySubtitle = "Chụp hoặc chọn ảnh màn hình TKB",
                    hasCountdown = false,
                    progressPercent = -1
                )
            }

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

            val todayClasses = schedules.filter { it.dayOfWeek == currentDayOfWeek }

            // 1. Check if a class is currently in progress
            for (item in todayClasses) {
                val startMin = parseMinutes(item.startTime) ?: continue
                val endMin = parseMinutes(item.endTime) ?: (startMin + 45)
                if (currentMinutes in startMin until endMin) {
                    val remainingMs = ((endMin - currentMinutes) * 60 - currentSeconds) * 1000L
                    val totalDurationMin = (endMin - startMin).coerceAtLeast(1)
                    val elapsedMin = (currentMinutes - startMin).coerceAtLeast(0)
                    val percent = ((elapsedMin.toFloat() / totalDurationMin) * 100).toInt().coerceIn(0, 100)

                    val gv = if (item.lecturer.isNotBlank()) " • GV: ${item.lecturer}" else ""
                    val notes = if (item.notes.isNotBlank()) " • ${item.notes}" else ""
                    return WidgetDisplayData(
                        header = "🔴 ĐANG DIỄN RA",
                        room = if (item.room.isNotBlank()) item.room else "Lớp học",
                        title = item.title,
                        subtitle = "${item.startTime} - ${item.endTime}$gv",
                        secondaryTitle = "Phòng: ${if (item.room.isNotBlank()) item.room else "Lớp học"}",
                        secondarySubtitle = "Đã học: $percent%$notes",
                        hasCountdown = true,
                        countdownLabel = "Kết thúc sau: ",
                        remainingMs = if (remainingMs > 0) remainingMs else 1000L,
                        progressPercent = percent
                    )
                }
            }

            // 2. Check for next upcoming class today
            val nextToday = todayClasses
                .mapNotNull { item ->
                    val startMin = parseMinutes(item.startTime) ?: return@mapNotNull null
                    if (startMin > currentMinutes) {
                        val remainingMs = ((startMin - currentMinutes) * 60 - currentSeconds) * 1000L
                        Pair(item, remainingMs)
                    } else null
                }
                .minByOrNull { it.second }

            if (nextToday != null) {
                val (item, remainingMs) = nextToday
                val gv = if (item.lecturer.isNotBlank()) " • GV: ${item.lecturer}" else ""
                val notes = if (item.notes.isNotBlank()) " • ${item.notes}" else ""
                val startMin = parseMinutes(item.startTime) ?: 0
                val remainingCount = todayClasses.count { (parseMinutes(it.startTime) ?: 0) >= startMin }

                return WidgetDisplayData(
                    header = "⏳ TIẾT TIẾP THEO",
                    room = if (item.room.isNotBlank()) item.room else "Lớp học",
                    title = item.title,
                    subtitle = "${item.startTime} - ${item.endTime}$gv",
                    secondaryTitle = "Hôm nay còn $remainingCount môn học",
                    secondarySubtitle = "Phòng: ${if (item.room.isNotBlank()) item.room else "Lớp học"}$notes",
                    hasCountdown = true,
                    countdownLabel = "Bắt đầu sau: ",
                    remainingMs = remainingMs,
                    progressPercent = -1
                )
            }

            // 3. Otherwise find next upcoming class in future days
            for (dayOffset in 1..7) {
                val nextDay = ((currentDayOfWeek - 1 + dayOffset) % 7) + 1
                val dayName = when (nextDay) {
                    1 -> "Thứ 2"
                    2 -> "Thứ 3"
                    3 -> "Thứ 4"
                    4 -> "Thứ 5"
                    5 -> "Thứ 6"
                    6 -> "Thứ 7"
                    7 -> "Chủ Nhật"
                    else -> ""
                }
                val dayClasses = schedules.filter { it.dayOfWeek == nextDay }
                val earliest = dayClasses.minByOrNull { parseMinutes(it.startTime) ?: 9999 }
                if (earliest != null) {
                    val startMin = parseMinutes(earliest.startTime) ?: (8 * 60)
                    val targetTime = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                        set(Calendar.HOUR_OF_DAY, startMin / 60)
                        set(Calendar.MINUTE, startMin % 60)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val remainingMs = targetTime.timeInMillis - now.timeInMillis
                    val gv = if (earliest.lecturer.isNotBlank()) " • GV: ${earliest.lecturer}" else ""
                    val notes = if (earliest.notes.isNotBlank()) " • ${earliest.notes}" else ""
                    return WidgetDisplayData(
                        header = "📅 TIẾT TIẾP ($dayName)",
                        room = if (earliest.room.isNotBlank()) earliest.room else "Lớp học",
                        title = earliest.title,
                        subtitle = "${earliest.startTime} - ${earliest.endTime}$gv",
                        secondaryTitle = "Tiết học kế tiếp: $dayName",
                        secondarySubtitle = "Phòng: ${if (earliest.room.isNotBlank()) earliest.room else "Lớp học"}$notes",
                        hasCountdown = true,
                        countdownLabel = "Còn lại: ",
                        remainingMs = remainingMs,
                        progressPercent = -1
                    )
                }
            }

            return WidgetDisplayData(
                header = "✨ XONG LỊCH HÔM NAY",
                room = "Nghỉ ngơi",
                title = "Hôm nay không còn tiết học",
                subtitle = "Chạm để xem toàn bộ lịch tuần",
                secondaryTitle = "Thư giãn & nghỉ ngơi!",
                secondarySubtitle = "Chạm để quản lý thời khóa biểu",
                hasCountdown = false,
                progressPercent = -1
            )
        }

        private fun parseMinutes(timeStr: String): Int? {
            val parts = timeStr.trim().split(":")
            if (parts.size < 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return h * 60 + m
        }
    }
}
