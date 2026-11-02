package com.crossware.crossclock.ui.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.crossware.crossclock.data.alarm.Alarm

/**
 * 闹钟调度器实现类。
 * 使用系统的 [AlarmManager] 来安排和取消闹钟任务。
 */
class CrossAlarmScheduler(
    private val context: Context
): AlarmScheduler {
    // 获取系统的闹钟服务
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 安排一个精确的闹钟。
     * 基于闹钟的目标唤醒时区 (timeZone) 精准计算下一次触发的绝对 UTC 时间戳。
     */
    override fun scheduler(item: Alarm) {
        if (!item.onOrOff || item.isExpired()) return

        val triggerMillis = item.getNextTriggerZonedDateTime().toInstant().toEpochMilli()
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("message", item.message)
            putExtra("time", item.time.toLocalTime().toString())
            putExtra("id", item.id)
            putExtra("ringtoneUri", item.ringtoneUri)
            putExtra("ringtoneTitle", item.ringtoneTitle)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
        Log.d("CrossAlarmScheduler", "Scheduled alarm id: ${item.id} at $triggerMillis")
    }

    /**
     * 取消一个已安排的闹钟。
     */
    override fun cancel(item: Alarm) {
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d("CrossAlarmScheduler", "Canceled alarm id: ${item.id}")
    }
}
