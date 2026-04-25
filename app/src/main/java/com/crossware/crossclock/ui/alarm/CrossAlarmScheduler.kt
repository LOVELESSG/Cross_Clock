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
    // 创建指向 AlarmReceiver 的 Intent，当闹钟触发时由系统发出广播
    private val intent = Intent(context, AlarmReceiver::class.java)

    /**
     * 安排一个精确的闹钟。
     * 使用 setExactAndAllowWhileIdle 确保在设备处于低功耗模式时也能触发。
     *
     * @param item 包含闹钟时间、消息和时区信息的闹钟对象。
     */
    override fun scheduler(item: Alarm) {
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, // 使用真实时间触发，并唤醒设备
            item.time.atZone(item.timeZone).toEpochSecond() * 1000, // 转换为毫秒时间戳
            PendingIntent.getBroadcast(
                context,
                item.id, // 使用闹钟 ID 作为请求码，以便后续取消或更新
                intent.apply {
                    // 将闹钟信息放入 Intent 传递给 Receiver
                    putExtra("message", item.message)
                    putExtra("time", item.time.toLocalTime().toString())
                    putExtra("id", item.id)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    /**
     * 取消一个已安排的闹钟。
     *
     * @param item 要取消的闹钟对象。
     */
    override fun cancel(item: Alarm) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                item.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        Log.d("cancel hashcode: ", item.id.toString())
    }
}
