package com.crossware.crossclock.ui.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationCompat
import com.crossware.crossclock.FullScreenAlarmNotification
import com.crossware.crossclock.R
import com.crossware.crossclock.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 闹钟广播接收器。
 * 当系统 [AlarmManager] 到达设定时间时触发，立即启动响铃服务与悬浮通知/全屏界面。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val alarmId = intent.getIntExtra("id", -1)
        val ringtoneUriStr = intent.getStringExtra("ringtoneUri")
        val ringtoneTitle = intent.getStringExtra("ringtoneTitle")
        val message = intent.getStringExtra("message") ?: "闹钟"
        val time = intent.getStringExtra("time") ?: ""

        // 1. 立即启动响铃播放服务（无论亮屏还是锁屏均立刻响起）
        AlarmRingtoneService.startRingtone(context, ringtoneUriStr)

        // 2. 若为单次固定日期闹钟，触发后立刻更新数据库将该闹钟标注为已关闭/过期
        if (alarmId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val alarmList = db.alarmDao().loadAllAlarmInList()
                    val targetAlarm = alarmList.find { it.id == alarmId }
                    if (targetAlarm != null && targetAlarm.repeatDays.isNullOrBlank()) {
                        db.alarmDao().updateAlarmStatus(alarmId, false)
                    }
                } catch (e: Exception) {
                    // ignore
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            pendingResult.finish()
        }

        // 3. 构建全屏响铃界面 Intent 与通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alarm_channel"
            val channelName = "Alarm Notification"
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            channel.setSound(null, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, FullScreenAlarmNotification::class.java).apply {
            putExtra("alarmMessage", message)
            putExtra("ringtoneTitle", ringtoneTitle)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            if (alarmId != -1) alarmId else 0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val deleteIntent = Intent(context, AlarmCancelReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            if (alarmId != -1) alarmId else 1,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setContentTitle(message)
            .setContentText(time)
            .setSmallIcon(R.drawable.baseline_alarm_on_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSound(null)
            .setDeleteIntent(deletePendingIntent)
            .setTimeoutAfter(30000)

        notificationManager.notify(24778, notification.build())
    }
}
