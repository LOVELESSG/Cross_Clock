package com.example.crossclock.ui

import android.icu.text.SimpleDateFormat
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.crossclock.data.AlarmState
import com.example.crossclock.data.AlarmViewModel
import com.example.crossclock.data.DRAWER_ITEMS
import com.example.crossclock.data.alarm.Alarm
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import kotlin.time.Duration.Companion.days


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(navController: NavController){
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
                Text(text = "World Clock side")
                Spacer(modifier = Modifier.height(12.dp))
                DRAWER_ITEMS.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(imageVector = item.icon, contentDescription = null) },
                        label = { Text(text = (item.name)) },
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
                padding = padding
            )
            if (openAddAlarm) {
                AddAlarm(
                    changeStatus = { openAddAlarm = !openAddAlarm }
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
    //alarmList: List<Alarm>,
    padding: PaddingValues
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn{
            itemsIndexed(state.items) { index, item ->
                var checked by remember {
                    mutableStateOf(true)
                }
                val dismissState = rememberDismissState(
                    confirmValueChange = {
                        if (it == DismissedToStart) {
                            //alarmList.removeAt(index)
                            deleteAlarm(item)
                        }
                        it != DismissedToStart
                    }
                )
                SwipeToDismiss(
                    state = dismissState,
                    modifier = Modifier.padding(vertical = 4.dp),
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
                            headlineContent = { Text(text = item.time.toString(), fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(
                                text = item.date) },
                            trailingContent = { Switch(checked = checked, onCheckedChange = {checked = it})}
                        )
                        HorizontalDivider(modifier = Modifier.padding(16.dp, 4.dp, 16.dp, 8.dp))
                    }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarm(
    changeStatus: () -> Unit
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
                val timePickerState = rememberTimePickerState()
                val formatter = SimpleDateFormat("dd MMMM yyyy")
                var expanded by remember {
                    mutableStateOf(false)
                }
                var selectedOptionText by remember {
                    mutableStateOf("")
                }
                var text by rememberSaveable {
                    mutableStateOf("")
                }

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
                        IconButton(onClick = { /*TODO*/ }) {
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
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { 
                        TextField(
                            value = text,
                            onValueChange = {text = it},
                            label = {Text("Title")},
                            placeholder = { Text(text = "Title of alarm") }
                        )
                    }
                }
                //need to save the alarm
                /*Text(text = timePickerState.hour.toString())
                Text(text = formatter.format(datePickerState.selectedDateMillis?.let { Date(it) }))*/
            }
        }
    }
}

/*val sampleAlarm =  mutableStateListOf(
    Alarm(LocalTime.now(), "First Alarm", "Tokyo", "2023/10/31"),
    Alarm(LocalTime.now(), "First Alarm", "Seoul", "2023/10/23"),
    Alarm(LocalTime.now(), "First Alarm", "Shanghai", "2023/10/04"),
    Alarm(LocalTime.now(), "First Alarm", "sss", "2023/9/31"),
    Alarm(LocalTime.now(), "First Alarm", "dsasac", "2023/9/23"),
    Alarm(LocalTime.now(), "First Alarm", "sadefbgh", "2023/9/04")
    )*/

@Preview
@Composable
fun AlarmPagePreview(){
    AlarmScreen(navController = rememberNavController())
}

/*
@Preview(showSystemUi = true)
@Composable
fun AddAlarmPagePreview(){
    AddAlarm()
}*/
