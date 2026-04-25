package com.crossware.crossclock.ui.alarm

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
import com.crossware.crossclock.FullScreenAlarmNotification
import com.crossware.crossclock.R

/**
 * 闹钟广播接收器。
 * 当系统 [AlarmManager] 到达设定时间时，会发送广播触发此类，执行通知和响铃。
 */
class AlarmReceiver: BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        // 获取系统默认闹钟铃声
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtone = RingtoneManager.getRingtone(context, alarmSound)

        // 从 Intent 中解析闹钟信息
        val message = intent.getStringExtra("message")
        val time = intent.getStringExtra("time")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Android O 及以上版本需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alarm_channel"
            val channelName = "Alarm Notification"
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            // 配置音频属性
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            channel.setSound(null, audioAttributes)
            notificationManager.createNotificationChannel(channel)
        }

        // 创建全屏 Intent，用于在锁屏状态下直接显示闹钟界面
        val fullScreenIntent = Intent(context, FullScreenAlarmNotification::class.java).apply {
            putExtra("alarmMessage", message)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(context, 0,
            fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        // 创建删除（取消）Intent，当通知被移除时触发
        val deleteIntent = Intent(context, AlarmCancelReceiver::class.java)
        val deletePendingIntent = PendingIntent.getBroadcast(context, 1, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // 构建通知
        val notification = NotificationCompat.Builder(context, "alarm_channel")
            .setContentTitle(message)
            .setContentText(time)
            .setSmallIcon(R.drawable.baseline_alarm_on_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // 锁屏时全屏显示
            .setSound(null) // 铃声通过 RingtoneManager 播放，此处置空避免冲突
            .setDeleteIntent(deletePendingIntent)
            .setTimeoutAfter(30000) // 30秒后自动超时

        val alarmNotification = notification.build()
        // 发送通知
        notificationManager.notify(24778, alarmNotification)
        // 播放铃声
        ringtone.play()
    }
}
