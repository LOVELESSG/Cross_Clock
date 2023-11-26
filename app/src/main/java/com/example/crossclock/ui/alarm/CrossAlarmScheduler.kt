package com.example.crossclock.ui.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.crossclock.Graph
import com.example.crossclock.data.AppDatabase
import com.example.crossclock.data.Repository
import com.example.crossclock.data.alarm.Alarm
import kotlinx.coroutines.flow.last

class CrossAlarmScheduler(
    private val context: Context
): AlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val intent = Intent(context, AlarmReceiver::class.java)
    override fun scheduler(item: Alarm) {
        //val intent = Intent(context, AlarmReceiver::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            item.time.atZone(item.timeZone).toEpochSecond() * 1000,
            PendingIntent.getBroadcast(
                context,
                item.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    override fun cancel(item: Alarm) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                item.id,
                intent,
                //Intent(context, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        Log.d("cancel hashcode: ", item.id.toString())
    }

    /*override fun resetAllAlarms() {
        val db = AppDatabase.getDatabase(context)
        val alarmList = db.alarmDao().loadAllAlarmInList()
        for (alarm in alarmList){
            if (alarm.onOrOff) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarm.time.atZone(alarm.timeZone).toEpochSecond() * 1000,
                    PendingIntent.getBroadcast(
                        context,
                        alarm.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }
    }*/
}