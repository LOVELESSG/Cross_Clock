package com.crossware.crossclock.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossware.crossclock.MainActivity
import com.crossware.crossclock.R
import com.crossware.crossclock.data.timer.AlertMode
import com.crossware.crossclock.data.timer.TimerEntity
import com.crossware.crossclock.data.timer.TimerStep
import com.crossware.crossclock.data.timer.TimerType
import com.crossware.crossclock.ui.timer.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

const val ACTION_TIMER_PLAY_PAUSE = "com.crossware.crossclock.ACTION_TIMER_PLAY_PAUSE"
const val ACTION_TIMER_STOP = "com.crossware.crossclock.ACTION_TIMER_STOP"
const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
const val EXTRA_TIMER_ID = "extra_timer_id"
const val ROUTE_TIMER_RUNNING = "timer_running_screen"

data class TimerState(
    val items: List<TimerEntity> = emptyList()
)

data class RunningTimerData(
    val timer: TimerEntity,
    val currentStepIndex: Int = 0,
    val currentLoopIndex: Int = 1, // 1-based
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false
) {
    val currentStep: TimerStep?
        get() = timer.steps.getOrNull(currentStepIndex)
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: Repository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var state by mutableStateOf(TimerState())
        private set

    // 多计时器并发运行状态映射 (timerId -> RunningTimerData)
    val activeTimersMap = mutableStateMapOf<Long, RunningTimerData>()

    // 当前组件/全屏选中的计时器 ID
    var selectedActiveTimerId by mutableStateOf<Long?>(null)

    // 所有未完成的活跃计时器列表
    val activeTimers: List<RunningTimerData>
        get() = activeTimersMap.values.filter { !it.isFinished }

    // 当前选中的活跃计时器数据
    val selectedActiveTimer: RunningTimerData?
        get() {
            val id = selectedActiveTimerId
            return if (id != null) activeTimersMap[id] else activeTimers.firstOrNull()
        }

    var alertMode by mutableStateOf(AlertMode.SOUND_AND_VIBRATION)
        private set

    private var countdownJob: Job? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val timerId = intent.getLongExtra(EXTRA_TIMER_ID, -1L)
            if (timerId == -1L) return

            when (intent.action) {
                ACTION_TIMER_PLAY_PAUSE -> {
                    val data = activeTimersMap[timerId] ?: return
                    if (data.isRunning && !data.isPaused) {
                        pauseTimer(timerId)
                    } else if (data.isPaused) {
                        resumeTimer(timerId)
                    }
                }
                ACTION_TIMER_STOP -> {
                    stopActiveTimer(timerId)
                }
            }
        }
    }

    init {
        getAllTimers()

        val filter = IntentFilter().apply {
            addAction(ACTION_TIMER_PLAY_PAUSE)
            addAction(ACTION_TIMER_STOP)
        }
        ContextCompat.registerReceiver(
            context,
            notificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onCleared() {
        super.onCleared()
        try {
            context.unregisterReceiver(notificationActionReceiver)
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun getAllTimers() {
        viewModelScope.launch {
            repository.allTimers.collectLatest {
                state = state.copy(items = it)
            }
        }
    }

    fun addTimer(timer: TimerEntity) {
        viewModelScope.launch {
            repository.insertTimer(timer)
        }
    }

    fun updateTimer(timer: TimerEntity) {
        viewModelScope.launch {
            repository.updateTimer(timer)
        }
    }

    fun deleteTimer(timer: TimerEntity) {
        viewModelScope.launch {
            repository.deleteTimer(timer)
        }
    }

    fun cycleAlertMode() {
        alertMode = when (alertMode) {
            AlertMode.SOUND_AND_VIBRATION -> AlertMode.SOUND_ONLY
            AlertMode.SOUND_ONLY -> AlertMode.VIBRATION_ONLY
            AlertMode.VIBRATION_ONLY -> AlertMode.NONE
            AlertMode.NONE -> AlertMode.SOUND_AND_VIBRATION
        }
    }

    fun selectActiveTimer(timerId: Long) {
        if (activeTimersMap.containsKey(timerId)) {
            selectedActiveTimerId = timerId
        }
    }

    /**
     * 启动或开始运行指定的计时器。
     */
    fun startActiveTimer(timer: TimerEntity, initialStepIndex: Int = 0) {
        if (timer.steps.isEmpty()) return

        val stepIndex = initialStepIndex.coerceIn(0, timer.steps.size - 1)
        val stepDurationMillis = timer.steps[stepIndex].durationSeconds * 1000L

        val runningData = RunningTimerData(
            timer = timer,
            currentStepIndex = stepIndex,
            currentLoopIndex = 1,
            remainingMillis = stepDurationMillis,
            isRunning = true,
            isPaused = false,
            isFinished = false
        )

        activeTimersMap[timer.id] = runningData
        selectedActiveTimerId = timer.id

        updateOngoingNotificationForTimer(runningData)
        ensureTickerRunning()
    }

    fun pauseTimer(timerId: Long) {
        val data = activeTimersMap[timerId] ?: return
        if (data.isRunning && !data.isPaused) {
            val updated = data.copy(isRunning = false, isPaused = true)
            activeTimersMap[timerId] = updated
            updateOngoingNotificationForTimer(updated)
        }
    }

    fun resumeTimer(timerId: Long) {
        val data = activeTimersMap[timerId] ?: return
        if (data.isPaused) {
            val updated = data.copy(isRunning = true, isPaused = false)
            activeTimersMap[timerId] = updated
            updateOngoingNotificationForTimer(updated)
            ensureTickerRunning()
        }
    }

    /**
     * 重新开始指定计时器的当前步骤。
     */
    fun restartCurrentStep(timerId: Long) {
        val data = activeTimersMap[timerId] ?: return
        val step = data.currentStep ?: return

        val updated = data.copy(
            remainingMillis = step.durationSeconds * 1000L,
            isRunning = true,
            isPaused = false,
            isFinished = false
        )
        activeTimersMap[timerId] = updated
        updateOngoingNotificationForTimer(updated)
        ensureTickerRunning()
    }

    fun skipToNextStep(timerId: Long) {
        val data = activeTimersMap[timerId] ?: return
        advanceStep(data)
    }

    fun stopActiveTimer(timerId: Long) {
        activeTimersMap.remove(timerId)
        cancelNotificationForTimer(timerId)
        if (selectedActiveTimerId == timerId) {
            selectedActiveTimerId = activeTimers.firstOrNull()?.timer?.id
        }
    }

    private fun ensureTickerRunning() {
        if (countdownJob?.isActive == true) return
        countdownJob = viewModelScope.launch {
            val interval = 100L
            while (activeTimersMap.values.any { it.isRunning && !it.isPaused }) {
                delay(interval)
                val currentRunningList = activeTimersMap.values.filter { it.isRunning && !it.isPaused }.toList()
                for (runningData in currentRunningList) {
                    val newRemaining = runningData.remainingMillis - interval
                    if (newRemaining <= 0) {
                        playStepCompleteAlert()
                        advanceStep(runningData)
                    } else {
                        val updated = runningData.copy(remainingMillis = newRemaining)
                        activeTimersMap[runningData.timer.id] = updated
                        if (newRemaining % 1000 < interval) {
                            updateOngoingNotificationForTimer(updated)
                        }
                    }
                }
            }
        }
    }

    private fun advanceStep(runningData: RunningTimerData) {
        val timer = runningData.timer
        val nextStepIndex = runningData.currentStepIndex + 1

        if (nextStepIndex < timer.steps.size) {
            val nextStep = timer.steps[nextStepIndex]
            val updated = runningData.copy(
                currentStepIndex = nextStepIndex,
                remainingMillis = nextStep.durationSeconds * 1000L
            )
            activeTimersMap[timer.id] = updated
            updateOngoingNotificationForTimer(updated)
        } else {
            // 当前循环所有步骤已走完
            if (timer.type == TimerType.LOOP) {
                val isInfinite = timer.loopCount < 0
                val nextLoop = runningData.currentLoopIndex + 1
                if (isInfinite || nextLoop <= timer.loopCount) {
                    val firstStep = timer.steps.first()
                    val updated = runningData.copy(
                        currentStepIndex = 0,
                        currentLoopIndex = nextLoop,
                        remainingMillis = firstStep.durationSeconds * 1000L
                    )
                    activeTimersMap[timer.id] = updated
                    updateOngoingNotificationForTimer(updated)
                    return
                }
            }
            // 计时全流程结束：播放声音/震动，发送一次性完成通知，并自动将该计时器从活跃与聚焦列表中移除
            playFinalCompleteAlert()
            cancelNotificationForTimer(timer.id)
            showCompletionNotificationForTimer(timer)

            activeTimersMap.remove(timer.id)
            if (selectedActiveTimerId == timer.id) {
                selectedActiveTimerId = activeTimers.firstOrNull()?.timer?.id
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "timer_running_channel",
                "Timer Running Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun notificationIdForTimer(timerId: Long): Int {
        return (timerId % 10000).toInt() + 1000
    }

    private fun updateOngoingNotificationForTimer(runningData: RunningTimerData) {
        val timer = runningData.timer
        val currentStep = runningData.currentStep ?: return
        createNotificationChannel()

        val remainingSeconds = (runningData.remainingMillis + 999) / 1000
        val title = "${timer.name} - ${currentStep.name}"
        val content = if (runningData.isFinished) {
            context.getString(R.string.timer_complete)
        } else if (runningData.isPaused) {
            "${context.getString(R.string.notification_pause)}: ${formatDuration(remainingSeconds)}"
        } else {
            "${context.getString(R.string.drawer_item4)}: ${formatDuration(remainingSeconds)}"
        }

        // 点击通知主体，一键跳转到对应计时器的全屏界面
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, ROUTE_TIMER_RUNNING)
            putExtra(EXTRA_TIMER_ID, timer.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationIdForTimer(timer.id),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "timer_running_channel")
            .setSmallIcon(R.drawable.outline_timer_24)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(!runningData.isFinished)
            .setAutoCancel(runningData.isFinished)

        // 仅在正在运行或暂停状态下提供快捷操作按键，已完成状态不提供任何按钮
        if (!runningData.isFinished) {
            val playPauseIntent = Intent(ACTION_TIMER_PLAY_PAUSE).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_TIMER_ID, timer.id)
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationIdForTimer(timer.id) * 10 + 1,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(ACTION_TIMER_STOP).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_TIMER_ID, timer.id)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationIdForTimer(timer.id) * 10 + 2,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val playPauseLabel = if (runningData.isRunning && !runningData.isPaused) {
                context.getString(R.string.notification_pause)
            } else {
                context.getString(R.string.notification_resume)
            }

            builder.addAction(0, playPauseLabel, playPausePendingIntent)
            builder.addAction(0, context.getString(R.string.notification_stop), stopPendingIntent)
        }

        try {
            notificationManager.notify(notificationIdForTimer(timer.id), builder.build())
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun showCompletionNotificationForTimer(timer: TimerEntity) {
        createNotificationChannel()

        val title = "${timer.name} - ${context.getString(R.string.timer_complete)}"
        val content = context.getString(R.string.timer_complete)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationIdForTimer(timer.id),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 运行完成通知：独立通知，不提供任何快捷按钮
        val builder = NotificationCompat.Builder(context, "timer_running_channel")
            .setSmallIcon(R.drawable.outline_timer_24)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .setAutoCancel(true)

        try {
            notificationManager.notify(notificationIdForTimer(timer.id), builder.build())
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun cancelNotificationForTimer(timerId: Long) {
        try {
            notificationManager.cancel(notificationIdForTimer(timerId))
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun playStepCompleteAlert() {
        val shouldPlaySound = alertMode == AlertMode.SOUND_AND_VIBRATION || alertMode == AlertMode.SOUND_ONLY
        val shouldVibrate = alertMode == AlertMode.SOUND_AND_VIBRATION || alertMode == AlertMode.VIBRATION_ONLY

        if (shouldPlaySound) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtone.play()
            } catch (e: Exception) {
                // ignore
            }
        }

        if (shouldVibrate) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun playFinalCompleteAlert() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
        } catch (e: Exception) {
            // ignore
        }

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            // ignore
        }
    }
}
