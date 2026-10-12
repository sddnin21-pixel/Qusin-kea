package com.example.util

import android.content.Context
import com.example.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import kotlin.random.Random

data class MotivationQuote(
    val dayNumber: Int,
    val content: String
) {
    val displayDayTitle: String
        get() = String.format("Ngày %03d", dayNumber)
}

object MotivationQuotesHelper {

    private var cachedQuotes: List<MotivationQuote>? = null

    /**
     * Tải toàn bộ 607 câu động lực từ file raw/quotes_607.txt
     */
    fun getAllQuotes(context: Context): List<MotivationQuote> {
        cachedQuotes?.let { return it }

        val list = mutableListOf<MotivationQuote>()
        try {
            val inputStream = context.resources.openRawResource(R.raw.quotes_607)
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotBlank() && trimmed.startsWith("Ngày")) {
                        // Hỗ trợ cả hai định dạng: "Ngày 001 — Nội dung..." và "Ngày 001: Nội dung..."
                        val separatorIndex = when {
                            trimmed.contains("—") -> trimmed.indexOf("—")
                            trimmed.contains("–") -> trimmed.indexOf("–")
                            trimmed.contains(":") -> trimmed.indexOf(":")
                            trimmed.contains("-") -> trimmed.indexOf("-")
                            else -> -1
                        }
                        if (separatorIndex != -1) {
                            val dayPart = trimmed.substring(0, separatorIndex).trim()
                            val contentPart = trimmed.substring(separatorIndex + 1).trim()
                            val dayNum = dayPart.replace("Ngày", "").trim().toIntOrNull() ?: (list.size + 1)
                            list.add(MotivationQuote(dayNumber = dayNum, content = contentPart))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (list.isEmpty()) {
            list.add(
                MotivationQuote(
                    dayNumber = 1,
                    content = "Có thể hôm nay chẳng ai nhìn thấy nỗ lực của bạn, nhưng một ngày nào đó, chính bạn sẽ biết ơn mình vì đã không bỏ cuộc."
                )
            )
        }

        cachedQuotes = list
        return list
    }

    /**
     * Lấy câu động lực của ngày hôm nay (tự động nhảy câu mỗi ngày theo lịch)
     */
    fun getTodayQuote(context: Context): MotivationQuote {
        val quotes = getAllQuotes(context)
        if (quotes.isEmpty()) return MotivationQuote(1, "Kiên trì hôm nay, thành công ngày mai.")

        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        // Tính chỉ số ngày luân phiên qua các năm
        val totalDays = (year - 2026) * 365 + dayOfYear
        val index = (Math.abs(totalDays) % quotes.size)
        return quotes[index]
    }

    /**
     * Lấy câu theo số ngày cụ thể (1..607)
     */
    fun getQuoteByDay(context: Context, dayNumber: Int): MotivationQuote {
        val quotes = getAllQuotes(context)
        val clampedDay = dayNumber.coerceIn(1, quotes.size)
        return quotes.find { it.dayNumber == clampedDay } ?: quotes[clampedDay - 1]
    }

    /**
     * Lấy ngẫu nhiên một câu động lực để truyền cảm hứng tức thì
     */
    fun getRandomQuote(context: Context): MotivationQuote {
        val quotes = getAllQuotes(context)
        return if (quotes.isNotEmpty()) {
            quotes[Random.nextInt(quotes.size)]
        } else {
            MotivationQuote(1, "Kiên trì hôm nay, thành công ngày mai.")
        }
    }

    /**
     * Tìm kiếm câu theo từ khóa
     */
    fun searchQuotes(context: Context, query: String): List<MotivationQuote> {
        val quotes = getAllQuotes(context)
        if (query.isBlank()) return quotes
        val q = query.trim().lowercase()
        return quotes.filter {
            it.content.lowercase().contains(q) || it.dayNumber.toString().contains(q)
        }
    }

    /**
     * Đếm ngược số ngày đến kỳ thi THPT Quốc Gia (cuối tháng 6 hàng năm)
     */
    fun getDaysUntilThptExam(): Int {
        val now = Calendar.getInstance()
        val examCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 26)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (now.after(examCal)) {
            examCal.add(Calendar.YEAR, 1)
        }
        val diffMillis = examCal.timeInMillis - now.timeInMillis
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }
}
