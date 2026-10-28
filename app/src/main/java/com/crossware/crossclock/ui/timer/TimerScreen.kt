package com.crossware.crossclock.ui.timer

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossware.crossclock.R
import com.crossware.crossclock.data.TimerViewModel
import com.crossware.crossclock.data.timer.TimerEntity
import com.crossware.crossclock.data.timer.TimerType

/**
 * 计时器主界面。
 */
@Composable
fun TimerScreen(
    paddingValues: PaddingValues,
    timerViewModel: TimerViewModel,
    onStartTimer: (TimerEntity) -> Unit,
    onEditTimer: (TimerEntity) -> Unit,
    onOpenRunningScreen: (Long) -> Unit,
    onAddTimerClick: () -> Unit,
    isExpandedListOpen: Boolean = false,
    onToggleExpand: () -> Unit = {}
) {
    val state = timerViewModel.state
    val activeTimers = timerViewModel.activeTimers

    var timerPendingDelete by remember { mutableStateOf<TimerEntity?>(null) }

    // 删除确认对话框
    if (timerPendingDelete != null) {
        val targetTimer = timerPendingDelete!!
        AlertDialog(
            onDismissRequest = { timerPendingDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text, targetTimer.name)) },
            confirmButton = {
                TextButton(onClick = {
                    timerViewModel.deleteTimer(targetTimer)
                    timerPendingDelete = null
                }) {
                    Text(stringResource(R.string.confirm_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { timerPendingDelete = null }) {
                    Text(stringResource(R.string.cancel_select_time))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        if (state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_timer_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.id }) { timer ->
                        TimerItemCard(
                            timer = timer,
                            onStartTimer = { onStartTimer(timer) },
                            onEditTimer = { onEditTimer(timer) },
                            onDeleteTimer = { timerPendingDelete = timer }
                        )
                    }
                }

                // 底部常驻活跃计时器状态组件 + 右侧 FAB 悬浮按键组合布局
                if (activeTimers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ActiveTimerBarWidget(
                            timerViewModel = timerViewModel,
                            onOpenRunningScreen = onOpenRunningScreen,
                            isExpandedListOpen = isExpandedListOpen,
                            onToggleExpand = onToggleExpand,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        FloatingActionButton(
                            onClick = onAddTimerClick,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Timer")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerItemCard(
    timer: TimerEntity,
    onStartTimer: () -> Unit,
    onEditTimer: () -> Unit,
    onDeleteTimer: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            // 折叠头部：名称、类型 Badge、格式化总时长、快速开始按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val typeLabel = when (timer.type) {
                            TimerType.SINGLE -> stringResource(R.string.timer_type_single)
                            TimerType.LINEAR -> stringResource(R.string.timer_type_linear)
                            TimerType.LOOP -> if (timer.loopCount < 0) {
                                stringResource(R.string.infinite_loop)
                            } else {
                                stringResource(R.string.loop_x_times, timer.loopCount)
                            }
                        }

                        AssistChip(
                            onClick = { },
                            label = { Text(typeLabel, style = MaterialTheme.typography.labelSmall) }
                        )

                        Text(
                            text = formatDuration(timer.totalDurationSeconds),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 折叠状态下的快速进入按钮
                if (!isExpanded) {
                    IconButton(onClick = onStartTimer) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.enter_timer),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 时间流导轨预览
            TimeRailBar(steps = timer.steps)

            // 展开形态：展现详细时间线及更多操作按键
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // 时间线展示
                TimerTimeline(steps = timer.steps)

                Spacer(modifier = Modifier.height(12.dp))

                // 操作按键区：修改、删除、进入计时
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDeleteTimer) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_timer),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    IconButton(onClick = onEditTimer) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_timer)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onStartTimer) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.enter_timer))
                    }
                }
            }
        }
    }
}
