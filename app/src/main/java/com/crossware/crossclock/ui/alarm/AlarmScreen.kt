package com.crossware.crossclock.ui.alarm

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
 * 负责展示闹钟列表，处理权限检测，并管理添加/编辑闹钟的逻辑流。
 * 
 * @param paddingValues 外部（Scaffold）传入的内边距。
 * @param scheduler 闹钟调度器，用于与系统 AlarmManager 交互。
 * @param openAddAlarmDialog 是否由外部触发打开“添加闹钟”界面。
 * @param onDismissAddAlarmDialog 关闭添加界面的回调。
 * @param alarmViewModel 闹钟数据管理的 ViewModel。
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
    val context = LocalContext.current
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    
    // 权限状态实时追踪
    var hasPermission by remember { mutableStateOf(notificationManager.areNotificationsEnabled()) }

    // 监听应用生命周期，确保当用户在系统设置中更改权限并返回应用时，UI 能及时刷新。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = notificationManager.areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 全局权限同步逻辑：
    // 1. 如果权限被关闭，立即取消系统中所有已设置的闹钟，防止无效调度。
    // 2. 如果权限重新开启，则自动恢复所有处于“开启”状态且未过期的闹钟。
    LaunchedEffect(hasPermission, alarmViewModel.state.items) {
        alarmViewModel.state.items.forEach { item ->
            val itemTime = item.time.atZone(item.timeZone)
            val nowTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
            val compareTime = nowTime.isBefore(itemTime)
            
            if (hasPermission && item.onOrOff && compareTime) {
                scheduler.scheduler(item)
            } else {
                scheduler.cancel(item)
            }
        }
    }

    // 当前正在编辑的闹钟。如果是新建，则其 ID 为默认值 0。
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    // 响应外部添加指令
    LaunchedEffect(openAddAlarmDialog) {
        if (openAddAlarmDialog) {
            val now = LocalDateTime.now()
            // 默认设置为下一整点
            val defaultTime = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
            editingAlarm = Alarm(
                time = defaultTime,
                message = "",
                timeZone = ZoneId.systemDefault(),
                onOrOff = true
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        // 如果缺失通知权限，显示警告卡片，引导用户前往系统设置
        if (!hasPermission) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "通知权限已关闭", fontWeight = FontWeight.Bold)
                        Text(text = "闹钟将无法触发提醒，请开启权限。", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("去设置")
                    }
                }
            }
        }

        // 主列表区域
        AlarmContent(
            alarmViewModel.state,
            deleteAlarm = alarmViewModel::deleteAlarm,
            padding = PaddingValues(0.dp),
            scheduler = scheduler,
            changeAlarmStatus = alarmViewModel::updateAlarmStatus,
            onEditAlarm = { alarm ->
                editingAlarm = alarm
            }
        )
    }
    
    // 显示底部编辑/添加动作条
    if (editingAlarm != null) {
        AlarmEditSheet(
            alarm = editingAlarm!!,
            onDismiss = {
                editingAlarm = null
                if (openAddAlarmDialog) onDismissAddAlarmDialog()
            },
            onSave = { updatedAlarm ->
                // 根据 ID 判断是更新已有闹钟还是插入新闹钟
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
 * 渲染闹钟列表内容，包括侧滑删除手势。
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
                // 实现左滑删除功能
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
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        supportingContent = {
                            Text(text = item.message)
                        },
                        trailingContent = {
                            // 仅当闹钟时间在当前时间之后时，开关才允许启用
                            Switch(
                                checked = item.onOrOff,
                                onCheckedChange = {
                                    changeAlarmStatus(item)
                                },
                                enabled = compareTime
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
 * 添加或修改闹钟信息的底部动作条。
 * 包含时间、日期、时区选择，以及标签输入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    // 内部状态维护，在保存前仅作为草稿
    var selectedTime by remember { mutableStateOf(alarm.time.toLocalTime()) }
    var selectedDate by remember { mutableStateOf(alarm.time.toLocalDate()) }
    var selectedTimeZone by remember { mutableStateOf<ZoneId?>(if (alarm.id != 0) alarm.timeZone else null) }
    var message by remember { mutableStateOf(alarm.message) }
    
    // 子选择器显示控制
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var timezoneExpanded by remember { mutableStateOf(false) }

    // 控制自定义权限引导对话框的显示
    var showPermissionGuideDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    
    // 处理权限请求的结果回调
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限获取成功，立即执行保存
            val finalDateTime = LocalDateTime.of(selectedDate, selectedTime)
            onSave(alarm.copy(
                time = finalDateTime,
                message = message,
                timeZone = selectedTimeZone!!
            ))
        } else {
            // 如果拒绝了权限，检查是否是“永久拒绝”（系统不再询问）
            val activity = context.findActivity()
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 如果 shouldShowRequestPermissionRationale 返回 false, 说明用户点击了“不再询问”或多次拒绝
                val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.POST_NOTIFICATIONS)
                if (!showRationale) {
                    showPermissionGuideDialog = true
                }
            }
        }
    }

    // 自定义权限引导对话框
    if (showPermissionGuideDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionGuideDialog = false },
            title = { Text("需要通知权限") },
            text = { Text("由于通知权限被多次拒绝，闹钟将无法触发，请前往设置手动开启通知权限") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionGuideDialog = false
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }) { Text("去设置") }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionGuideDialog = false}) { Text("取消") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 支持内部滚动，配合 imePadding 防止键盘遮挡
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 时间展示与修改
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

            // 日期展示与修改
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

            // 时区选择下拉列表
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

            // 闹钟备注/标签
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = message,
                onValueChange = { message = it },
                label = { Text("闹钟标签") },
                placeholder = { Text("例如：起床、开会") }
            )

            // 操作按钮：保存
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        // 保存前检查权限
                        if (notificationManager.areNotificationsEnabled()) {
                            // 已有权限，直接保存
                            val finalDateTime = LocalDateTime.of(selectedDate, selectedTime)
                            onSave(alarm.copy(
                                time = finalDateTime,
                                message = message,
                                timeZone = selectedTimeZone!!
                            ))
                        } else {
                            // 没有权限，尝试请求
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val activity = context.findActivity()
                                val showRationale = activity?.let {
                                    ActivityCompat.shouldShowRequestPermissionRationale(it,
                                        Manifest.permission.POST_NOTIFICATIONS)
                                } ?: false

                                // 如果系统已经不再弹出权限对话框，直接显示自定义引导
                                if (!showRationale && activity != null) {
                                    // 第一次请求权限时 rationale 也是 false，所以这里先launch一次
                                    // 系统会决定时弹窗还是直接回调失败
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                // API 33 以下直接引导去设置
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }


                            // 针对 Android 13+ 的权限申请流程
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
//                            } else {
//                                // 引导用户手动开启
//                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
//                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
//                                }
//                                context.startActivity(intent)
//                            }
                        }
                    },
                    enabled = selectedTimeZone != null // 必须选择时区后才可确认
                ) {
                    Text("完成")
                }
            }
            Spacer(modifier = Modifier.size(32.dp))
        }
    }

    // 时间选择对话框
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

    // 日期选择对话框
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

/**
 * 工具函数：从 Context 中获取Activity.
 */
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}