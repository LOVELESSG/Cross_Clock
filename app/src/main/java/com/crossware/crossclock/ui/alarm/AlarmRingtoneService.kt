package com.crossware.crossclock.ui.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.crossware.crossclock.R

/**
 * 响铃前台服务，专门控制闹钟铃声的精准循环播放与立刻打断停止。
 */
class AlarmRingtoneService : Service() {

    companion object {
        const val ACTION_PLAY_ALARM = "com.crossware.crossclock.ACTION_PLAY_ALARM"
        const val ACTION_STOP_ALARM = "com.crossware.crossclock.ACTION_STOP_ALARM"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"

        fun startRingtone(context: Context, ringtoneUriStr: String?) {
            val intent = Intent(context, AlarmRingtoneService::class.java).apply {
                action = ACTION_PLAY_ALARM
                putExtra(EXTRA_RINGTONE_URI, ringtoneUriStr)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRingtone(context: Context) {
            val intent = Intent(context, AlarmRingtoneService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_ALARM -> {
                val uriStr = intent.getStringExtra(EXTRA_RINGTONE_URI)
                promoteToForeground()
                playRingtone(uriStr)
            }
            ACTION_STOP_ALARM -> {
                stopRingtoneInternal()
            }
        }
        return START_NOT_STICKY
    }

    private fun promoteToForeground() {
        val channelId = "alarm_ringtone_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alarm Ringtone Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("闹钟响铃中")
            .setContentText("正在播放闹钟铃声")
            .setSmallIcon(R.drawable.outline_timer_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSound(null)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(24779, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(24779, notification)
            }
        } catch (e: Exception) {
            Log.e("AlarmRingtoneService", "Failed to start foreground service", e)
        }
    }

    private fun playRingtone(uriStr: String?) {
        stopAudioEngine()

        try {
            var targetUri: Uri? = if (!uriStr.isNullOrBlank()) {
                try { Uri.parse(uriStr) } catch (e: Exception) { null }
            } else null

            if (targetUri == null) {
                targetUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (targetUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, targetUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } else {
                playFallbackRingtone()
            }
        } catch (e: Exception) {
            Log.e("AlarmRingtoneService", "MediaPlayer failed, attempting fallback RingtoneManager", e)
            playFallbackRingtone()
        }
    }

    private fun playFallbackRingtone() {
        try {
            val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            fallbackRingtone = RingtoneManager.getRingtone(applicationContext, fallbackUri)
            fallbackRingtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                fallbackRingtone?.isLooping = true
            }
            fallbackRingtone?.play()
        } catch (e: Exception) {
            Log.e("AlarmRingtoneService", "Fallback ringtone failed", e)
        }
    }

    private fun stopAudioEngine() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("AlarmRingtoneService", "Error stopping MediaPlayer", e)
        }

        try {
            fallbackRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            fallbackRingtone = null
        } catch (e: Exception) {
            Log.e("AlarmRingtoneService", "Error stopping fallback Ringtone", e)
        }
    }

    private fun stopRingtoneInternal() {
        stopAudioEngine()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopRingtoneInternal()
        super.onDestroy()
    }
}
