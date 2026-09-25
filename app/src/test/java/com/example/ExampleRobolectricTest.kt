package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DayOfWeekHelper
import com.example.data.model.RepeatType
import com.example.data.model.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Thời Khóa Biểu", appName)
    }

    @Test
    fun `verify day of week helper`() {
        assertEquals("Thứ Hai", DayOfWeekHelper.getDayNameVi(1))
        assertEquals("Chủ Nhật", DayOfWeekHelper.getDayNameVi(7))
        assertEquals("MO", DayOfWeekHelper.getRruleDayCode(1))
        assertEquals("SU", DayOfWeekHelper.getRruleDayCode(7))
    }

    @Test
    fun `verify repeat types`() {
        val weekly = RepeatType.WEEKLY
        assertEquals("Hàng tuần", weekly.labelVi)
        assertEquals("WEEKLY", weekly.rruleFreq)

        val item = ScheduleItem(
            title = "Lập Trình",
            dayOfWeek = 1,
            startTime = "08:00",
            endTime = "09:30"
        )
        assertTrue(item.isReminderEnabled)
        assertEquals("08:00", item.startTime)
    }
}
