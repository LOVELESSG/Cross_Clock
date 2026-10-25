package com.crossware.crossclock.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.crossware.crossclock.data.RunningTimerData
import com.crossware.crossclock.data.TimerViewModel

/**
 * 计时器列表底部胶囊型常驻状态组件。
 * 1. 左侧圆环进度条中间包含开始/暂停按键。
 * 2. 步骤标识展示为 "1/3" 纯数字格式。
 * 3. 包含跳过当前步骤按键与警示色背景的停止按键。
 * 4. 聚焦卡片支持展开其他运行中的计时器列表。
 */
@Composable
fun ActiveTimerBarWidget(
    timerViewModel: TimerViewModel,
    onOpenRunningScreen: (Long) -> Unit,
    isExpandedListOpen: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTimers = timerViewModel.activeTimers
    if (activeTimers.isEmpty()) return

    val selectedActive = timerViewModel.selectedActiveTimer ?: activeTimers.first()

    AnimatedVisibility(
        visible = activeTimers.isNotEmpty(),
        enter = slideInVertically { height -> height },
        exit = slideOutVertically { height -> height },
        modifier = modifier
    ) {
        TimerWidgetPill(
            runningData = selectedActive,
            activeCount = activeTimers.size,
            showExpandButton = activeTimers.size > 1,
            onPlayPause = {
                if (selectedActive.isRunning && !selectedActive.isPaused) {
                    timerViewModel.pauseTimer(selectedActive.timer.id)
                } else {
                    timerViewModel.resumeTimer(selectedActive.timer.id)
                }
            },
            onNextStep = {
                timerViewModel.skipToNextStep(selectedActive.timer.id)
            },
            onStop = {
                timerViewModel.stopActiveTimer(selectedActive.timer.id)
            },
            onExpandClick = onToggleExpand,
            onCardClick = {
                if (!isExpandedListOpen) {
                    timerViewModel.selectActiveTimer(selectedActive.timer.id)
                    onOpenRunningScreen(selectedActive.timer.id)
                } else {
                    onToggleExpand()
                }
            }
        )
    }
}

/**
 * 独立渲染单条运行中计时器的胶囊卡片。
 */
@Composable
fun TimerWidgetPill(
    runningData: RunningTimerData,
    activeCount: Int,
    showExpandButton: Boolean,
    onPlayPause: () -> Unit,
    onNextStep: () -> Unit,
    onStop: () -> Unit,
    onExpandClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timer = runningData.timer
    val currentStep = runningData.currentStep ?: return
    val remainingSeconds = (runningData.remainingMillis + 999) / 1000
    val totalStepSeconds = currentStep.durationSeconds.coerceAtLeast(1L)
    val stepProgress = ((totalStepSeconds * 1000L - runningData.remainingMillis).toFloat() / (totalStepSeconds * 1000L).toFloat())
        .coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：圆环进度条并在中间放置开始/暂停按键
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { stepProgress },
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 3.dp
                )

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    val isPlaying = runningData.isRunning && !runningData.isPaused
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 中间信息区域：倒计时 + 纯数字步骤 (例如 "1/3") + 计时器及步骤名称
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 倒计时时间
                    Text(
                        text = formatDuration(remainingSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 纯数字步骤胶囊 Label（例如 "1/3"）
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${runningData.currentStepIndex + 1}/${timer.steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 计时器名称 · 当前步骤名称
                Text(
                    text = "${timer.name} · ${currentStep.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 右侧按键区域：下一步 + 停止 + (可选多计时器展开按键)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 下一步按键（跳过当前步骤）
                IconButton(
                    onClick = onNextStep,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Step",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 停止按键（警示背景色）
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Timer",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 多计时器展开按键（当运行数 > 1 时显示）
                if (showExpandButton) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onExpandClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.UnfoldMore,
                                contentDescription = "Expand Active Timers",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "$activeCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
