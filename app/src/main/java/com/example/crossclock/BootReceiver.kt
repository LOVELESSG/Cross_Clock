package com.example.crossclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Looper
import android.util.Log
import com.example.crossclock.data.AppDatabase
import com.example.crossclock.data.Repository
import com.example.crossclock.data.alarm.Alarm
import com.example.crossclock.ui.alarm.AlarmReceiver
import com.example.crossclock.ui.alarm.CrossAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.logging.Handler

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            reSetAllAlarms(context)
        }
    }

    private fun reSetAllAlarms(context: Context){
        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void?): Void? {
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
                return null
            }
        }

        task.execute()
    }
}


