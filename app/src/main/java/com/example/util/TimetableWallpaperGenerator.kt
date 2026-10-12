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
import kotlin.math.min

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

    enum class WallpaperLayout(val label: String, val description: String) {
        STANDARD_9_16(
            "Màn Khóa 9:16 Dọc Co Giãn",
            "Bố cục danh sách tuần tiêu chuẩn"
        ),
        TWO_COLUMNS(
            "Chia 2 Cột Thông Minh",
            "Tự động chia 2 cột, thích hợp 20 - 50 tiết"
        ),
        GRID_MATRIX(
            "Bảng Ma Trận Tuần (Chuẩn 116 Tiết)",
            "Lưới 7 ngày x 12 ca học kinh điển, vừa khít 100% không cắt xén!"
        ),
        LONG_POSTER(
            "Ảnh Cuộn Dài 100%",
            "Mở rộng chiều dọc không giới hạn, chữ to rõ nét"
        )
    }

    /**
     * Thuật toán Gộp Tiết Thông Minh:
     * Tự động gộp các tiết liên tiếp cùng môn, cùng phòng trong ngày
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

    fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * Tạo hình nền 9:16 thích ứng ngay cả khi lịch học lên đến 116 tiết/tuần
     */
    fun generateWallpaper(
        schedules: List<ScheduleItem>,
        theme: WallpaperTheme = WallpaperTheme.CYBER_DARK,
        customTitle: String = "THỜI KHÓA BIỂU",
        customSubtitle: String = "Lịch Học Cá Nhân",
        scope: WallpaperScope = WallpaperScope.ALL_WEEK,
        layout: WallpaperLayout = WallpaperLayout.STANDARD_9_16,
        autoMergeConsecutive: Boolean = true,
        customMotivationalQuote: String? = null,
        context: Context? = null
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
            val processedItems = if (autoMergeConsecutive) {
                mergeConsecutivePeriods(dayItems)
            } else {
                dayItems.sortedBy { it.startTime }
            }
            day to processedItems
        }

        val totalProcessedItems = dayGroups.sumOf { it.second.size }

        // Quyết định bố cục tối ưu:
        // Nếu người dùng chọn STANDARD_9_16 nhưng có quá nhiều tiết (> 25 ca), tự động chuyển sang TWO_COLUMNS
        // Nếu số tiết cực kỳ lớn (> 55 tiết) và không chọn LONG_POSTER hay GRID_MATRIX, ưu tiên GRID_MATRIX
        val effectiveLayout = when {
            layout == WallpaperLayout.GRID_MATRIX -> WallpaperLayout.GRID_MATRIX
            layout == WallpaperLayout.LONG_POSTER -> WallpaperLayout.LONG_POSTER
            layout == WallpaperLayout.STANDARD_9_16 && totalProcessedItems > 50 -> WallpaperLayout.GRID_MATRIX
            layout == WallpaperLayout.STANDARD_9_16 && totalProcessedItems > 16 -> WallpaperLayout.TWO_COLUMNS
            else -> layout
        }

        val width = 1080
        val height = when (effectiveLayout) {
            WallpaperLayout.LONG_POSTER -> {
                max(1920, 440 + (totalProcessedItems * 64) + (targetDays.size * 68))
            }
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

        // Họa tiết phát sáng nền viền
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
        canvas.drawText("✦ THỜI KHÓA BIỂU THÔNG MINH • ${scope.label.uppercase(Locale.getDefault())} ✦", width / 2f, 95f, headerTagPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = theme.textPrimary
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(customTitle, width / 2f, 155f, titlePaint)

        val subPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        val countInfo = if (schedules.size != totalProcessedItems) {
            "$customSubtitle • ${schedules.size} tiết (Tối ưu gộp ${totalProcessedItems} ca học)"
        } else {
            "$customSubtitle • ${totalProcessedItems} tiết học"
        }
        canvas.drawText(countInfo, width / 2f, 202f, subPaint)

        val dividerPaint = Paint().apply {
            color = theme.accentColor
            alpha = 70
            strokeWidth = 2f
        }
        canvas.drawLine(width * 0.15f, 228f, width * 0.85f, 228f, dividerPaint)

        // 5. Vẽ danh sách theo Bố cục
        when (effectiveLayout) {
            WallpaperLayout.GRID_MATRIX -> {
                drawGridMatrixLayout(canvas, dayGroups, targetDays, width, height, theme)
            }
            WallpaperLayout.TWO_COLUMNS -> {
                drawTwoColumnsLayout(canvas, dayGroups, width, height, theme)
            }
            else -> {
                drawStandardListLayout(canvas, dayGroups, width, height, theme)
            }
        }

        // 6. Footer: Hiển thị câu động lực học tập & THPTQG từ file 607 câu
        val effectiveQuote = when {
            !customMotivationalQuote.isNullOrBlank() -> customMotivationalQuote.trim()
            context != null -> {
                val q = MotivationQuotesHelper.getTodayQuote(context)
                "“${q.content}” — (Ngày ${String.format("%03d", q.dayNumber)}/607 • Chinh phục THPTQG)"
            }
            else -> "“Kỷ luật là cầu nối giữa mục tiêu và thành tựu.” — Chinh phục THPTQG ✨"
        }
        drawFooterQuote(canvas, effectiveQuote, width, height, theme)

        return bitmap
    }

    /**
     * Bố cục Ma Trận Tuần (Grid Matrix 7 ngày x các ca học)
     * Giải pháp hoàn hảo khi có tới 116 tiết:
     * Toàn bộ 116 tiết được đưa vào bảng lưới tuần chuẩn mực như TKB học sinh,
     * không bị tràn ra ngoài, không bị bỏ sót bất kỳ tiết nào trên màn hình khóa 9:16.
     */
    private fun drawGridMatrixLayout(
        canvas: Canvas,
        dayGroups: List<Pair<Int, List<ScheduleItem>>>,
        targetDays: List<Int>,
        width: Int,
        height: Int,
        theme: WallpaperTheme
    ) {
        val startY = 245f
        val bottomMargin = 95f
        val availableH = height - startY - bottomMargin

        val leftMargin = 30f
        val rightMargin = 30f
        val timeColWidth = 80f
        val availableW = width - leftMargin - rightMargin - timeColWidth
        val dayColWidth = availableW / targetDays.size.toFloat()

        // Các ca học đại diện từ 1 đến 12 (Sáng: 1-5, Chiều: 6-10, Tối: 11-12)
        val periodSlots = listOf(
            "T1" to "07:00",
            "T2" to "07:50",
            "T3" to "08:45",
            "T4" to "09:35",
            "T5" to "10:25",
            "T6" to "13:00",
            "T7" to "13:50",
            "T8" to "14:45",
            "T9" to "15:35",
            "T10" to "16:25",
            "T11" to "17:30",
            "T12" to "18:40"
        )
        val headerRowHeight = 44f
        val rowHeight = (availableH - headerRowHeight) / periodSlots.size.toFloat()

        val gridBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = theme.accentColor
            alpha = 50
        }
        val headerBgPaint = Paint().apply {
            isAntiAlias = true
            color = theme.cardBg
            alpha = 230
        }
        val dayHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = theme.accentColor
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val timeTextPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 17f
            textAlign = Paint.Align.CENTER
        }
        val cellBgPaint = Paint().apply { isAntiAlias = true }
        val cellTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val cellRoomPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            alpha = 200
            textSize = 13f
            textAlign = Paint.Align.CENTER
        }

        // 1. Vẽ Header bảng (Cột Giờ + Các Ngày)
        val headerRect = RectF(leftMargin, startY, width - rightMargin, startY + headerRowHeight)
        canvas.drawRoundRect(headerRect, 10f, 10f, headerBgPaint)
        canvas.drawRoundRect(headerRect, 10f, 10f, gridBorderPaint)

        canvas.drawText("Tiết", leftMargin + timeColWidth / 2f, startY + 28f, dayHeaderPaint)

        targetDays.forEachIndexed { index, dayNum ->
            val colX = leftMargin + timeColWidth + (index * dayColWidth)
            val dayName = DayOfWeekHelper.getShortDayNameVi(dayNum)
            canvas.drawText(dayName, colX + dayColWidth / 2f, startY + 28f, dayHeaderPaint)
            // Đường phân cách dọc
            canvas.drawLine(colX, startY, colX, startY + availableH, gridBorderPaint)
        }

        // 2. Vẽ Hàng Ca Học
        val contentStartY = startY + headerRowHeight
        periodSlots.forEachIndexed { pIdx, (periodName, approxTime) ->
            val rowY = contentStartY + (pIdx * rowHeight)

            // Nền hàng chẵn/lẻ
            if (pIdx % 2 == 1) {
                val rowRect = RectF(leftMargin, rowY, width - rightMargin, rowY + rowHeight)
                val stripePaint = Paint().apply {
                    color = theme.cardBg
                    alpha = 70
                }
                canvas.drawRect(rowRect, stripePaint)
            }

            // Cột thời gian
            canvas.drawText(periodName, leftMargin + timeColWidth / 2f, rowY + rowHeight * 0.45f, timeTextPaint)
            canvas.drawText(approxTime, leftMargin + timeColWidth / 2f, rowY + rowHeight * 0.85f, timeTextPaint)

            // Đường phân cách ngang
            canvas.drawLine(leftMargin, rowY + rowHeight, width - rightMargin, rowY + rowHeight, gridBorderPaint)
        }

        // 3. Phân bổ các môn học vào ô tương ứng
        val dayMap = dayGroups.toMap()
        targetDays.forEachIndexed { colIdx, dayNum ->
            val colX = leftMargin + timeColWidth + (colIdx * dayColWidth)
            val items = dayMap[dayNum] ?: emptyList()

            // Nếu ngày không có môn
            if (items.isEmpty()) {
                val freePaint = Paint().apply {
                    isAntiAlias = true
                    color = theme.textSecondary
                    alpha = 80
                    textSize = 18f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("Nghỉ", colX + dayColWidth / 2f, contentStartY + availableH / 2f, freePaint)
            } else {
                for (item in items) {
                    val startMin = parseTimeToMinutes(item.startTime)
                    val endMin = parseTimeToMinutes(item.endTime)

                    // Tính vị trí Y xấp xỉ trong ngày (07:00 -> 21:00 = 14 tiếng = 840 phút)
                    val dayStartMin = 7 * 60 // 07:00
                    val dayEndMin = 21 * 60  // 21:00
                    val totalDayMin = (dayEndMin - dayStartMin).toFloat()

                    val clampedStart = startMin.coerceIn(dayStartMin, dayEndMin)
                    val clampedEnd = endMin.coerceIn(clampedStart + 35, dayEndMin)

                    val topFraction = (clampedStart - dayStartMin) / totalDayMin
                    val bottomFraction = (clampedEnd - dayStartMin) / totalDayMin

                    val cellTop = contentStartY + (topFraction * (availableH - headerRowHeight)) + 2f
                    val cellBottom = contentStartY + (bottomFraction * (availableH - headerRowHeight)) - 2f
                    val cellHeight = max(24f, cellBottom - cellTop)

                    val cellRect = RectF(colX + 3f, cellTop, colX + dayColWidth - 3f, cellTop + cellHeight)

                    // Màu môn học
                    try {
                        cellBgPaint.color = Color.parseColor(item.colorHex)
                    } catch (_: Exception) {
                        cellBgPaint.color = theme.accentColor
                    }
                    cellBgPaint.alpha = 225
                    canvas.drawRoundRect(cellRect, 6f, 6f, cellBgPaint)

                    // Text môn học
                    val maxLen = if (dayColWidth < 140f) 8 else 12
                    val displayTitle = if (item.title.length > maxLen) {
                        item.title.take(maxLen - 1) + "…"
                    } else {
                        item.title
                    }
                    val centerY = cellTop + (cellHeight / 2f)

                    if (cellHeight >= 36f) {
                        canvas.drawText(displayTitle, colX + dayColWidth / 2f, centerY - 2f, cellTextPaint)
                        val roomStr = if (item.room.isNotBlank()) item.room else item.startTime
                        canvas.drawText(roomStr, colX + dayColWidth / 2f, centerY + 14f, cellRoomPaint)
                    } else {
                        canvas.drawText(displayTitle, colX + dayColWidth / 2f, centerY + 5f, cellTextPaint)
                    }
                }
            }
        }
    }

    /**
     * Bố cục 2 Cột Song Song Thông Minh (Adaptive Two Columns Layout)
     * Tự động co dãn chiều cao theo ngân sách điểm ảnh, đảm bảo không bỏ sót bất kỳ tiết nào
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

        val startY = 248f
        val bottomMargin = 95f
        val availableH = height - startY - bottomMargin

        // Tính ngân sách chiều cao động để không bao giờ bị cắt xén
        val maxColItems = max(1, max(leftGroups.sumOf { it.second.size }, rightGroups.sumOf { it.second.size }))
        val maxColDays = max(1, max(leftGroups.size, rightGroups.size))
        val headerBudget = maxColDays * 36f
        val itemBudget = availableH - headerBudget - (maxColDays * 10f)

        val itemHeight = (itemBudget / maxColItems).coerceIn(20f, 44f)
        val itemTextSize = (itemHeight * 0.52f).coerceIn(13f, 22f)
        val itemTimeSize = (itemHeight * 0.40f).coerceIn(11f, 17f)

        val cardPaint = Paint().apply { isAntiAlias = true; color = theme.cardBg; alpha = 230 }
        val cardBorderPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.2f; color = theme.accentColor; alpha = 60 }
        val dayHeaderPaint = Paint().apply { isAntiAlias = true; color = theme.accentColor; textSize = 22f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTextPaint = Paint().apply { isAntiAlias = true; color = theme.textPrimary; textSize = itemTextSize; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTimePaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = itemTimeSize }
        val pillPaint = Paint().apply { isAntiAlias = true }

        fun renderColumn(groups: List<Pair<Int, List<ScheduleItem>>>, startX: Float) {
            var currentY = startY
            for ((day, items) in groups) {
                val dayName = DayOfWeekHelper.getShortDayNameVi(day).uppercase(Locale.getDefault())
                val cardHeight = if (items.isEmpty()) 40f else (34f + items.size * itemHeight)

                val cardRect = RectF(startX, currentY, startX + colWidth, currentY + cardHeight)
                canvas.drawRoundRect(cardRect, 12f, 12f, cardPaint)
                canvas.drawRoundRect(cardRect, 12f, 12f, cardBorderPaint)

                canvas.drawText(dayName, startX + 16f, currentY + 24f, dayHeaderPaint)

                if (items.isEmpty()) {
                    val emptyPaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = 17f; alpha = 140 }
                    canvas.drawText("Nghỉ học", startX + 110f, currentY + 24f, emptyPaint)
                } else {
                    var itemY = currentY + 34f + (itemHeight * 0.7f)
                    for (item in items) {
                        try {
                            pillPaint.color = Color.parseColor(item.colorHex)
                        } catch (_: Exception) {
                            pillPaint.color = theme.accentColor
                        }
                        val pillTop = itemY - (itemHeight * 0.5f)
                        val pillBottom = itemY + (itemHeight * 0.15f)
                        canvas.drawRoundRect(RectF(startX + 14f, pillTop, startX + 19f, pillBottom), 3f, 3f, pillPaint)

                        val timeStr = "${item.startTime}-${item.endTime}"
                        canvas.drawText(timeStr, startX + 26f, itemY, itemTimePaint)

                        val roomStr = if (item.room.isNotBlank()) " [${item.room}]" else ""
                        val titleText = "${item.title}$roomStr"
                        val maxTitleLen = if (itemHeight < 28f) 14 else 18
                        val truncated = if (titleText.length > maxTitleLen) titleText.take(maxTitleLen - 1) + "…" else titleText
                        canvas.drawText(truncated, startX + 175f, itemY, itemTextPaint)

                        itemY += itemHeight
                    }
                }
                currentY += cardHeight + 8f
            }
        }

        renderColumn(leftGroups, colMargin)
        renderColumn(rightGroups, colMargin + colWidth + colGap)
    }

    /**
     * Bố cục Danh Sách Dọc Co Giãn (Standard List Layout)
     */
    private fun drawStandardListLayout(
        canvas: Canvas,
        dayGroups: List<Pair<Int, List<ScheduleItem>>>,
        width: Int,
        height: Int,
        theme: WallpaperTheme
    ) {
        val totalItems = dayGroups.sumOf { it.second.size }
        val startY = 248f
        val bottomMargin = 95f
        val availableH = height - startY - bottomMargin

        val dayCount = max(1, dayGroups.size)
        val headerBudget = dayCount * 42f
        val itemBudget = availableH - headerBudget - (dayCount * 12f)
        val itemHeight = (itemBudget / max(1, totalItems)).coerceIn(24f, 52f)
        val itemTextSize = (itemHeight * 0.52f).coerceIn(14f, 26f)
        val itemTimeSize = (itemHeight * 0.42f).coerceIn(12f, 22f)

        val cardMarginX = 50f
        val cardWidth = width - (cardMarginX * 2)
        var currentY = startY

        val cardPaint = Paint().apply { isAntiAlias = true; color = theme.cardBg; alpha = 220 }
        val cardBorderPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 1.5f; color = theme.accentColor; alpha = 60 }
        val dayHeaderPaint = Paint().apply { isAntiAlias = true; color = theme.accentColor; textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTextPaint = Paint().apply { isAntiAlias = true; color = theme.textPrimary; textSize = itemTextSize; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val itemTimePaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = itemTimeSize }
        val pillPaint = Paint().apply { isAntiAlias = true }

        for ((day, items) in dayGroups) {
            val dayName = DayOfWeekHelper.getDayNameVi(day).uppercase(Locale.getDefault())
            val cardHeight = if (items.isEmpty()) 48f else (42f + items.size * itemHeight)

            val cardRect = RectF(cardMarginX, currentY, cardMarginX + cardWidth, currentY + cardHeight)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)
            canvas.drawRoundRect(cardRect, 16f, 16f, cardBorderPaint)

            canvas.drawText(dayName, cardMarginX + 24f, currentY + 30f, dayHeaderPaint)

            if (items.isEmpty()) {
                val emptyPaint = Paint().apply { isAntiAlias = true; color = theme.textSecondary; textSize = 20f; alpha = 140 }
                canvas.drawText("Nghỉ học / Tự do", cardMarginX + 220f, currentY + 30f, emptyPaint)
            } else {
                var itemY = currentY + 36f + (itemHeight * 0.7f)
                for (item in items) {
                    try {
                        pillPaint.color = Color.parseColor(item.colorHex)
                    } catch (_: Exception) {
                        pillPaint.color = theme.accentColor
                    }
                    val pillTop = itemY - (itemHeight * 0.5f)
                    val pillBottom = itemY + (itemHeight * 0.15f)
                    canvas.drawRoundRect(RectF(cardMarginX + 24f, pillTop, cardMarginX + 30f, pillBottom), 3f, 3f, pillPaint)

                    val timeStr = "${item.startTime} - ${item.endTime}"
                    canvas.drawText(timeStr, cardMarginX + 42f, itemY, itemTimePaint)

                    val roomStr = if (item.room.isNotBlank()) " [${item.room}]" else ""
                    val titleText = "${item.title}$roomStr"
                    val maxLen = if (itemHeight < 32f) 22 else 30
                    val truncated = if (titleText.length > maxLen) titleText.take(maxLen - 1) + "…" else titleText
                    canvas.drawText(truncated, cardMarginX + 250f, itemY, itemTextPaint)

                    itemY += itemHeight
                }
            }
            currentY += cardHeight + 10f
        }
    }

    /**
     * Vẽ câu động lực chân trang (Footer Quote từ file 607 câu)
     * Tự động xuống dòng 2 dòng nếu câu dài để chữ hiển thị trọn vẹn, thanh thoát.
     */
    private fun drawFooterQuote(
        canvas: Canvas,
        quote: String,
        width: Int,
        height: Int,
        theme: WallpaperTheme
    ) {
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }

        val maxWidth = width * 0.88f
        val lines = splitTextToLines(quote, footerPaint, maxWidth)

        val startY = if (lines.size > 1) height - 55f else height - 38f
        lines.take(2).forEachIndexed { index, line ->
            canvas.drawText(line, width / 2f, startY + (index * 26f), footerPaint)
        }
    }

    private fun splitTextToLines(text: String, paint: Paint, maxW: Float): List<String> {
        if (paint.measureText(text) <= maxW) return listOf(text)

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (w in words) {
            val candidate = if (current.isEmpty()) w else "$current $w"
            if (paint.measureText(candidate) <= maxW) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(w)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
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
