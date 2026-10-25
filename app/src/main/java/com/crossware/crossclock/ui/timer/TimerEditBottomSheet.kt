package com.crossware.crossclock.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossware.crossclock.R
import com.crossware.crossclock.data.timer.TimerEntity
import com.crossware.crossclock.data.timer.TimerStep
import com.crossware.crossclock.data.timer.TimerType

val PRESET_COLORS = listOf(
    "#3F51B5", // Indigo
    "#FF5722", // Deep Orange
    "#4CAF50", // Green
    "#00BCD4", // Cyan
    "#9C27B0", // Purple
    "#FF9800", // Orange
    "#E91E63", // Pink
    "#607D8B"  // Blue Grey
)

/**
 * 添加/编辑计时器的 BottomSheet 表单。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerEditBottomSheet(
    timer: TimerEntity?,
    onDismiss: () -> Unit,
    onSave: (TimerEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(timer?.name ?: "") }
    var selectedType by remember { mutableStateOf(timer?.type ?: TimerType.SINGLE) }
    var isInfiniteLoop by remember { mutableStateOf(timer?.loopCount == -1) }
    var customLoopInput by remember { mutableStateOf(if (timer != null && timer.loopCount > 0) timer.loopCount.toString() else "3") }

    // 步骤列表状态
    val steps = remember {
        mutableStateListOf<TimerStep>().apply {
            if (timer != null && timer.steps.isNotEmpty()) {
                addAll(timer.steps)
            } else {
                add(TimerStep(name = "步骤 1", durationSeconds = 300, colorHex = "#3F51B5"))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (timer == null) stringResource(R.string.add_timer) else stringResource(R.string.edit_timer),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // 计时器名称
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.timer_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 计时器类型选择
            Column {
                Text(
                    text = stringResource(R.string.timer_type),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == TimerType.SINGLE,
                        onClick = {
                            selectedType = TimerType.SINGLE
                            if (steps.size > 1) {
                                val first = steps.first()
                                steps.clear()
                                steps.add(first)
                            }
                        },
                        label = { Text(stringResource(R.string.timer_type_single)) }
                    )
                    FilterChip(
                        selected = selectedType == TimerType.LINEAR,
                        onClick = { selectedType = TimerType.LINEAR },
                        label = { Text(stringResource(R.string.timer_type_linear)) }
                    )
                    FilterChip(
                        selected = selectedType == TimerType.LOOP,
                        onClick = { selectedType = TimerType.LOOP },
                        label = { Text(stringResource(R.string.timer_type_loop)) }
                    )
                }
            }

            // 循环模式下的循环次数配置
            if (selectedType == TimerType.LOOP) {
                Column {
                    Text(
                        text = stringResource(R.string.loop_count_title),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isInfiniteLoop,
                            onClick = {
                                isInfiniteLoop = !isInfiniteLoop
                            },
                            label = { Text(stringResource(R.string.infinite_loop)) }
                        )

                        if (!isInfiniteLoop) {
                            OutlinedTextField(
                                value = customLoopInput,
                                onValueChange = { input ->
                                    customLoopInput = input.filter { it.isDigit() }
                                },
                                label = { Text(stringResource(R.string.loop_count_label)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // 步骤/单独计时器列表编辑器
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.timer_steps_title),
                        style = MaterialTheme.typography.labelLarge
                    )

                    if (selectedType != TimerType.SINGLE) {
                        TextButton(onClick = {
                            val newIndex = steps.size + 1
                            val newColor = PRESET_COLORS[(steps.size) % PRESET_COLORS.size]
                            steps.add(TimerStep(name = "步骤 $newIndex", durationSeconds = 60, colorHex = newColor))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.add_step))
                        }
                    }
                }

                steps.forEachIndexed { index, step ->
                    StepEditCard(
                        stepIndex = index,
                        step = step,
                        canDelete = selectedType != TimerType.SINGLE && steps.size > 1,
                        onStepChange = { updated -> steps[index] = updated },
                        onDelete = { steps.removeAt(index) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 底部确认保存按钮
            Button(
                onClick = {
                    val finalLoopCount = if (selectedType == TimerType.LOOP) {
                        if (isInfiniteLoop) -1 else (customLoopInput.toIntOrNull()?.coerceAtLeast(1) ?: 1)
                    } else 1

                    val finalTimer = TimerEntity(
                        id = timer?.id ?: 0,
                        name = name.ifBlank { "计时器" },
                        type = selectedType,
                        loopCount = finalLoopCount,
                        steps = steps.toList()
                    )
                    onSave(finalTimer)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = steps.isNotEmpty() && steps.all { it.durationSeconds > 0 }
            ) {
                Text(stringResource(R.string.save_timer))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepEditCard(
    stepIndex: Int,
    step: TimerStep,
    canDelete: Boolean,
    onStepChange: (TimerStep) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "步骤 ${stepIndex + 1}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 步骤名称
            OutlinedTextField(
                value = step.name,
                onValueChange = { onStepChange(step.copy(name = it)) },
                label = { Text(stringResource(R.string.step_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 时长选择（分/秒）
            var minutes by remember(step.durationSeconds) { mutableIntStateOf((step.durationSeconds / 60).toInt()) }
            var seconds by remember(step.durationSeconds) { mutableIntStateOf((step.durationSeconds % 60).toInt()) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = minutes.toString(),
                    onValueChange = {
                        val m = it.toIntOrNull()?.coerceAtLeast(0) ?: 0
                        minutes = m
                        onStepChange(step.copy(durationSeconds = m * 60L + seconds))
                    },
                    label = { Text(stringResource(R.string.minutes)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = seconds.toString(),
                    onValueChange = {
                        val s = it.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        seconds = s
                        onStepChange(step.copy(durationSeconds = minutes * 60L + s))
                    },
                    label = { Text(stringResource(R.string.seconds)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 颜色选择器
            Text(
                text = stringResource(R.string.card_color),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_COLORS.forEach { colorHex ->
                    val color = parseHexColor(colorHex)
                    val isSelected = step.colorHex.equals(colorHex, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                else Modifier
                            )
                            .clickable {
                                onStepChange(step.copy(colorHex = colorHex))
                            }
                    )
                }
            }
        }
    }
}
