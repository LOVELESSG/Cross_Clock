package com.crossware.crossclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.crossware.crossclock.data.AppDatabase
import com.crossware.crossclock.ui.alarm.AlarmReceiver
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 开机广播接收器。
 * 当设备重启完成后，负责从数据库加载并恢复所有已设置的闹钟。
 */
class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 检查广播动作是否为开机完成
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            reSetAllAlarms(context)
        }
    }

    /**
     * 重新设置所有符合条件的闹钟。
     * 由于需要访问数据库，因此在协程中执行。
     */
    @OptIn(DelicateCoroutinesApi::class)
    private fun reSetAllAlarms(context: Context){
        GlobalScope.launch {
            // 获取数据库实例
            val db = AppDatabase.getDatabase(context)
            // 同步加载所有闹钟记录
            val alarms = db.alarmDao().loadAllAlarmInList()

            for (alarm in alarms) {
                val alarmTime = alarm.time.atZone(alarm.timeZone)
                val nowTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val compareTime = nowTime.isBefore(alarmTime)
                
                // 仅当闹钟标记为开启且时间尚未过期时，才重新排程
                if (alarm.onOrOff && compareTime) {
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("message", alarm.message)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarm.id,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // 再次安排精确闹钟
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarm.time.atZone(alarm.timeZone).toEpochSecond() * 1000,
                        pendingIntent
                    )
                }
            }
        }
    }
}
