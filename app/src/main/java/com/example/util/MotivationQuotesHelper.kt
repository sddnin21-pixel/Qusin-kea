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

    const val START_YEAR = 2026
    const val START_MONTH = Calendar.OCTOBER // Calendar.OCTOBER is 9
    const val START_DAY = 14

    const val END_YEAR = 2028
    const val END_MONTH = Calendar.JUNE // Calendar.JUNE is 5
    const val END_DAY = 10

    /**
     * Lấy số ngày (1..607) dựa trên mốc hành trình 607 ngày: 14/10/2026 -> 10/06/2028
     * Ngày 14/10/2026 tương ứng Ngày 001.
     * Ngày 10/06/2028 tương ứng Ngày 607.
     */
    fun getDayNumberForDate(cal: Calendar): Int {
        val startCal = Calendar.getInstance().apply {
            set(START_YEAR, START_MONTH, START_DAY, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - startCal.timeInMillis
        val days = (diffMillis / (1000L * 60 * 60 * 24)).toInt()
        return (days + 1).coerceIn(1, 607)
    }

    /**
     * Kiểm tra thời điểm có nằm trong giai đoạn thông báo 14/10/2026 đến 10/06/2028 hay không
     */
    fun isWithinCampaignPeriod(timeMs: Long = System.currentTimeMillis()): Boolean {
        val startCal = Calendar.getInstance().apply {
            set(START_YEAR, START_MONTH, START_DAY, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(END_YEAR, END_MONTH, END_DAY, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return timeMs in startCal.timeInMillis..endCal.timeInMillis
    }

    /**
     * Lấy câu động lực của ngày hôm nay (tự động nhảy theo ngày từ 14/10/2026 đến 10/06/2028)
     */
    fun getTodayQuote(context: Context): MotivationQuote {
        val quotes = getAllQuotes(context)
        if (quotes.isEmpty()) return MotivationQuote(1, "Có thể hôm nay chẳng ai nhìn thấy nỗ lực của bạn...")

        val dayNumber = getDayNumberForDate(Calendar.getInstance())
        return getQuoteByDay(context, dayNumber)
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
     * Đếm ngược số ngày đến mốc kết thúc 607 ngày ôn thi THPT Quốc Gia (10/06/2028)
     * Bắt đầu: 14/10/2026 -> Kết thúc: 10/06/2028.
     * Ngày 14/10/2026 là Ngày 001 của hành trình, còn đúng 605 ngày đến ngày 10/06/2028.
     */
    fun getDaysUntilThptExam(nowMs: Long = System.currentTimeMillis()): Int {
        val nowCal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val targetCal = Calendar.getInstance().apply {
            set(END_YEAR, END_MONTH, END_DAY, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = targetCal.timeInMillis - nowCal.timeInMillis
        val days = (diffMillis / (1000L * 60 * 60 * 24)).toInt()
        return days.coerceAtLeast(0)
    }

    /**
     * Lấy mô tả chi tiết tiến trình đếm ngược
     */
    fun getCountdownSummary(nowMs: Long = System.currentTimeMillis()): String {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dayNum = getDayNumberForDate(nowCal)
        val daysLeft = getDaysUntilThptExam(nowMs)
        return "Ngày %03d/607 • Còn %d ngày (đến 10/06/2028)".format(dayNum, daysLeft)
    }
}
