package com.crossware.crossclock.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.compose.runtime.mutableStateOf
import com.crossware.crossclock.data.StopWatchViewModel
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

/**
 * 秒表后台服务，负责处理计时逻辑和维护计时状态。
 * 使用 @AndroidEntryPoint 注解，以便 Hilt 注入依赖项。
 */
@AndroidEntryPoint
class StopWatchService: Service() {

    // 注入 StopWatchViewModel 以便在服务中同步更新 UI 状态
    @Inject
    lateinit var viewModel: StopWatchViewModel

    private val binder = StopwatchBinder()
    private lateinit var timer: Timer
    private var duration: Duration = ZERO

    // 使用 Compose 的 mutableStateOf 以便 UI 能直接观察到这些值的变化
    // 秒数状态 （00-59）
    var seconds = mutableStateOf("00")
        private set
    // 分钟状态 （00-59）
    var minutes = mutableStateOf("00")
        private set
    // 小时状态 （00-59）
    var hours = mutableStateOf("00")
        private set
    // 当前秒表运行状态（Idle、Started、Stopped、Canceled）
    var currentState = mutableStateOf(StopWatchState.Idle)
        private set

    /**
     * 绑定服务，返回 binder 实例，允许 Activity 直接调用服务的方法。
     */
    override fun onBind(p0: Intent?) = binder

    /**
     * 处理启动指令，支持通过 Intent 的 Extra 或 Action 来控制秒表。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. 根据传入的状态名执行对应操作
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

        // 2. 根据预定义的 Action 执行对应操作
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

    /**
     * 启动秒表计时器
     */
    private fun startStopwatch(){
        currentState.value = StopWatchState.Started
        viewModel.updateState(StopWatchState.Started)

        // 创建一个每秒执行一次的定时器
        timer = fixedRateTimer(initialDelay = 1000L, period = 1000L) {
            duration = duration.plus(1.seconds) // 增加1秒
            updateTimeUnits() // 更新时间显示
            // 同步更新 ViewModel 中的时间数据
            viewModel.updateTime(hours.value, minutes.value, seconds.value)
        }
    }

    /**
     * 停止/暂停计时
     */
    private fun stopStopwatch() {
        if (this::timer.isInitialized) {
            timer.cancel() // 取消定时器任务
        }
        currentState.value = StopWatchState.Stopped
        viewModel.updateState(StopWatchState.Stopped)
    }

    /**
     * 取消计时并重置数据
     */
    private fun cancelStopwatch() {
        duration = ZERO // 重置时长为 0
        currentState.value = StopWatchState.Idle
        viewModel.updateState(StopWatchState.Idle)
        updateTimeUnits() // 更新显示为 00:00:00
        viewModel.updateTime(hours.value, minutes.value, seconds.value)
    }

    /**
     * 将 Duration 转换为小时，分钟，秒，并格式化为两位数的字符串
     */
    private fun updateTimeUnits() {
        duration.toComponents { hours, minutes, seconds, _ ->
            this@StopWatchService.hours.value = hours.toInt().pad()
            this@StopWatchService.minutes.value = minutes.pad()
            this@StopWatchService.seconds.value = seconds.pad()
        }
    }

    /**
     * 用于跨组件通信的内部类，方便 Activity 获取 Service 实例。
     */
    inner class StopwatchBinder: Binder() {
        fun getService(): StopWatchService = this@StopWatchService
    }
}

/**
 * 定义秒表可能的几种状态
 */
enum class StopWatchState {
    Idle,
    Started,
    Stopped,
    Canceled
}