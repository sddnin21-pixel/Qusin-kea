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
    val file = java.io.File("src/main/res/raw/quotes_607.txt")
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
}
