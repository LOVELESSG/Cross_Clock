package com.crossware.crossclock.ui.alarm

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossware.crossclock.data.ALL_CITIES
import com.crossware.crossclock.data.alarm.Alarm
import com.crossware.crossclock.ui.components.CitySearchContent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 跨时区世界闹钟创建/编辑底部抽屉界面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 草稿状态
    var selectedTime by remember { mutableStateOf(alarm.time.toLocalTime()) }
    var message by remember { mutableStateOf(alarm.message) }
    var selectedTimeZone by remember { mutableStateOf(alarm.timeZone) }
    var selectedRingtoneUri by remember { mutableStateOf(alarm.ringtoneUri) }
    var selectedRingtoneTitle by remember { mutableStateOf(alarm.ringtoneTitle ?: "系统默认闹铃") }
    var repeatDaysStr by remember { mutableStateOf(alarm.repeatDays) }
    var targetDateStr by remember { mutableStateOf(alarm.targetDate) }

    // 内部弹窗/Sheet 控制
    var showTimePicker by remember { mutableStateOf(false) }
    var showCitySearchSheet by remember { mutableStateOf(false) }
    var showTitleDialog by remember { mutableStateOf(false) }
    var showRepeatConfigDialog by remember { mutableStateOf(false) }

    // 铃声选择器 Launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                selectedRingtoneUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                selectedRingtoneTitle = ringtone?.getTitle(context) ?: "自定义铃声"
            } else {
                selectedRingtoneUri = null
                selectedRingtoneTitle = "静音"
            }
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 顶栏：取消 | 设定跨时区闹钟 | 完成
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    text = "设定跨时区闹钟",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        val date = if (!targetDateStr.isNullOrBlank()) {
                            try { LocalDate.parse(targetDateStr) } catch (e: Exception) { LocalDate.now() }
                        } else {
                            LocalDate.now()
                        }
                        val finalDateTime = LocalDateTime.of(date, selectedTime)
                        val updatedAlarm = alarm.copy(
                            time = finalDateTime,
                            message = message.ifBlank { "跨时区闹钟" },
                            timeZone = selectedTimeZone,
                            ringtoneUri = selectedRingtoneUri,
                            ringtoneTitle = selectedRingtoneTitle,
                            repeatDays = repeatDaysStr,
                            targetDate = targetDateStr,
                            onOrOff = true
                        )
                        onSave(updatedAlarm)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("完成", fontWeight = FontWeight.Bold)
                }
            }

            // 大型 12/24小时制时间轮播展示卡片
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val locale = LocalConfiguration.current.locales[0]
                    val isPm = selectedTime.hour >= 12

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = String.format(locale, "%02d : %02d", selectedTime.hour, selectedTime.minute),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (isPm) "PM" else "AM",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 4 个参数设置卡片列表
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // 1. 目标唤醒时区
                val cityInfo = ALL_CITIES.find { it.cityTimeZoneId == selectedTimeZone }
                val zoneName = cityInfo?.city ?: selectedTimeZone.id
                val nowZdt = ZonedDateTime.now(selectedTimeZone)
                val offsetStr = nowZdt.format(DateTimeFormatter.ofPattern("O"))

                OptionCard(
                    icon = Icons.Default.Public,
                    title = "目标唤醒时区",
                    subtitle = "$zoneName ($offsetStr)",
                    onClick = { showCitySearchSheet = true }
                )

                // 2. 闹钟标题与标签（未输入时显示占位提示语“请输入闹钟标题”）
                OptionCard(
                    icon = Icons.Default.Edit,
                    title = "闹钟标题与标签",
                    subtitle = message.ifBlank { "请输入闹钟标题" },
                    onClick = { showTitleDialog = true }
                )

                // 3. 重复周期
                val tempAlarm = alarm.copy(repeatDays = repeatDaysStr, targetDate = targetDateStr)
                OptionCard(
                    icon = Icons.Default.Repeat,
                    title = "重复周期",
                    subtitle = tempAlarm.getRepeatDisplayText(),
                    onClick = { showRepeatConfigDialog = true }
                )

                // 4. 铃声与触感
                OptionCard(
                    icon = Icons.Default.GraphicEq,
                    title = "铃声与触感",
                    subtitle = selectedRingtoneTitle,
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择闹钟铃声")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedRingtoneUri?.let { Uri.parse(it) })
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        }
                        ringtonePickerLauncher.launch(intent)
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 时间选择器弹窗
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("设置唤醒时间") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            }
        )
    }

    // 城市/时区选择 Sheet
    if (showCitySearchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCitySearchSheet = false }
        ) {
            CitySearchContent(
                isMultiSelect = false,
                onCitySelected = { searchResult ->
                    selectedTimeZone = searchResult.zoneId
                    showCitySearchSheet = false
                }
            )
        }
    }

    // 闹钟标题编辑弹窗
    if (showTitleDialog) {
        var tempTitle by remember { mutableStateOf(message) }
        AlertDialog(
            onDismissRequest = { showTitleDialog = false },
            title = { Text("闹钟标题与标签") },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text("标签内容") },
                    placeholder = { Text("请输入闹钟标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    message = tempTitle
                    showTitleDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) { Text("取消") }
            }
        )
    }

    // 重复周期配置对话框
    if (showRepeatConfigDialog) {
        RepeatConfigDialog(
            currentRepeatDays = repeatDaysStr,
            currentTargetDate = targetDateStr,
            onDismiss = { showRepeatConfigDialog = false },
            onConfirm = { days, date ->
                repeatDaysStr = days
                targetDateStr = date
                showRepeatConfigDialog = false
            }
        )
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatConfigDialog(
    currentRepeatDays: String?,
    currentTargetDate: String?,
    onDismiss: () -> Unit,
    onConfirm: (repeatDays: String?, targetDate: String?) -> Unit
) {
    var isRepeatMode by remember { mutableStateOf(!currentRepeatDays.isNullOrBlank()) }

    // 选中的重复天数 (1=Mon, ..., 7=Sun)
    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            if (!currentRepeatDays.isNullOrBlank()) {
                addAll(currentRepeatDays.split(",").mapNotNull { it.trim().toIntOrNull() })
            } else {
                addAll(listOf(1, 2, 3, 4, 5)) // 默认工作日
            }
        }
    }

    var selectedSingleDate by remember {
        mutableStateOf(
            if (!currentTargetDate.isNullOrBlank()) {
                try { LocalDate.parse(currentTargetDate) } catch (e: Exception) { LocalDate.now() }
            } else LocalDate.now()
        )
    }

    var showDatePickerDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置重复周期 (基于目标时区)") },
        text = {
            // 对话框内部内容可上下滑动，完美适配横屏与小屏
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isRepeatMode,
                        onClick = { isRepeatMode = false },
                        label = { Text("单次固定日期") }
                    )
                    FilterChip(
                        selected = isRepeatMode,
                        onClick = { isRepeatMode = true },
                        label = { Text("按周固定日重复") }
                    )
                }

                if (!isRepeatMode) {
                    // 单次固定日期模式
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("响铃日期", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${selectedSingleDate.year}年${selectedSingleDate.monthValue}月${selectedSingleDate.dayOfMonth}日",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // 按周重复模式：预设与星期多选
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedDays.sorted() == listOf(1, 2, 3, 4, 5),
                            onClick = {
                                selectedDays.clear()
                                selectedDays.addAll(listOf(1, 2, 3, 4, 5))
                            },
                            label = { Text("工作日") }
                        )
                        FilterChip(
                            selected = selectedDays.sorted() == listOf(6, 7),
                            onClick = {
                                selectedDays.clear()
                                selectedDays.addAll(listOf(6, 7))
                            },
                            label = { Text("周末") }
                        )
                        FilterChip(
                            selected = selectedDays.size == 7,
                            onClick = {
                                selectedDays.clear()
                                selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                            },
                            label = { Text("每天") }
                        )
                    }

                    // 周一到周日 Checkbox Chips（添加横向滚动，确保全屏展示与无缝滑动选择）
                    val daysMap = listOf(
                        1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四",
                        5 to "周五", 6 to "周六", 7 to "周日"
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        daysMap.forEach { (dayVal, dayLabel) ->
                            val isChecked = selectedDays.contains(dayVal)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) selectedDays.remove(dayVal)
                                    else selectedDays.add(dayVal)
                                },
                                label = { Text(dayLabel) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isRepeatMode) {
                        onConfirm(null, selectedSingleDate.toString())
                    } else {
                        val daysStr = selectedDays.sorted().joinToString(",")
                        onConfirm(daysStr.ifBlank { null }, null)
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedSingleDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedSingleDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePickerDialog = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
