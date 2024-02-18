package com.crossware.crossclock.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_CANCEL
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_START
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_STOP
import com.crossware.crossclock.util.Constants.STOPWATCH_STATE
import com.crossware.crossclock.util.pad
import dagger.hilt.android.AndroidEntryPoint
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

class StopWatchService: Service() {

    private val binder = StopwatchBinder()
    private lateinit var timer: Timer
    private var duration: Duration = ZERO
    var seconds = mutableStateOf("00")
        private set
    var minutes = mutableStateOf("00")
        private set
    var hours = mutableStateOf("00")
        private set
    var currentState = mutableStateOf(StopWatchState.Idle)
        private set

    override fun onBind(p0: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(STOPWATCH_STATE)) {
            StopWatchState.Started.name -> {
                startStopwatch()
            }
            StopWatchState.Stopped.name -> {
                stopStopwatch()
            }
            StopWatchState.Canceled.name -> {
                cancelStopwatch()
            }
        }
        intent?.action.let {
            when (it) {
                ACTION_SERVICE_START -> {
                    startStopwatch()
                }
                ACTION_SERVICE_STOP -> {
                    stopStopwatch()
                }
                ACTION_SERVICE_CANCEL -> {
                    cancelStopwatch()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startStopwatch(){
    //private fun startStopwatch(onTick: (h: String, m: String, s: String) -> Unit) {
        currentState.value = StopWatchState.Started
        timer = fixedRateTimer(initialDelay = 1000L, period = 1000L) {
            duration = duration.plus(1.seconds)
            updateTimeUnits()
            //onTick(hours.value, minutes.value, seconds.value)
        }
    }

    private fun stopStopwatch() {
        if (this::timer.isInitialized) {
            timer.cancel()
        }
        currentState.value = StopWatchState.Stopped
    }

    private fun cancelStopwatch() {
        duration = ZERO
        currentState.value = StopWatchState.Idle
        updateTimeUnits()
    }

    private fun updateTimeUnits() {
        duration.toComponents { hours, minutes, seconds, _ ->
            this@StopWatchService.hours.value = hours.toInt().pad()
            this@StopWatchService.minutes.value = minutes.pad()
            this@StopWatchService.seconds.value = seconds.pad()
        }
    }

    inner class StopwatchBinder: Binder() {
        fun getService(): StopWatchService = this@StopWatchService
    }
}

enum class StopWatchState {
    Idle,
    Started,
    Stopped,
    Canceled
}