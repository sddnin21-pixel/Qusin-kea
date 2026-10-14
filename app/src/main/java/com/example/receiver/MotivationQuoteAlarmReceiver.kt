package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.MotivationNotificationHelper
import com.example.util.MotivationQuotesHelper
import com.example.util.PreferencesManager

class MotivationQuoteAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager(context)

        // Khi thiết bị khởi động lại (Reboot)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (prefs.isMotivation3hNotificationEnabled()) {
                MotivationNotificationHelper.scheduleNext3HourAlarm(context)
            }
            return
        }

        // Khi chuông báo 3h kích hoạt
        if (prefs.isMotivation3hNotificationEnabled() && MotivationQuotesHelper.isWithinCampaignPeriod()) {
            MotivationNotificationHelper.showMotivationNotification(context)
            // Tự động lập lịch cho 3 tiếng tiếp theo
            MotivationNotificationHelper.scheduleNext3HourAlarm(context)
        } else if (!MotivationQuotesHelper.isWithinCampaignPeriod()) {
            // Nếu đã vượt qua ngày 10/06/2028, tự động hủy lịch
            MotivationNotificationHelper.cancel3HourAlarm(context)
        }
    }
}
