package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testQuotes607FileParse() {
    val candidate1 = java.io.File("src/main/res/raw/quotes_607.txt")
    val candidate2 = java.io.File("app/src/main/res/raw/quotes_607.txt")
    val file = if (candidate1.exists()) candidate1 else candidate2
    assertTrue("quotes_607.txt should exist", file.exists())

    val quotes = mutableListOf<Pair<Int, String>>()
    file.readLines().forEach { line ->
      val trimmed = line.trim()
      if (trimmed.startsWith("Ngày")) {
        val sepIndex = when {
          trimmed.contains("—") -> trimmed.indexOf("—")
          trimmed.contains("–") -> trimmed.indexOf("–")
          trimmed.contains(":") -> trimmed.indexOf(":")
          trimmed.contains("-") -> trimmed.indexOf("-")
          else -> -1
        }
        if (sepIndex != -1) {
          val dayNum = trimmed.substring(0, sepIndex).replace("Ngày", "").trim().toInt()
          val content = trimmed.substring(sepIndex + 1).trim()
          quotes.add(dayNum to content)
        }
      }
    }

    assertEquals("Should parse exactly 607 quotes", 607, quotes.size)
    assertEquals(1, quotes.first().first)
    assertEquals("Có thể hôm nay chẳng ai nhìn thấy nỗ lực của bạn, nhưng một ngày nào đó, chính bạn sẽ biết ơn mình vì đã không bỏ cuộc.", quotes.first().second)
    assertEquals(607, quotes.last().first)
  }

  @Test
  fun testDateCountdownCalculation() {
    val calStart = java.util.Calendar.getInstance().apply {
      set(2026, java.util.Calendar.OCTOBER, 14, 0, 0, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val calEnd = java.util.Calendar.getInstance().apply {
      set(2028, java.util.Calendar.JUNE, 10, 0, 0, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }

    // On 14/10/2026: Day 1
    val dayAtStart = com.example.util.MotivationQuotesHelper.getDayNumberForDate(calStart)
    assertEquals(1, dayAtStart)

    // On 10/06/2028: Day 606
    val dayAtEnd = com.example.util.MotivationQuotesHelper.getDayNumberForDate(calEnd)
    assertEquals(606, dayAtEnd)

    // Days remaining on 14/10/2026 until 10/06/2028: exactly 605 days
    val daysLeftAtStart = com.example.util.MotivationQuotesHelper.getDaysUntilThptExam(calStart.timeInMillis)
    assertEquals(605, daysLeftAtStart)

    // Days remaining on 10/06/2028: 0 days
    val daysLeftAtEnd = com.example.util.MotivationQuotesHelper.getDaysUntilThptExam(calEnd.timeInMillis)
    assertEquals(0, daysLeftAtEnd)
  }
}
