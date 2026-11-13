package com.example.wordapp.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机完成后重新注册每日提醒（闹钟在关机后会丢失）。
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.reschedule(context)
        }
    }
}
