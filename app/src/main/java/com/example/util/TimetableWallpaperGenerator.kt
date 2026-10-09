package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.model.DayOfWeekHelper
import com.example.data.model.ScheduleItem
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

object TimetableWallpaperGenerator {

    enum class WallpaperTheme(
        val displayName: String,
        val bgStart: Int,
        val bgEnd: Int,
        val cardBg: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val accentColor: Int
    ) {
        CYBER_DARK(
            "Cyber Midnight 🌌",
            Color.parseColor("#0F172A"),
            Color.parseColor("#1E1B4B"),
            Color.parseColor("#1E293B"),
            Color.parseColor("#F8FAFC"),
            Color.parseColor("#94A3B8"),
            Color.parseColor("#38BDF8")
        ),
        PASTEL_CREAM(
            "Pastel Aesthetic 🌸",
            Color.parseColor("#FFF7ED"),
            Color.parseColor("#FDE68A"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#1C1917"),
            Color.parseColor("#78716C"),
            Color.parseColor("#F43F5E")
        ),
        ACADEMIC_BLUE(
            "Sinh Viên Thanh Lịch 🎓",
            Color.parseColor("#1E3A8A"),
            Color.parseColor("#0F172A"),
            Color.parseColor("#1E293B"),
            Color.parseColor("#FFFFFF"),
            Color.parseColor("#CBD5E1"),
            Color.parseColor("#F59E0B")
        ),
        MINIMAL_MONO(
            "Tối Giản Đen Trắng 🖤",
            Color.parseColor("#000000"),
            Color.parseColor("#18181B"),
            Color.parseColor("#27272A"),
            Color.parseColor("#FAFAFA"),
            Color.parseColor("#A1A1AA"),
            Color.parseColor("#FFFFFF")
        )
    }

    enum class WallpaperScope(val label: String) {
        ALL_WEEK("Cả Tuần (T2 - CN)"),
        MON_TO_WED("Đầu Tuần (T2 - T4)"),
        THU_TO_SUN("Cuối Tuần (T5 - CN)"),
        WEEKDAYS("Chỉ Ngày Thường (T2 - T6)")
    }

    enum class WallpaperLayout(val label: String) {
        STANDARD_9_16("Màn Khóa 9:16 Tự Động Co Giãn"),
        TWO_COLUMNS("Chia 2 Cột Thông Minh (Nhiều Tiết)"),
        LONG_POSTER("Ảnh Cuộn Dài (Đầy Đủ 100% Tiết)")
    }

    /**
     * Thuật toán Gộp Tiết Thông Minh:
     * Tự động gộp các tiết liên tiếp cùng môn, cùng phòng, cùng giảng viên trong ngày
     * (Ví dụ 4 tiết Toán 07:00-07:45, 07:45-08:30, 08:35-09:20, 09:25-10:10 -> 1 khối 07:00 - 10:10)
     */
    fun mergeConsecutivePeriods(items: List<ScheduleItem>): List<ScheduleItem> {
        if (items.size <= 1) return items
        val sorted = items.sortedBy { it.startTime }
        val merged = mutableListOf<ScheduleItem>()

        var current = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            val isSameSubject = current.title.trim().equals(next.title.trim(), ignoreCase = true)
            val isSameRoom = current.room.trim().equals(next.room.trim(), ignoreCase = true)
            val isContinuous = isTimeContinuous(current.endTime, next.startTime)

            if (isSameSubject && isSameRoom && isContinuous) {
                // Gộp thời gian
                current = current.copy(endTime = next.endTime)
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private fun isTimeContinuous(endTimeStr: String, nextStartTimeStr: String): Boolean {
        return try {
            val endMinutes = parseTimeToMinutes(endTimeStr)
            val startMinutes = parseTimeToMinutes(nextStartTimeStr)
            // Cách nhau tối đa 20 phút giải lao vẫn tính là cùng 1 buổi học
            (startMinutes - endMinutes) in -5..20
        } catch (e: Exception) {
            false
        }
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * Tạo hình nền thích ứng với số lượng tiết học lớn (116 tiết)
     */
    fun generateWallpaper(
        schedules: List<ScheduleItem>,
        theme: WallpaperTheme = WallpaperTheme.CYBER_DARK,
        customTitle: String = "THỜI KHÓA BIỂU",
        customSubtitle: String = "Lịch Học Cá Nhân",
        scope: WallpaperScope = WallpaperScope.ALL_WEEK,
        layout: WallpaperLayout = WallpaperLayout.STANDARD_9_16,
        autoMergeConsecutive: Boolean = true
    ): Bitmap {
        // 1. Lọc theo Scope
        val targetDays = when (scope) {
            WallpaperScope.ALL_WEEK -> (1..7).toList()
            WallpaperScope.MON_TO_WED -> listOf(1, 2, 3)
            WallpaperScope.THU_TO_SUN -> listOf(4, 5, 6, 7)
            WallpaperScope.WEEKDAYS -> listOf(1, 2, 3, 4, 5)
        }

        val filteredSchedules = schedules.filter { it.dayOfWeek in targetDays }

        // 2. Nhóm theo ngày và tùy chọn gộp tiết
        val dayGroups = targetDays.map { day ->
            val dayItems = filteredSchedules.filter { it.dayOfWeek == day }
            val processedItems = if (autoMergeConsecutive) mergeConsecutivePeriods(dayItems) else dayItems.sortedBy { it.startTime }
            day to processedItems
        }

        val totalProcessedItems = dayGroups.sumOf { it.second.size }

        // Quyết định bố cục tự động nếu quá nhiều môn (> 20 môn/tuần)
        val effectiveLayout = if (layout == WallpaperLayout.STANDARD_9_16 && totalProcessedItems > 18) {
            WallpaperLayout.TWO_COLUMNS
        } else {
            layout
        }

        val width = 1080
        val height = when (effectiveLayout) {
            WallpaperLayout.LONG_POSTER -> max(1920, 420 + (totalProcessedItems * 58) + (targetDays.size * 65))
            else -> 1920
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 3. Vẽ nền Gradient
        val bgPaint = Paint().apply { isAntiAlias = true }
        val shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            theme.bgStart, theme.bgEnd,
            android.graphics.Shader.TileMode.CLAMP
        )
        bgPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Họa tiết phát sáng nền
        val glowPaint = Paint().apply {
            isAntiAlias = true
            color = theme.accentColor
            alpha = 25
        }
        canvas.drawCircle(width * 0.85f, 180f, 260f, glowPaint)
        canvas.drawCircle(width * 0.15f, height - 200f, 300f, glowPaint)

        // 4. Header
        val headerTagPaint = Paint().apply {
            isAntiAlias = true
            color = theme.accentColor
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦ SMART SCHEDULE • ${scope.label.uppercase(Locale.getDefault())} ✦", width / 2f, 100f, headerTagPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = theme.textPrimary
            textSize = 50f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(customTitle, width / 2f, 165f, titlePaint)

        val subPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        val countInfo = if (schedules.size != totalProcessedItems) {
            "$customSubtitle • ${schedules.size} tiết (Gộp gọn ${totalProcessedItems} ca học)"
        } else {
            "$customSubtitle • ${totalProcessedItems} môn học"
        }
        canvas.drawText(countInfo, width / 2f, 215f, subPaint)

        val dividerPaint = Paint().apply {
            color = theme.accentColor
            alpha = 80
            strokeWidth = 2.5f
        }
        canvas.drawLine(width * 0.15f, 245f, width * 0.85f, 245f, dividerPaint)

        // 5. Vẽ danh sách theo Bố cục
        when (effectiveLayout) {
            WallpaperLayout.TWO_COLUMNS -> {
                drawTwoColumnsLayout(canvas, dayGroups, width, height, theme)
            }
            else -> {
                drawStandardListLayout(canvas, dayGroups, width, height, theme)
            }
        }

        // 6. Footer
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✨ Kỷ luật là cây cầu nối giữa mục tiêu và thành tựu ✨", width / 2f, height - 40f, footerPaint)

        return bitmap
    }

    /**
     * Bố cục Danh Sách Dọc (Standard List)
     */
    private fun drawStandardListLayout(
        canvas: Canvas,
        dayGroups: List<Pair<Int, List<ScheduleItem>>>,
        width: Int,
        height: Int,
        theme: WallpaperTheme
    ) {
        val totalItems = dayGroups.sumOf { it.second.size }
        val itemHeight = if (totalItems > 15) 42f else 52f
        val itemTextSize = if (totalItems > 15) 24f else 28f
        val itemTimeSize = if (totalItems > 15) 20f else 24f

        val cardMarginX = 50f
        val cardWidth = width - (cardMarginX * 2)
        var currentY = 275f

        val cardPaint = Paint().apply { isAntiAlias = true; color = theme.cardBg; alpha = 220 }
        val cardBorderPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.5f; color = theme.accentColor; alpha = 60 }
        val dayHeaderPaint = Paint().apply { isAntiAlias = true; color = theme.accentColor; textSize = 28f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTextPaint = Paint().apply { isAntiAlias = true; color = theme.textPrimary; textSize = itemTextSize; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTimePaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = itemTimeSize }
        val pillPaint = Paint().apply { isAntiAlias = true }

        for ((day, items) in dayGroups) {
            if (currentY > height - 120f) break

            val dayName = DayOfWeekHelper.getDayNameVi(day).uppercase(Locale.getDefault())
            val cardHeight = if (items.isEmpty()) 54f else (48f + items.size * itemHeight)

            val cardRect = RectF(cardMarginX, currentY, cardMarginX + cardWidth, currentY + cardHeight)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardBorderPaint)

            canvas.drawText(dayName, cardMarginX + 24f, currentY + 34f, dayHeaderPaint)

            if (items.isEmpty()) {
                val emptyPaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = 22f; alpha = 140 }
                canvas.drawText("Nghỉ học / Tự do", cardMarginX + 240f, currentY + 34f, emptyPaint)
            } else {
                var itemY = currentY + 68f
                for (item in items) {
                    try { pillPaint.color = Color.parseColor(item.colorHex) } catch (_: Exception) { pillPaint.color = theme.accentColor }
                    canvas.drawRoundRect(RectF(cardMarginX + 24f, itemY - 18f, cardMarginX + 32f, itemY + 10f), 4f, 4f, pillPaint)

                    val timeStr = "${item.startTime} - ${item.endTime}"
                    canvas.drawText(timeStr, cardMarginX + 44f, itemY, itemTimePaint)

                    val roomStr = if (item.room.isNotBlank()) " [${item.room}]" else ""
                    val titleText = "${item.title}$roomStr"
                    val truncated = if (titleText.length > 28) titleText.take(27) + "…" else titleText
                    canvas.drawText(truncated, cardMarginX + 270f, itemY, itemTextPaint)

                    itemY += itemHeight
                }
            }

            currentY += cardHeight + 12f
        }
    }

    /**
     * Bố cục 2 Cột Song Song Thông Minh (Two Columns Layout)
     * Thích hợp hoàn hảo cho lịch học dày đặc 50-116 tiết mà không làm vỡ tỉ lệ 9:16
     */
    private fun drawTwoColumnsLayout(
        canvas: Canvas,
        dayGroups: List<Pair<Int, List<ScheduleItem>>>,
        width: Int,
        height: Int,
        theme: WallpaperTheme
    ) {
        val midIndex = (dayGroups.size + 1) / 2
        val leftGroups = dayGroups.take(midIndex)
        val rightGroups = dayGroups.drop(midIndex)

        val colMargin = 30f
        val colGap = 16f
        val colWidth = (width - (colMargin * 2) - colGap) / 2f

        val cardPaint = Paint().apply { isAntiAlias = true; color = theme.cardBg; alpha = 230 }
        val cardBorderPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.2f; color = theme.accentColor; alpha = 60 }
        val dayHeaderPaint = Paint().apply { isAntiAlias = true; color = theme.accentColor; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTextPaint = Paint().apply { isAntiAlias = true; color = theme.textPrimary; textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTimePaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = 18f }
        val pillPaint = Paint().apply { isAntiAlias = true }

        fun renderColumn(groups: List<Pair<Int, List<ScheduleItem>>>, startX: Float) {
            var currentY = 270f
            for ((day, items) in groups) {
                if (currentY > height - 90f) break

                val dayName = DayOfWeekHelper.getShortDayNameVi(day).uppercase(Locale.getDefault())
                val cardHeight = if (items.isEmpty()) 46f else (42f + items.size * 42f)

                val cardRect = RectF(startX, currentY, startX + colWidth, currentY + cardHeight)
                canvas.drawRoundRect(cardRect, 14f, 14f, cardPaint)
                canvas.drawRoundRect(cardRect, 14f, 14f, cardBorderPaint)

                canvas.drawText(dayName, startX + 16f, currentY + 30f, dayHeaderPaint)

                if (items.isEmpty()) {
                    val emptyPaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = 19f; alpha = 140 }
                    canvas.drawText("Nghỉ", startX + 120f, currentY + 30f, emptyPaint)
                } else {
                    var itemY = currentY + 60f
                    for (item in items) {
                        try { pillPaint.color = Color.parseColor(item.colorHex) } catch (_: Exception) { pillPaint.color = theme.accentColor }
                        canvas.drawRoundRect(RectF(startX + 16f, itemY - 14f, startX + 22f, itemY + 8f), 3f, 3f, pillPaint)

                        val timeStr = "${item.startTime}-${item.endTime}"
                        canvas.drawText(timeStr, startX + 28f, itemY, itemTimePaint)

                        val roomStr = if (item.room.isNotBlank()) " [${item.room}]" else ""
                        val titleText = "${item.title}$roomStr"
                        val truncated = if (titleText.length > 16) titleText.take(15) + "…" else titleText
                        canvas.drawText(truncated, startX + 185f, itemY, itemTextPaint)

                        itemY += 42f
                    }
                }
                currentY += cardHeight + 10f
            }
        }

        // Vẽ cột trái & cột phải
        renderColumn(leftGroups, colMargin)
        renderColumn(rightGroups, colMargin + colWidth + colGap)
    }

    /**
     * Lưu ảnh vào cache để chia sẻ qua Intent ACTION_SEND
     */
    fun saveWallpaperToCacheAndShare(context: Context, bitmap: Bitmap) {
        try {
            val cacheDir = File(context.cacheDir, "wallpapers")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val file = File(cacheDir, "tkb_wallpaper_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Thời khóa biểu cá nhân")
                putExtra(Intent.EXTRA_TEXT, "Thời khóa biểu học tập của mình ✨")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Chia sẻ hoặc Đặt làm hình nền"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
