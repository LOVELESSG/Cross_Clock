package com.crossware.crossclock.ui.alarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.crossware.crossclock.R
import com.crossware.crossclock.data.ALL_CITIES
import com.crossware.crossclock.data.AlarmState
import com.crossware.crossclock.data.AlarmViewModel
import com.crossware.crossclock.data.alarm.Alarm
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 闹钟页面的主入口组合项。
 * 负责展示闹钟列表，并处理添加或编辑闹钟的底部动作条。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    paddingValues: PaddingValues,
    scheduler: CrossAlarmScheduler,
    openAddAlarmDialog: Boolean,
    onDismissAddAlarmDialog: () -> Unit,
    alarmViewModel: AlarmViewModel = hiltViewModel()
) {
    // 当前正在编辑的闹钟（如果是新建，则 id 为 0）
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    // 当外部触发“添加闹钟”时，初始化一个新的闹钟对象
    LaunchedEffect(openAddAlarmDialog) {
        if (openAddAlarmDialog) {
            val now = LocalDateTime.now()
            // 默认时间为当前时间的后一个小时
            val defaultTime = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
            editingAlarm = Alarm(
                time = defaultTime,
                message = "",
                timeZone = ZoneId.systemDefault(),
                onOrOff = true
            )
        }
    }

    // 渲染闹钟列表内容
    AlarmContent(
        alarmViewModel.state,
        deleteAlarm = alarmViewModel::deleteAlarm,
        padding = paddingValues,
        scheduler = scheduler,
        changeAlarmStatus = alarmViewModel::updateAlarmStatus,
        onEditAlarm = { alarm ->
            editingAlarm = alarm
        }
    )
    
    // 如果有正在编辑或新建的闹钟，显示底部动作条
    if (editingAlarm != null) {
        AlarmEditSheet(
            alarm = editingAlarm!!,
            onDismiss = {
                editingAlarm = null
                if (openAddAlarmDialog) onDismissAddAlarmDialog()
            },
            onSave = { updatedAlarm ->
                if (updatedAlarm.id == 0) {
                    alarmViewModel.addAlarm(updatedAlarm)
                } else {
                    alarmViewModel.updateAlarm(updatedAlarm)
                }
                editingAlarm = null
                if (openAddAlarmDialog) onDismissAddAlarmDialog()
            }
        )
    }
}

/**
 * 闹钟列表内容的组合项。
 */
@Composable
fun AlarmContent(
    state: AlarmState,
    deleteAlarm: (Alarm) -> Unit,
    padding: PaddingValues,
    scheduler: CrossAlarmScheduler,
    changeAlarmStatus: (Alarm) -> Unit,
    onEditAlarm: (Alarm) -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn {
            items(state.items, key = { it.id }) { item ->
                // 实现侧滑删除的逻辑
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            item.let(scheduler::cancel)
                            deleteAlarm(item)
                            true
                        } else {
                            false
                        }
                    }
                )
                
                val itemTime = item.time.atZone(item.timeZone)
                val nowTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val compareTime = nowTime.isBefore(itemTime)
                
                // 调度逻辑：在副作用中处理，避免在 Composition 期间直接执行
                LaunchedEffect(item.onOrOff, item.time, item.timeZone) {
                    if (item.onOrOff && compareTime) {
                        scheduler.scheduler(item)
                    } else {
                        scheduler.cancel(item)
                    }
                }

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val direction = dismissState.dismissDirection
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                SwipeToDismissBoxValue.StartToEnd -> Color.Green
                                SwipeToDismissBoxValue.EndToStart -> Color.Red
                                else -> Color.LightGray
                            }, label = "dismissBackground"
                        )
                        val alignment = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                        val icon = when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                            SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                            else -> Icons.Default.Done
                        }

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = alignment
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                            )
                        }
                    },
                    enableDismissFromStartToEnd = false
                ) {
                    ListItem(
                        modifier = Modifier.clickable { onEditAlarm(item) },
                        headlineContent = { 
                            Text(
                                text = item.time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)), 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ) 
                        },
                        supportingContent = {
                            Text(text = item.message)
                        },
                        trailingContent = {
                            Switch(
                                checked = item.onOrOff,
                                onCheckedChange = {
                                    changeAlarmStatus(item)
                                },
                                enabled = compareTime // 直接根据是否过期决定开关是否可用
                            )
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 0.dp))
            }
        }
    }
}

/**
 * 编辑或添加闹钟的底部动作条。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    // 编辑状态
    var selectedTime by remember { mutableStateOf(alarm.time.toLocalTime()) }
    var selectedDate by remember { mutableStateOf(alarm.time.toLocalDate()) }
    var selectedTimeZone by remember { mutableStateOf<ZoneId?>(if (alarm.id != 0) alarm.timeZone else null) }
    var message by remember { mutableStateOf(alarm.message) }
    
    // 对话框控制
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var timezoneExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 闹钟时间
            ListItem(
                headlineContent = { Text("闹钟时间", style = MaterialTheme.typography.labelMedium) },
                supportingContent = { 
                    Text(
                        text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.displaySmall
                    )
                },
                trailingContent = {
                    Button(onClick = { showTimePicker = true }) {
                        Text("修改")
                    }
                }
            )

            // 2. 闹钟日期
            ListItem(
                modifier = Modifier.clickable { showDatePicker = true },
                headlineContent = { Text("闹钟日期", style = MaterialTheme.typography.labelMedium) },
                supportingContent = {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            )

            // 3. 时区选择器
            ExposedDropdownMenuBox(
                expanded = timezoneExpanded,
                onExpandedChange = { timezoneExpanded = !timezoneExpanded }
            ) {
                TextField(
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    readOnly = true,
                    value = ALL_CITIES.find { it.cityTimeZoneId == selectedTimeZone }?.city ?: "请选择时区",
                    label = { Text("时区") },
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = timezoneExpanded,
                    onDismissRequest = { timezoneExpanded = false }
                ) {
                    ALL_CITIES.forEach { cityInfo ->
                        DropdownMenuItem(
                            text = { Text(cityInfo.city) },
                            onClick = {
                                selectedTimeZone = cityInfo.cityTimeZoneId
                                timezoneExpanded = false
                            }
                        )
                    }
                }
            }

            // 4. 闹钟标签
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = message,
                onValueChange = { message = it },
                label = { Text("闹钟标签") },
                placeholder = { Text("例如：起床、开会") }
            )

            // 5. 完成按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        val finalDateTime = LocalDateTime.of(selectedDate, selectedTime)
                        onSave(alarm.copy(
                            time = finalDateTime,
                            message = message,
                            timeZone = selectedTimeZone!!
                        ))
                    },
                    enabled = selectedTimeZone != null
                ) {
                    Text("完成")
                }
            }
            
            Spacer(modifier = Modifier.size(32.dp))
        }
    }

    // 时间选择器对话框
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        text = "选择时间",
                        style = MaterialTheme.typography.labelMedium
                    )
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("取消") }
                        TextButton(onClick = {
                            selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) { Text("确定") }
                    }
                }
            }
        }
    }

    // 日期选择器对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
