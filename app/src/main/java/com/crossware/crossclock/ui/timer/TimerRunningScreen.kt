package com.crossware.crossclock.ui.timer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.crossware.crossclock.R
import com.crossware.crossclock.data.TimerViewModel
import com.crossware.crossclock.data.timer.AlertMode
import com.crossware.crossclock.data.timer.TimerType

/**
 * 全屏倒计时全屏运行画面。
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TimerRunningScreen(
    timerViewModel: TimerViewModel,
    onExit: () -> Unit
) {
    // 拦截系统返回手势/按键：仅返回列表页面，不取消/不停止计时器
    BackHandler {
        onExit()
    }

    val runningData = timerViewModel.selectedActiveTimer ?: return
    val timer = runningData.timer
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    // 沉浸式全屏（自动隐藏系统状态栏与底部导航条）及屏幕常亮
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    val currentStep = runningData.currentStep ?: return
    val nextStepIndex = runningData.currentStepIndex + 1
    val nextStep = if (nextStepIndex < timer.steps.size) {
        timer.steps[nextStepIndex]
    } else if (timer.type == TimerType.LOOP) {
        timer.steps.firstOrNull()
    } else {
        null
    }

    // 滑动手势检测：滑动跳过当前步骤
    var totalOffsetY by remember { mutableFloatStateOf(0f) }
    var totalOffsetX by remember { mutableFloatStateOf(0f) }

    val swipeGestureModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDrag = { change, dragAmount ->
                change.consume()
                totalOffsetY += dragAmount.y
                totalOffsetX += dragAmount.x
            },
            onDragEnd = {
                if (!isLandscape && totalOffsetY < -80f) {
                    timerViewModel.skipToNextStep(timer.id)
                } else if (isLandscape && totalOffsetX < -80f) {
                    timerViewModel.skipToNextStep(timer.id)
                }
                totalOffsetY = 0f
                totalOffsetX = 0f
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        if (!isLandscape) {
            // 竖屏布局：上方顶部按键，中间无边框卡片铺满
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部控制按键栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 点击左上角退出按钮：停止当前计时器并退出全屏界面
                    IconButton(
                        onClick = {
                            timerViewModel.stopActiveTimer(timer.id)
                            onExit()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit and Stop", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 提示模式切换按钮（响铃震动/仅响铃/仅震动/静音）
                        IconButton(onClick = { timerViewModel.cycleAlertMode() }) {
                            val alertIcon = when (timerViewModel.alertMode) {
                                AlertMode.SOUND_AND_VIBRATION -> Icons.Default.NotificationsActive
                                AlertMode.SOUND_ONLY -> Icons.AutoMirrored.Filled.VolumeUp
                                AlertMode.VIBRATION_ONLY -> Icons.Default.Vibration
                                AlertMode.NONE -> Icons.AutoMirrored.Filled.VolumeOff
                            }
                            Icon(alertIcon, contentDescription = "Alert Mode", tint = Color.White)
                        }

                        // 重新开始当前步骤按键
                        IconButton(onClick = { timerViewModel.restartCurrentStep(timer.id) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Current Step", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                if (runningData.isRunning && !runningData.isPaused) {
                                    timerViewModel.pauseTimer(timer.id)
                                } else {
                                    timerViewModel.resumeTimer(timer.id)
                                }
                            }
                        ) {
                            if (runningData.isRunning && !runningData.isPaused) {
                                Icon(
                                    painter = painterResource(R.drawable.outline_timer_24),
                                    contentDescription = "Pause",
                                    tint = Color.White
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }
                }

                // 竖屏卡片区域（滑动切换 & 动画：向上）
                AnimatedContent(
                    targetState = runningData.currentStepIndex to runningData.currentLoopIndex,
                    transitionSpec = {
                        slideInVertically { height -> height } togetherWith slideOutVertically { height -> -height }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .then(swipeGestureModifier)
                ) { (stepIdx, loopIdx) ->
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 主计时器卡片 (占据绝大部分空间)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.75f)
                        ) {
                            RunningStepCard(
                                timerName = timer.name,
                                stepName = currentStep.name,
                                colorHex = currentStep.colorHex,
                                remainingMillis = runningData.remainingMillis,
                                loopStatusText = getLoopStatusText(timer.type, timer.loopCount, runningData.currentLoopIndex),
                                isFinished = runningData.isFinished
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 下一个卡片预览 (占据下方区域)
                        if (nextStep != null && !runningData.isFinished) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.25f)
                            ) {
                                NextStepPreviewCard(
                                    stepName = nextStep.name,
                                    durationSeconds = nextStep.durationSeconds,
                                    colorHex = nextStep.colorHex
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 横屏布局：左侧控制按键栏，右侧无边框卡片（向左滑动）
            Row(modifier = Modifier.fillMaxSize()) {
                // 左侧纵向按键栏
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 点击左上角退出按钮：停止当前计时器并退出全屏界面
                    IconButton(
                        onClick = {
                            timerViewModel.stopActiveTimer(timer.id)
                            onExit()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit and Stop", tint = Color.White)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 提示模式切换按钮
                        IconButton(onClick = { timerViewModel.cycleAlertMode() }) {
                            val alertIcon = when (timerViewModel.alertMode) {
                                AlertMode.SOUND_AND_VIBRATION -> Icons.Default.NotificationsActive
                                AlertMode.SOUND_ONLY -> Icons.AutoMirrored.Filled.VolumeUp
                                AlertMode.VIBRATION_ONLY -> Icons.Default.Vibration
                                AlertMode.NONE -> Icons.AutoMirrored.Filled.VolumeOff
                            }
                            Icon(alertIcon, contentDescription = "Alert Mode", tint = Color.White)
                        }

                        IconButton(onClick = { timerViewModel.restartCurrentStep(timer.id) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Current Step", tint = Color.White)
                        }

                        IconButton(
                            onClick = {
                                if (runningData.isRunning && !runningData.isPaused) {
                                    timerViewModel.pauseTimer(timer.id)
                                } else {
                                    timerViewModel.resumeTimer(timer.id)
                                }
                            }
                        ) {
                            if (runningData.isRunning && !runningData.isPaused) {
                                Icon(
                                    painter = painterResource(R.drawable.outline_timer_24),
                                    contentDescription = "Pause",
                                    tint = Color.White
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            }
                        }
                    }
                }

                // 横屏卡片区域（滑入滑出动画：向左）
                AnimatedContent(
                    targetState = runningData.currentStepIndex to runningData.currentLoopIndex,
                    transitionSpec = {
                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .then(swipeGestureModifier)
                ) { (stepIdx, loopIdx) ->
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(0.7f)
                        ) {
                            RunningStepCard(
                                timerName = timer.name,
                                stepName = currentStep.name,
                                colorHex = currentStep.colorHex,
                                remainingMillis = runningData.remainingMillis,
                                loopStatusText = getLoopStatusText(timer.type, timer.loopCount, runningData.currentLoopIndex),
                                isFinished = runningData.isFinished
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (nextStep != null && !runningData.isFinished) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.3f)
                            ) {
                                NextStepPreviewCard(
                                    stepName = nextStep.name,
                                    durationSeconds = nextStep.durationSeconds,
                                    colorHex = nextStep.colorHex
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningStepCard(
    timerName: String,
    stepName: String,
    colorHex: String,
    remainingMillis: Long,
    loopStatusText: String?,
    isFinished: Boolean
) {
    val cardColor = parseHexColor(colorHex)
    val totalSeconds = (remainingMillis + 999) / 1000

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 头部：计时器名、当前步骤名、循环状态
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Text(
                    text = stepName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (!loopStatusText.isNullOrBlank()) {
                    Text(
                        text = loopStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 中间：醒目大号字体的实时倒数
            Text(
                text = if (isFinished) stringResource(R.string.timer_complete) else formatDuration(totalSeconds),
                fontSize = if (isFinished) 32.sp else 68.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NextStepPreviewCard(
    stepName: String,
    durationSeconds: Long,
    colorHex: String
) {
    val cardColor = parseHexColor(colorHex).copy(alpha = 0.85f)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.next_step, stepName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )

                Text(
                    text = formatDuration(durationSeconds),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun getLoopStatusText(type: TimerType, loopCount: Int, currentLoopIndex: Int): String? {
    if (type != TimerType.LOOP) return null
    return if (loopCount < 0) {
        stringResource(R.string.loop_infinite_status, currentLoopIndex)
    } else {
        stringResource(R.string.loop_status, currentLoopIndex, loopCount)
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
