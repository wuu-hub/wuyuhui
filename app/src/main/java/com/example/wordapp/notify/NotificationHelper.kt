package com.example.wordapp.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.wordapp.MainActivity
import com.example.wordapp.R
import com.example.wordapp.data.WordRepository
import java.util.Calendar

/**
 * 通知与每日定时调度。
 * 用 AlarmManager 在每天固定时间触发 [AlarmReceiver]，由其发出复习通知。
 */
object NotificationHelper {

    const val CHANNEL_ID = "review_reminder"
    private const val CHANNEL_NAME = "复习提醒"
    private const val NOTIF_ID = 1001
    private const val REQUEST_CODE = 200
    private const val PREFS = "reminder_prefs"

    private fun alarmIntent(context: Context): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REVIEW_REMINDER
        }

    /** 创建通知渠道（Android 8.0+ 必需） */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "每天提醒你按时复习到期单词"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /** 发出复习提醒通知，内容包含当前到期单词数 */
    fun showReviewNotification(context: Context) {
        ensureChannel(context)
        val due = WordRepository.get(context).dueCount()
        val text = if (due > 0)
            "今天有 $due 个单词到期复习，现在开始吧！"
        else
            "今天没有到期单词，来学几个新词吧！"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("该背单词啦")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, notification)
    }

    /**
     * 设置每天 hour:minute 的重复提醒。
     * Android 12+ 没有精确闹钟权限时自动退化为非精确闹钟，仍可正常提醒。
     */
    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        ensureChannel(context)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("enabled", true)
            .putInt("hour", hour).putInt("minute", minute).apply()

        val triggerAt = nextTriggerAt(hour, minute)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, alarmIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            alarmManager.canScheduleExactAlarms() else true

        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            // 无精确权限：用可接受的非精确窗口，保证功能可用
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    /** 取消每日提醒 */
    fun cancel(context: Context) {
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_CODE, alarmIntent(context),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pending)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("enabled", false).apply()
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("enabled", false)

    /** 开机/触发后按保存的时间重新安排 */
    fun reschedule(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean("enabled", false)) {
            scheduleDaily(
                context,
                prefs.getInt("hour", 19),
                prefs.getInt("minute", 0)
            )
        }
    }

    private fun nextTriggerAt(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }
}
