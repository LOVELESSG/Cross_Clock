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
import java.util.logging.Handler

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository: Repository = Graph.repository
        val scheduler = CrossAlarmScheduler(context)
        Log.d("before the if: ", (intent.action == "android.intent.action.BOOT_COMPLETED").toString())
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            /*GlobalScope.launch {
                var alarms = repository.allAlarm.last()
                Log.d("before the loop: ", alarms.toString())
                for (alarm in alarms){
                    if (alarm.onOrOff) {
                        alarm.let(scheduler::scheduler)
                    }
                }
            }*/
            /*Log.d("before the block: ", "yes")
            runBlocking {
                //Log.d("before the init: ","yes" )

                Log.d("before the collect: ", "yes")
                repository.allAlarm.collectLatest {
                    Log.d("before the assign:", repository.allAlarm.last().toString())
                    alarms = alarms.copy(
                        items = it
                    )
                }
                Log.d("before the loop: ", alarms.toString())
                for (alarm in alarms.items){
                    if (alarm.onOrOff) {
                        alarm.let(scheduler::scheduler)
                    }
                }
            }*/

            //scheduler.resetAllAlarms()
            reSetAllAlarms(context)
        }
    }

    private fun reSetAllAlarms(context: Context){

        val task = object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void?): Void? {
                val db = AppDatabase.getDatabase(context)
                val alarms = db.alarmDao().loadAllAlarmInList()

                for (alarm in alarms) {
                    if (alarm.onOrOff) {
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


