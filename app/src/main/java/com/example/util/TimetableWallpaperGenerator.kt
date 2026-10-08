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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimetableWallpaperGenerator {

    enum class WallpaperTheme(val displayName: String, val bgStart: Int, val bgEnd: Int, val cardBg: Int, val textPrimary: Int, val textSecondary: Int, val accentColor: Int) {
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

    /**
     * Tạo Bitmap 1080x1920 (chuẩn tỉ lệ 9:16 cho màn hình khóa điện thoại hoặc Instagram/FB Story)
     */
    fun generateWallpaper(
        schedules: List<ScheduleItem>,
        theme: WallpaperTheme = WallpaperTheme.CYBER_DARK,
        customTitle: String = "THỜI KHÓA BIỂU",
        customSubtitle: String = "Lịch Học Cá Nhân"
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Nền gradient
        val bgPaint = Paint().apply { isAntiAlias = true }
        val shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            theme.bgStart, theme.bgEnd,
            android.graphics.Shader.TileMode.CLAMP
        )
        bgPaint.shader = shader
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Trang trí họa tiết nền nhẹ (circles)
        val glowPaint = Paint().apply {
            isAntiAlias = true
            color = theme.accentColor
            alpha = 25
        }
        canvas.drawCircle(width * 0.85f, height * 0.12f, 280f, glowPaint)
        canvas.drawCircle(width * 0.15f, height * 0.85f, 320f, glowPaint)

        // 2. Header
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = theme.accentColor
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✦ SCHEDULE WALLPAPER ✦", width / 2f, 130f, headerPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = theme.textPrimary
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(customTitle, width / 2f, 210f, titlePaint)

        val subPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(customSubtitle, width / 2f, 260f, subPaint)

        // Đường kẻ phân cách
        val dividerPaint = Paint().apply {
            color = theme.accentColor
            alpha = 100
            strokeWidth = 3f
        }
        canvas.drawLine(width * 0.2f, 290f, width * 0.8f, 290f, dividerPaint)

        // 3. Render danh sách các ngày trong tuần (T2 -> CN)
        val dayGroups = (1..7).map { day ->
            day to schedules.filter { it.dayOfWeek == day }.sortedBy { it.startTime }
        }

        var currentY = 330f
        val cardMarginX = 60f
        val cardWidth = width - (cardMarginX * 2)

        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = theme.cardBg
            alpha = 230
        }
        val cardBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = theme.accentColor
            alpha = 70
        }

        val dayHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = theme.accentColor
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val itemTextPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textPrimary
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val itemTimePaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 26f
        }

        val pillPaint = Paint().apply { isAntiAlias = true }

        for ((day, items) in dayGroups) {
            if (currentY > height - 180f) break

            val dayName = DayOfWeekHelper.getDayNameVi(day).uppercase(Locale.getDefault())
            val cardHeight = if (items.isEmpty()) 70f else (60f + items.size * 54f)

            // Draw Card Background
            val cardRect = RectF(cardMarginX, currentY, cardMarginX + cardWidth, currentY + cardHeight)
            canvas.drawRoundRect(cardRect, 20f, 20f, cardPaint)
            canvas.drawRoundRect(cardRect, 20f, 20f, cardBorderPaint)

            // Draw Day Header
            canvas.drawText(dayName, cardMarginX + 30f, currentY + 42f, dayHeaderPaint)

            if (items.isEmpty()) {
                val emptyPaint = Paint().apply {
                    isAntiAlias = true
                    color = theme.textSecondary
                    textSize = 26f
                    alpha = 150
                }
                canvas.drawText("Nghỉ học / Tự do", cardMarginX + 300f, currentY + 42f, emptyPaint)
            } else {
                var itemY = currentY + 86f
                for (item in items) {
                    // Color pill
                    try {
                        pillPaint.color = Color.parseColor(item.colorHex)
                    } catch (e: Exception) {
                        pillPaint.color = theme.accentColor
                    }
                    canvas.drawRoundRect(
                        RectF(cardMarginX + 30f, itemY - 22f, cardMarginX + 40f, itemY + 12f),
                        5f, 5f, pillPaint
                    )

                    // Time badge
                    val timeStr = "${item.startTime} - ${item.endTime}"
                    canvas.drawText(timeStr, cardMarginX + 55f, itemY, itemTimePaint)

                    // Title & Room
                    val roomStr = if (item.room.isNotBlank()) " [P.${item.room}]" else ""
                    val titleText = "${item.title}$roomStr"
                    val truncatedTitle = if (titleText.length > 24) titleText.take(23) + "…" else titleText
                    canvas.drawText(truncatedTitle, cardMarginX + 320f, itemY, itemTextPaint)

                    itemY += 54f
                }
            }

            currentY += cardHeight + 16f
        }

        // 4. Footer Motivational Quote
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = theme.textSecondary
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("✨ Kỷ luật là cây cầu nối giữa mục tiêu và thành công ✨", width / 2f, height - 70f, footerPaint)

        return bitmap
    }

    /**
     * Lưu Bitmap vào cache file để chia sẻ qua Intent ACTION_SEND
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
