package com.example.crossclock.ui.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.crossclock.FullScreenAlarmNotification
import com.example.crossclock.MainActivity
import com.example.crossclock.R
import kotlin.system.exitProcess

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, alarmSound)

        val message = intent.getStringExtra("message")
        val time = intent.getStringExtra("time")
        val alarmId = intent.getIntExtra("id", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alarm_channel"
            val channelName = "channel for alarm"
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
            //.setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            //.setSound(alarmSound)
            .setSound(null)
            .setDeleteIntent(deletePendingIntent)
            //.setWhen(System.currentTimeMillis()+15000)
            //.setOnlyAlertOnce(false)
            //.setTimeoutAfter(30000)

        val alarmNotification = notification.build()
        notificationManager.notify(24778, alarmNotification)
        /*if (notificationManager.areNotificationsEnabled()) {
            ringtone.play()
        } else {
            ringtone.stop()
        }*/
        ringtone.play()
    }
}