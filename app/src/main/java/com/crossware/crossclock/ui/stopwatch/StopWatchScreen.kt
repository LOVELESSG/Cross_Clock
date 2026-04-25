package com.crossware.crossclock.ui.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crossware.crossclock.R
import com.crossware.crossclock.data.StopWatchViewModel
import com.crossware.crossclock.service.ServiceHelper
import com.crossware.crossclock.service.StopWatchState
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_CANCEL
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_START
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_STOP

/**
 * 秒表主界面 Composable。
 * 负责注入 ViewModel 并将其传递给内容组件。
 *
 * @param paddingValues 来自 Scaffold 的内边距，用于确保内容不被导航栏遮挡。
 * @param viewModel 通过 Hilt 自动注入的秒表逻辑管理器。
 */
@Composable
fun StopWatchScreen(
    paddingValues: PaddingValues,
    viewModel: StopWatchViewModel = hiltViewModel()
) {
    StopWatchContent(viewModel, paddingValues)
}

/**
 * 秒表具体内容界面。
 * 包含时间显示区（带动画效果）和操作控制区（启动/停止/取消按钮）。
 */
@Composable
fun StopWatchContent(viewModel: StopWatchViewModel, padding: PaddingValues) {

    val context = LocalContext.current
    // 观察 ViewModel 中的时间状态
    val hours by viewModel.hours
    val minutes by viewModel.minutes
    val seconds by viewModel.seconds
    // 观察当前秒表的运行状态（启动、停止、空闲等）
    val currentState by viewModel.currentState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(padding), // 应用导航内边距
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. 时间显示区域 ---
        Row(
            modifier = Modifier.weight(weight = 2f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间单位（时、分、秒）的样式
            val unitStyle = TextStyle(
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 小时显示（带垂直滑动切换动画）
            AnimatedContent(targetState = hours, transitionSpec = { addAnimation() }, label = "HoursAnimation") { targetCount ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = targetCount,
                        style = TextStyle(
                            fontSize = MaterialTheme.typography.displayLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (hours == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = stringResource(R.string.stopwatch_hour_unit),
                        style = unitStyle,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp, end = 8.dp)
                    )
                }
            }

            // 分钟显示
            AnimatedContent(targetState = minutes, transitionSpec = { addAnimation() },
                label = "MinutesAnimation"
            ) { targetCount ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = targetCount,
                        style = TextStyle(
                            fontSize = MaterialTheme.typography.displayLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (minutes == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = stringResource(R.string.stopwatch_minute_unit),
                        style = unitStyle,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp, end = 8.dp)
                    )
                }
            }

            // 秒钟显示
            AnimatedContent(targetState = seconds, transitionSpec = { addAnimation() },
                label = "SecondsAnimation"
            ) { targetCount ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = targetCount,
                        style = TextStyle(
                            fontSize = MaterialTheme.typography.displayLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = if (seconds == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = stringResource(R.string.stopwatch_second_unit),
                        style = unitStyle,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )
                }
            }
        }

        // --- 2. 按钮控制区域 ---
        Row(
            modifier = Modifier.weight(weight = 1f),
            horizontalArrangement = Arrangement.Center
        ) {
            // 启动 / 暂停 / 继续 按钮
            Button(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                onClick = {
                    // 根据当前状态，通过 ServiceHelper 切换秒表运行或停止
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = if (currentState == StopWatchState.Started) ACTION_SERVICE_STOP
                        else ACTION_SERVICE_START
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    contentColor = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(
                    text = when (currentState) {
                        StopWatchState.Started -> stringResource(R.string.stopwatch_stop)
                        StopWatchState.Stopped -> stringResource(R.string.stopwatch_resume)
                        else -> stringResource(R.string.stopwatch_start)
                    }
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 取消 / 重置 按钮
            Button(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                onClick = {
                    // 触发取消动作，重置计时数据
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_CANCEL
                    )
                },
                // 仅当非计时状态且有时间累积时才启用重置
                enabled = seconds != "00" && currentState != StopWatchState.Started,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(text = stringResource(R.string.stopwatch_cancel))
            }
        }
    }
}

/**
 * 为时间切换提供垂直滑动淡入淡出的动画配置。
 */
fun addAnimation(duration: Int = 100): ContentTransform {
    return slideInVertically(animationSpec = tween(durationMillis = duration)) { height -> height } + fadeIn(
        animationSpec = tween(durationMillis = duration)
    ) togetherWith slideOutVertically(animationSpec = tween(durationMillis = duration)) { height -> height } + fadeOut(
        animationSpec = tween(durationMillis = duration)
    )
}
