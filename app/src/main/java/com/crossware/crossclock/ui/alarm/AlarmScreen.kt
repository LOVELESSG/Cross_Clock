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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crossware.crossclock.R
import com.crossware.crossclock.data.AlarmState
import com.crossware.crossclock.data.AlarmViewModel
import com.crossware.crossclock.data.alarm.Alarm
import com.crossware.crossclock.ui.ALL_CITIES
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    paddingValues: PaddingValues,
    scheduler: CrossAlarmScheduler,
    openAddAlarmDialog: Boolean, // Added to control dialog visibility from parent
    onDismissAddAlarmDialog: () -> Unit // Added to control dialog visibility from parent
) {
    val alarmViewModel = viewModel(modelClass = AlarmViewModel::class.java)

    AlarmContent(
        alarmViewModel.state,
        deleteAlarm = alarmViewModel::deleteAlarm,
        padding = paddingValues,
        scheduler = scheduler,
        changeAlarmStatus = alarmViewModel::updateAlarmStatus
    )
    if (openAddAlarmDialog) {
        AddAlarm(
            changeStatus = onDismissAddAlarmDialog,
            addAlarm = alarmViewModel::addAlarm
        )
    }
}

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
            .padding(padding), // Apply padding from NavHost
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn {
            items(state.items, key = { it.id }) { item ->
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
                var enableSwitch by remember {
                    mutableStateOf(true)
                }
                if (item.onOrOff && compareTime) {
                    item.let(scheduler::scheduler)
                } else if (!item.onOrOff && compareTime) {
                    item.let(scheduler::cancel)
                } else {
                    item.let(scheduler::cancel)
                    enableSwitch = false
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
                            }, label = ""
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
                                contentDescription = "Localized description",
                            )
                        }
                    },
                    enableDismissFromStartToEnd = false
                ) {
                    ListItem(
                        headlineContent = { Text(text = item.time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)), fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                text = item.message
                            )
                        },
                        trailingContent = {
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarm(
    changeStatus: () -> Unit,
    addAlarm: (Alarm) -> Unit
) {
    Dialog(
        onDismissRequest = { changeStatus() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
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
                val datePickerState = rememberDatePickerState(
                    initialDisplayMode = DisplayMode.Input,
                    initialSelectedDateMillis = Instant.now().toEpochMilli()
                )
                val timePickerState = rememberTimePickerState(is24Hour = false)
                var expanded by remember {
                    mutableStateOf(false)
                }
                var selectedOptionText by remember {
                    mutableStateOf("")
                }
                var selectedTimeZone: ZoneId? by remember {
                    mutableStateOf(null)
                }
                var message by rememberSaveable {
                    mutableStateOf("")
                }
                var localDateTime by remember {
                    mutableStateOf(LocalDateTime.now())
                }
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
                                contentDescription = "Back to the alarm list"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
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
                                contentDescription = "Save this alarm"
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
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = {
                                expanded = !expanded
                            }
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
