package com.crossware.crossclock.ui.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crossware.crossclock.R
import com.crossware.crossclock.data.StopWatchViewModel
import com.crossware.crossclock.data.stopwatch.LapItem
import com.crossware.crossclock.service.ServiceHelper
import com.crossware.crossclock.service.StopWatchState
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_CANCEL
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_START
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_STOP
import java.util.Locale

/**
 * 将毫秒数解析为 (分钟:秒钟, .百分之几秒) 格式的字符串对。
 */
fun splitTimeComponents(totalMillis: Long, locale: Locale = Locale.getDefault()): Pair<String, String> {
    val totalCentiseconds = totalMillis / 10
    val centiseconds = totalCentiseconds % 100
    val totalSeconds = totalMillis / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60
    val mainPart = String.format(locale, "%02d:%02d", minutes, seconds)
    val subPart = String.format(locale, ".%02d", centiseconds)
    return Pair(mainPart, subPart)
}

/**
 * 秒表主界面 Composable。
 */
@Composable
fun StopWatchScreen(
    paddingValues: PaddingValues,
    viewModel: StopWatchViewModel = hiltViewModel()
) {
    StopWatchContent(viewModel, paddingValues)
}

/**
 * 秒表内容主界面。
 */
@Composable
fun StopWatchContent(viewModel: StopWatchViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]

    val elapsedMillis by viewModel.elapsedMillis
    val currentState by viewModel.currentState
    val laps by viewModel.laps

    val (mainTimeStr, subTimeStr) = splitTimeComponents(elapsedMillis, locale)

    // 计算当前计次单圈用时
    val lastLapTotalMillis = laps.firstOrNull()?.totalTimeMillis ?: 0L
    val currentLapMillis = (elapsedMillis - lastLapTotalMillis).coerceAtLeast(0L)
    val currentLapComponents = splitTimeComponents(currentLapMillis, locale)
    val currentLapFormatted = "${currentLapComponents.first}${currentLapComponents.second}"

    // 计算圆环进度 (1圈 = 60秒)
    val ringProgress = ((elapsedMillis % 60000L).toFloat() / 60000f).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(padding)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. 主圆环仪表盘 display ---
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // 背景底环
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 6.dp
            )

            // 动态进度环
            CircularProgressIndicator(
                progress = { ringProgress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 大号时间字号 (05:33 .45)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = mainTimeStr,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subTimeStr,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 当前计次单圈 Pill 标签
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_timer_24),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.stopwatch_current_lap, currentLapFormatted),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 2. 常驻悬浮按钮区域 (重置 - 开始/暂停 - 计次分圈，仅图标，无文字) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：重置/清空按钮
            IconButton(
                onClick = {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_CANCEL
                    )
                },
                enabled = elapsedMillis > 0L,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (elapsedMillis > 0L) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.stopwatch_cancel),
                    tint = if (elapsedMillis > 0L) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }

            // 中间：主开始/暂停/继续大按钮
            IconButton(
                onClick = {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = if (currentState == StopWatchState.Started) ACTION_SERVICE_STOP else ACTION_SERVICE_START
                    )
                },
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (currentState == StopWatchState.Started) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (currentState == StopWatchState.Started) stringResource(R.string.stopwatch_stop) else stringResource(R.string.stopwatch_start),
                    tint = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }

            // 右侧：分圈/计次按钮
            IconButton(
                onClick = {
                    viewModel.recordLap()
                },
                enabled = currentState == StopWatchState.Started,
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Record Lap",
                    tint = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 3. 历史计次列表区域 ---
        if (laps.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 头部栏：标题 + 计次数量 Badge + 清除按键
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.stopwatch_history_laps),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.stopwatch_laps_count, laps.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        TextButton(onClick = { viewModel.clearLaps() }) {
                            Text(
                                text = stringResource(R.string.stopwatch_clear_history),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 历史分圈数据列表
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(laps, key = { "lap_${it.lapIndex}" }) { lap ->
                            val (lapMain, lapSub) = splitTimeComponents(lap.lapTimeMillis, locale)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = String.format(locale, "%02d", lap.lapIndex),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    if (lap.isBest) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "★ ${stringResource(R.string.stopwatch_best_lap)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = stringResource(R.string.stopwatch_segment_record),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = lapMain,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = lapSub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
