package com.example.wordapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 每日定时触发：发通知，并重新安排下一天的闹钟。
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REVIEW_REMINDER = "com.example.wordapp.ACTION_REVIEW_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.showReviewNotification(context)
        NotificationHelper.reschedule(context)
    }
}
