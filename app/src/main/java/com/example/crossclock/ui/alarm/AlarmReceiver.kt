package com.example.crossclock.ui.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.crossclock.FullScreenAlarmNotification
import com.example.crossclock.R

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, alarmSound)

        val message = intent.getStringExtra("message")
        val time = intent.getStringExtra("time")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alarm_channel"
            val channelName = "Alarm Notification"
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
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
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
            fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val deleteIntent = Intent(context, AlarmCancelReceiver::class.java).apply {
        }
        val deletePendingIntent = PendingIntent.getBroadcast(context, 1, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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

        val alarmNotification = notification.build()
        notificationManager.notify(24778, alarmNotification)
        ringtone.play()
    }
}