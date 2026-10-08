package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.DayOfWeekHelper
import com.example.data.model.ScheduleItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimetableAgendaWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_UPDATE_AGENDA = "com.example.widget.ACTION_UPDATE_AGENDA"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TimetableAgendaWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, TimetableAgendaWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_AGENDA
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_UPDATE_AGENDA) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TimetableAgendaWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val pendingResult = goAsync()
                providerScope.launch {
                    try {
                        updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        try {
                            pendingResult?.finish()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        providerScope.launch {
            try {
                updateWidgetsInternal(context, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    pendingResult?.finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun updateWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val db = AppDatabase.getDatabase(context)
        val allSchedules = try {
            db.scheduleDao().getAllSchedulesList()
        } catch (e: Exception) {
            emptyList()
        }

        val cal = Calendar.getInstance()
        val currentDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dateFormat = SimpleDateFormat("EEEE, dd/MM", Locale("vi", "VN"))
        val dateString = dateFormat.format(Date()).replaceFirstChar { it.uppercase() }

        // Môn học hôm nay
        val todayClasses = allSchedules.filter { it.dayOfWeek == currentDay }.sortedBy { it.startTime }

        // Kiểm tra xem hôm nay còn môn không hay đã qua hết
        val remainingToday = todayClasses.filter { parseTimeToMinutes(it.endTime) > nowMinutes }
        val displayClasses: List<ScheduleItem>
        val isShowingTomorrow: Boolean

        if (todayClasses.isNotEmpty() && remainingToday.isNotEmpty()) {
            displayClasses = todayClasses
            isShowingTomorrow = false
        } else if (todayClasses.isNotEmpty() && remainingToday.isEmpty()) {
            // Hôm nay đã xong hết, hiển thị ngày mai
            val nextDay = if (currentDay == 7) 1 else currentDay + 1
            val tomorrowClasses = allSchedules.filter { it.dayOfWeek == nextDay }.sortedBy { it.startTime }
            displayClasses = tomorrowClasses
            isShowingTomorrow = true
        } else {
            // Hôm nay không có môn, hiển thị ngày tiếp theo có môn
            var foundDay = currentDay
            var foundClasses = emptyList<ScheduleItem>()
            for (i in 1..6) {
                val checkDay = ((currentDay - 1 + i) % 7) + 1
                val classes = allSchedules.filter { it.dayOfWeek == checkDay }.sortedBy { it.startTime }
                if (classes.isNotEmpty()) {
                    foundDay = checkDay
                    foundClasses = classes
                    break
                }
            }
            displayClasses = foundClasses
            isShowingTomorrow = foundDay != currentDay
        }

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_timetable_agenda)

            // Setup click opens app
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_agenda_root, appPendingIntent)

            // Setup refresh button
            val refreshIntent = Intent(context, TimetableAgendaWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_AGENDA
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_agenda_refresh, refreshPendingIntent)

            // Set Header Date
            val headerText = if (isShowingTomorrow) {
                "Ngày mai • Lịch kế tiếp"
            } else {
                dateString
            }
            views.setTextViewText(R.id.widget_agenda_date, headerText)
            views.setTextViewText(R.id.widget_agenda_count_badge, "${displayClasses.size} Môn")

            if (displayClasses.isEmpty()) {
                views.setViewVisibility(R.id.widget_agenda_list, View.GONE)
                views.setViewVisibility(R.id.widget_agenda_empty, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_agenda_list, View.VISIBLE)
                views.setViewVisibility(R.id.widget_agenda_empty, View.GONE)

                // Render Slot 1
                bindSlot(views, 1, displayClasses.getOrNull(0), nowMinutes, isShowingTomorrow)
                // Render Slot 2
                bindSlot(views, 2, displayClasses.getOrNull(1), nowMinutes, isShowingTomorrow)
                // Render Slot 3
                bindSlot(views, 3, displayClasses.getOrNull(2), nowMinutes, isShowingTomorrow)
            }

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun bindSlot(
        views: RemoteViews,
        slotIndex: Int,
        item: ScheduleItem?,
        nowMinutes: Int,
        isShowingTomorrow: Boolean
    ) {
        val containerId = when (slotIndex) {
            1 -> R.id.slot_1_container
            2 -> R.id.slot_2_container
            else -> R.id.slot_3_container
        }
        val titleId = when (slotIndex) {
            1 -> R.id.slot_1_title
            2 -> R.id.slot_2_title
            else -> R.id.slot_3_title
        }
        val detailId = when (slotIndex) {
            1 -> R.id.slot_1_detail
            2 -> R.id.slot_2_detail
            else -> R.id.slot_3_detail
        }
        val badgeId = when (slotIndex) {
            1 -> R.id.slot_1_badge
            2 -> R.id.slot_2_badge
            else -> R.id.slot_3_badge
        }

        if (item == null) {
            views.setViewVisibility(containerId, View.GONE)
            return
        }

        views.setViewVisibility(containerId, View.VISIBLE)
        views.setTextViewText(titleId, item.title)

        val roomStr = if (item.room.isNotBlank()) " • P.${item.room}" else ""
        val lecturerStr = if (item.lecturer.isNotBlank()) " • ${item.lecturer}" else ""
        views.setTextViewText(detailId, "${item.startTime} - ${item.endTime}$roomStr$lecturerStr")

        val startMin = parseTimeToMinutes(item.startTime)
        val endMin = parseTimeToMinutes(item.endTime)

        if (isShowingTomorrow) {
            views.setTextViewText(badgeId, "Ngày mai")
            views.setTextColor(badgeId, Color.parseColor("#94A3B8"))
        } else {
            when {
                nowMinutes in startMin..endMin -> {
                    views.setTextViewText(badgeId, "Đang học 🔴")
                    views.setTextColor(badgeId, Color.parseColor("#EF4444"))
                }
                nowMinutes < startMin -> {
                    val diff = startMin - nowMinutes
                    val badgeStr = if (diff <= 60) "Còn ${diff}p" else "Sắp tới"
                    views.setTextViewText(badgeId, badgeStr)
                    views.setTextColor(badgeId, Color.parseColor("#38BDF8"))
                }
                else -> {
                    views.setTextViewText(badgeId, "Đã xong ✓")
                    views.setTextColor(badgeId, Color.parseColor("#64748B"))
                }
            }
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
}
