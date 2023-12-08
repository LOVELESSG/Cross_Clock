package com.example.crossclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.crossclock.data.AppDatabase
import com.example.crossclock.ui.alarm.AlarmReceiver
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            reSetAllAlarms(context)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun reSetAllAlarms(context: Context){
        GlobalScope.launch {
            val db = AppDatabase.getDatabase(context)
            val alarms = db.alarmDao().loadAllAlarmInList()

            for (alarm in alarms) {
                val alarmTime = alarm.time.atZone(alarm.timeZone)
                val nowTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val compareTime = nowTime.isBefore(alarmTime)
                if (alarm.onOrOff && compareTime) {
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val alarmIntent = Intent(context, AlarmReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarm.id,
                        alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

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


