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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.crossware.crossclock.R
import com.crossware.crossclock.data.ALL_CITIES
import com.crossware.crossclock.data.AlarmState
import com.crossware.crossclock.data.AlarmViewModel
import com.crossware.crossclock.data.alarm.Alarm
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * 闹钟页面的主入口组合项。
 * 负责展示闹钟列表，并根据 [openAddAlarmDialog] 状态决定是否显示添加闹钟的对话框。
 *
 * @param paddingValues 外部传入的内边距。
 * @param scheduler 闹钟调度器，用于向系统设置定时任务。
 * @param openAddAlarmDialog 是否打开添加闹钟对话框。
 * @param onDismissAddAlarmDialog 关闭添加闹钟对话框的回调。
 * @param alarmViewModel 闹钟管理的 ViewModel。
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
    // 渲染闹钟列表内容
    AlarmContent(
        alarmViewModel.state,
        deleteAlarm = alarmViewModel::deleteAlarm,
        padding = paddingValues,
        scheduler = scheduler,
        changeAlarmStatus = alarmViewModel::updateAlarmStatus
    )
    
    // 如果为真，则展示添加闹钟的全屏对话框
    if (openAddAlarmDialog) {
        AddAlarm(
            changeStatus = onDismissAddAlarmDialog,
            addAlarm = alarmViewModel::addAlarm
        )
    }
}

/**
 * 闹钟列表内容的组合项。
 * 支持侧滑删除和开关控制。
 *
 * @param state 闹钟状态，包含闹钟列表。
 * @param deleteAlarm 删除闹钟的回调。
 * @param padding 内边距。
 * @param scheduler 闹钟调度器。
 * @param changeAlarmStatus 更新闹钟启用状态的回调。
 */
@Composable
fun AlarmContent(
    state: AlarmState,
    deleteAlarm: (Alarm) -> Unit,
    padding: PaddingValues,
    scheduler: CrossAlarmScheduler,
    changeAlarmStatus: (Alarm) -> Unit
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
                            item.let(scheduler::cancel) // 取消系统闹钟
                            deleteAlarm(item) // 从数据库删除
                            true
                        } else {
                            false
                        }
                    }
                )
                
                val itemTime = item.time.atZone(item.timeZone)
                val nowTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val compareTime = nowTime.isBefore(itemTime)
                var enableSwitch by remember {
                    mutableStateOf(true)
                }
                
                // 闹钟调度逻辑：如果闹钟开启且时间未到，则设置系统闹钟；否则取消或禁用开关
                if (item.onOrOff && compareTime) {
                    item.let(scheduler::scheduler)
                } else if (!item.onOrOff && compareTime) {
                    item.let(scheduler::cancel)
                } else {
                    item.let(scheduler::cancel)
                    enableSwitch = false // 已过期
                }
                
                // 渲染带滑动手势的闹钟项
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
                        headlineContent = { 
                            Text(
                                text = item.time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)), 
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        supportingContent = {
                            Text(text = item.message)
                        },
                        trailingContent = {
                            // 切换闹钟开关
                            Switch(
                                checked = item.onOrOff,
                                onCheckedChange = {
                                    changeAlarmStatus(item)
                                },
                                enabled = enableSwitch
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
 * 添加闹钟的对话框界面。
 * 提供日期选择、时间输入、时区选择和标签输入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarm(
    changeStatus: () -> Unit,
    addAlarm: (Alarm) -> Unit
) {
    Dialog(
        onDismissRequest = { changeStatus() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false // 使用全屏宽度
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        )
        {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 日期选择状态
                val datePickerState = rememberDatePickerState(
                    initialDisplayMode = DisplayMode.Input,
                    initialSelectedDateMillis = Instant.now().toEpochMilli()
                )
                // 时间选择状态
                val timePickerState = rememberTimePickerState(is24Hour = false)
                
                var expanded by remember { mutableStateOf(false) }
                var selectedOptionText by remember { mutableStateOf("") }
                var selectedTimeZone: ZoneId? by remember { mutableStateOf(null) }
                var message by rememberSaveable { mutableStateOf("") }
                var localDateTime by remember { mutableStateOf(LocalDateTime.now()) }
                
                // 根据选择的日期和时间更新本地日期时间对象
                if (datePickerState.selectedDateMillis != null && selectedTimeZone != null) {
                    localDateTime = LocalDateTime.of(
                        datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        },
                        LocalTime.of(timePickerState.hour, timePickerState.minute)
                    )
                }
                
                var alarmItem: Alarm?
                val context = LocalContext.current
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // 处理通知权限
                var hasNotificationPermission by remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    } else mutableStateOf(true)
                }
                val requestPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasNotificationPermission = isGranted
                    }
                )

                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    ),
                    title = { Text(text = stringResource(R.string.new_alarm)) },
                    navigationIcon = {
                        IconButton(onClick = { changeStatus() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            // 保存闹钟前的逻辑：检查通知权限，然后创建并添加闹钟
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                when (manager.areNotificationsEnabled()) {
                                    true -> {
                                        alarmItem = selectedTimeZone?.let {
                                            Alarm(
                                                time = localDateTime,
                                                message = message,
                                                timeZone = it
                                            )
                                        }
                                        alarmItem?.let { addAlarm(it) }
                                        alarmItem?.let { changeStatus() }
                                    }
                                    false -> {
                                        requestPermissionLauncher.launch(
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                    }
                                }
                            } else {
                                alarmItem = selectedTimeZone?.let {
                                    Alarm(
                                        time = localDateTime,
                                        message = message,
                                        timeZone = it
                                    )
                                }
                                alarmItem?.let { addAlarm(it) }
                                alarmItem?.let { changeStatus() }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Save"
                            )
                        }
                    }
                )

                LazyColumn(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        DatePicker(state = datePickerState, modifier = Modifier.padding(16.dp))
                    }
                    item {
                        TimeInput(state = timePickerState, modifier = Modifier.padding(16.dp))
                    }
                    item {
                        // 时区选择下拉框
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            TextField(
                                readOnly = true,
                                value = selectedOptionText,
                                label = { Text(text = stringResource(R.string.timezone)) },
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier.menuAnchor(
                                    type = MenuAnchorType.PrimaryNotEditable,
                                    enabled = true
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                ALL_CITIES.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = selectionOption.city) },
                                        onClick = {
                                            selectedOptionText = selectionOption.city
                                            selectedTimeZone = selectionOption.cityTimeZoneId
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.size(16.dp)) }
                    item {
                        // 闹钟标签输入
                        TextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text(stringResource(R.string.alarm_label)) },
                            placeholder = { Text(text = stringResource(R.string.alarm_label_hint)) }
                        )
                    }
                }
            }
        }
    }
}
