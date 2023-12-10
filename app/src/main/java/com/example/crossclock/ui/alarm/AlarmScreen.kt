package com.example.crossclock.ui.alarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DismissDirection.*
import androidx.compose.material3.DismissValue.*
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDismissState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.crossclock.data.AlarmState
import com.example.crossclock.data.AlarmViewModel
import com.example.crossclock.data.DRAWER_ITEMS
import com.example.crossclock.data.alarm.Alarm
import com.example.crossclock.ui.ALL_CITIES
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(navController: NavController, scheduler: CrossAlarmScheduler){
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectedItem = remember {
        mutableStateOf(DRAWER_ITEMS[1])
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var openAddAlarm by remember {
        mutableStateOf(false)
    }

    val alarmViewModel = viewModel(modelClass = AlarmViewModel::class.java)
    val alarmState = alarmViewModel.state
    val alarmList = alarmState.items

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .requiredWidth(200.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "CrossClock",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                    )
                HorizontalDivider(modifier = Modifier.padding(0.dp, 4.dp, 0.dp, 4.dp))
                DRAWER_ITEMS.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(painter = painterResource(id = item.icon), contentDescription = null) },
                        label = {
                            Text(
                                text = (item.name),
                                modifier = Modifier.padding(16.dp))
                                },
                        selected = item == selectedItem.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItem.value = item
                            navController.navigate(route = item.route){
                                popUpTo(route = item.route){
                                    inclusive = true
                                }
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(text = "Alarm")
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Function List"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { openAddAlarm = !openAddAlarm }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new alarm")
                }
            }
        ) { padding ->
            AlarmContent(
                alarmViewModel.state,
                deleteAlarm = alarmViewModel::deleteAlarm,
                padding = padding,
                scheduler = scheduler,
                changeAlarmStatus = alarmViewModel::updateAlarmStatus
            )
            if (openAddAlarm) {
                AddAlarm(
                    changeStatus = { openAddAlarm = !openAddAlarm },
                    addAlarm = alarmViewModel::addAlarm
                )
            }
        }
    }
    BackHandler(
        enabled = drawerState.isOpen
    ) {
        scope.launch {
            drawerState.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        LazyColumn{
            items(state.items, key = {it.id}) { item ->
                val dismissState = rememberDismissState(
                    confirmValueChange = {
                        if (it == DismissedToStart) {
                            item.let(scheduler::cancel)
                            deleteAlarm(item)
                        }
                        it != DismissedToStart
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
                } else{
                    item.let(scheduler::cancel)
                    enableSwitch = false
                }
                SwipeToDismiss(
                    state = dismissState,
                    modifier = Modifier.padding(vertical = 0.dp),
                    directions = setOf(StartToEnd, EndToStart),
                    background = {
                        val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                        val color by animateColorAsState(
                            when (dismissState.targetValue) {
                                Default -> Color.LightGray
                                DismissedToEnd -> Color.Green
                                DismissedToStart -> Color.Red
                            }, label = ""
                        )
                        val alignment = when (direction) {
                            StartToEnd -> Alignment.CenterStart
                            EndToStart -> Alignment.CenterEnd
                        }
                        val icon = when (direction) {
                            StartToEnd -> Icons.Default.Done
                            EndToStart -> Icons.Default.Delete
                        }
                        val scale by animateFloatAsState(
                            if (dismissState.targetValue == Default) 0.75f else 1f, label = ""
                        )

                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 20.dp),
                            contentAlignment = alignment
                        ) {
                            Icon(
                                icon,
                                contentDescription = "Localized description",
                                modifier = Modifier.scale(scale)
                            )
                        }
                    },
                    dismissContent = {
                        ListItem(
                            headlineContent = { Text(text = item.time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)), fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(
                                text = item.message) },
                            trailingContent = { Switch(
                                checked = item.onOrOff,
                                onCheckedChange = {
                                    changeAlarmStatus(item)
                                },
                                enabled = enableSwitch
                            )
                            }
                        )
                    }
                )
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
){
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
                    title = { Text(text = "New Alarm") },
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
                                label = { Text(text = "Location of alarm") },
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                ALL_CITIES.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(text = selectionOption.city)},
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
                            onValueChange = {message = it},
                            label = {Text("Title")},
                            placeholder = { Text(text = "Title of alarm") }
                        )
                    }
                }
            }
        }
    }
}