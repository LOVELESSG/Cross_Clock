package com.crossware.crossclock.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import com.crossware.crossclock.data.StopWatchViewModel
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_CANCEL
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_START
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_STOP
import com.crossware.crossclock.util.Constants.STOPWATCH_STATE
import dagger.hilt.android.AndroidEntryPoint
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer

/**
 * 秒表后台服务，负责高精度毫秒级走时逻辑与状态切换。
 */
@AndroidEntryPoint
class StopWatchService: Service() {

    @Inject
    lateinit var viewModel: StopWatchViewModel

    private val binder = StopwatchBinder()
    private var timer: Timer? = null

    private var accumulativeMillis = 0L
    private var startTimeMillis = 0L

    var currentState = mutableStateOf(StopWatchState.Idle)
        private set

    override fun onBind(p0: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(STOPWATCH_STATE)) {
            StopWatchState.Started.name -> startStopwatch()
            StopWatchState.Stopped.name -> stopStopwatch()
            StopWatchState.Canceled.name -> cancelStopwatch()
        }

        intent?.action?.let {
            when (it) {
                ACTION_SERVICE_START -> startStopwatch()
                ACTION_SERVICE_STOP -> stopStopwatch()
                ACTION_SERVICE_CANCEL -> cancelStopwatch()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startStopwatch() {
        if (currentState.value == StopWatchState.Started) return
        currentState.value = StopWatchState.Started
        viewModel.updateState(StopWatchState.Started)

        startTimeMillis = SystemClock.elapsedRealtime()

        timer?.cancel()
        timer = fixedRateTimer(initialDelay = 10L, period = 10L) {
            val now = SystemClock.elapsedRealtime()
            val total = accumulativeMillis + (now - startTimeMillis)
            viewModel.updateElapsedMillis(total)
        }
    }

    private fun stopStopwatch() {
        if (currentState.value == StopWatchState.Started) {
            val now = SystemClock.elapsedRealtime()
            accumulativeMillis += (now - startTimeMillis)
        }
        timer?.cancel()
        timer = null
        currentState.value = StopWatchState.Stopped
        viewModel.updateState(StopWatchState.Stopped)
        viewModel.updateElapsedMillis(accumulativeMillis)
    }

    private fun cancelStopwatch() {
        timer?.cancel()
        timer = null
        accumulativeMillis = 0L
        startTimeMillis = 0L
        currentState.value = StopWatchState.Idle
        viewModel.resetStopwatch()
    }

    inner class StopwatchBinder : Binder() {
        fun getService(): StopWatchService = this@StopWatchService
    }
}

enum class StopWatchState {
    Idle,
    Started,
    Stopped,
    Canceled
}
