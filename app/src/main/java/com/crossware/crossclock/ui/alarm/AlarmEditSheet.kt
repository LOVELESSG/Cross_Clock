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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossware.crossclock.R
import com.crossware.crossclock.data.alarm.Alarm
import com.crossware.crossclock.ui.components.CitySearchContent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 跨时区世界闹钟创建/编辑底部抽屉界面（全语种本地化与响应式顶栏抗挤压自适应）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    alarm: Alarm,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val defaultRingtoneText = stringResource(R.string.default_ringtone)

    // 草稿状态
    var selectedTime by remember { mutableStateOf(alarm.time.toLocalTime()) }
    var message by remember { mutableStateOf(alarm.message) }
    var selectedTimeZone by remember { mutableStateOf(alarm.timeZone) }
    var selectedLocationName by remember { mutableStateOf(alarm.locationName) }
    var selectedRingtoneUri by remember { mutableStateOf(alarm.ringtoneUri) }
    var selectedRingtoneTitle by remember { mutableStateOf(alarm.ringtoneTitle ?: defaultRingtoneText) }
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
                selectedRingtoneTitle = ringtone?.getTitle(context) ?: "Ringtone"
            } else {
                selectedRingtoneUri = null
                selectedRingtoneTitle = defaultRingtoneText
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
            // 顶栏：取消 | 设定跨时区闹钟 | 完成 (利用 weight(1f) 与 maxLines 彻底解决长英文标题挤压变形问题)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(stringResource(R.string.cancel_select_time), style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    text = stringResource(R.string.set_cross_timezone_alarm),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                val defaultTitle = stringResource(R.string.cross_timezone_alarm)
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
                            message = message.ifBlank { defaultTitle },
                            timeZone = selectedTimeZone,
                            locationName = selectedLocationName,
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
                    Text(stringResource(R.string.done), fontWeight = FontWeight.Bold, maxLines = 1)
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
                val tempAlarm = alarm.copy(timeZone = selectedTimeZone, locationName = selectedLocationName)
                val zoneName = tempAlarm.getDisplayZoneCityName(locale)
                val nowZdt = ZonedDateTime.now(selectedTimeZone)
                val offsetStr = nowZdt.format(DateTimeFormatter.ofPattern("O"))

                OptionCard(
                    icon = Icons.Default.Public,
                    title = stringResource(R.string.target_wakeup_timezone),
                    subtitle = "$zoneName ($offsetStr)",
                    onClick = { showCitySearchSheet = true }
                )

                // 2. 闹钟标题与标签
                OptionCard(
                    icon = Icons.Default.Edit,
                    title = stringResource(R.string.alarm_title_and_label),
                    subtitle = message.ifBlank { stringResource(R.string.enter_alarm_title_placeholder) },
                    onClick = { showTitleDialog = true }
                )

                // 3. 重复周期
                val repeatAlarm = alarm.copy(repeatDays = repeatDaysStr, targetDate = targetDateStr)
                OptionCard(
                    icon = Icons.Default.Repeat,
                    title = stringResource(R.string.repeat_schedule),
                    subtitle = repeatAlarm.getRepeatDisplayText(context),
                    onClick = { showRepeatConfigDialog = true }
                )

                // 4. 铃声与触感
                OptionCard(
                    icon = Icons.Default.GraphicEq,
                    title = stringResource(R.string.ringtone_and_vibration),
                    subtitle = selectedRingtoneTitle,
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
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
            title = { Text(stringResource(R.string.set_wakeup_time)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.confirm_select_time)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel_select_time)) }
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
                    selectedLocationName = searchResult.cityName
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
            title = { Text(stringResource(R.string.alarm_title_and_label)) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    label = { Text(stringResource(R.string.label_content)) },
                    placeholder = { Text(stringResource(R.string.enter_alarm_title_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    message = tempTitle
                    showTitleDialog = false
                }) { Text(stringResource(R.string.save_timer)) }
            },
            dismissButton = {
                TextButton(onClick = { showTitleDialog = false }) { Text(stringResource(R.string.cancel_select_time)) }
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
    val locale = LocalConfiguration.current.locales[0]
    var isRepeatMode by remember { mutableStateOf(!currentRepeatDays.isNullOrBlank()) }

    val selectedDays = remember {
        mutableStateListOf<Int>().apply {
            if (!currentRepeatDays.isNullOrBlank()) {
                addAll(currentRepeatDays.split(",").mapNotNull { it.trim().toIntOrNull() })
            } else {
                addAll(listOf(1, 2, 3, 4, 5))
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
        title = { Text(stringResource(R.string.set_repeat_schedule_dialog)) },
        text = {
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
                        label = { Text(stringResource(R.string.single_fixed_date)) }
                    )
                    FilterChip(
                        selected = isRepeatMode,
                        onClick = { isRepeatMode = true },
                        label = { Text(stringResource(R.string.repeat_weekly)) }
                    )
                }

                if (!isRepeatMode) {
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
                            Text(stringResource(R.string.ring_date), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = selectedSingleDate.format(DateTimeFormatter.ofPattern("yyyy/MM/dd", locale)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedDays.sorted() == listOf(1, 2, 3, 4, 5),
                            onClick = {
                                selectedDays.clear()
                                selectedDays.addAll(listOf(1, 2, 3, 4, 5))
                            },
                            label = { Text(stringResource(R.string.workdays)) }
                        )
                        FilterChip(
                            selected = selectedDays.sorted() == listOf(6, 7),
                            onClick = {
                                selectedDays.clear()
                                selectedDays.addAll(listOf(6, 7))
                            },
                            label = { Text(stringResource(R.string.weekends)) }
                        )
                        FilterChip(
                            selected = selectedDays.size == 7,
                            onClick = {
                                selectedDays.clear()
                                selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                            },
                            label = { Text(stringResource(R.string.everyday)) }
                        )
                    }

                    val daysMap = if (locale.language == "zh") {
                        listOf(1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日")
                    } else if (locale.language == "ja") {
                        listOf(1 to "月", 2 to "火", 3 to "水", 4 to "木", 5 to "金", 6 to "土", 7 to "日")
                    } else {
                        listOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
                    }

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
            ) { Text(stringResource(R.string.confirm_select_time)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_select_time)) }
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
                }) { Text(stringResource(R.string.confirm_select_time)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text(stringResource(R.string.cancel_select_time)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
