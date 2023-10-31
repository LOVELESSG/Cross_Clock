package com.example.crossclock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.crossclock.data.DRAWER_ITEMS
import com.example.crossclock.data.alarm.Alarm
import com.example.crossclock.data.worldclock.WorldClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(navController: NavController){
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectedItem = remember {
        mutableStateOf(DRAWER_ITEMS[1])
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
                // Plan to separate this TopAppBar as an independent component
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
                FloatingActionButton(onClick = { /* Open the Alarm Setting Page */  }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new alarm")
                }
            }
        ) { padding ->
            AlarmContent(alarmList = sampleAlarm, padding = padding)
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

@Composable
fun AlarmContent(
    alarmList: List<Alarm>,
    padding: PaddingValues
) {
    var checked by remember {
        mutableStateOf(true)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn{
            itemsIndexed(alarmList) { _, item ->
                ListItem(
                    headlineContent = { Text(text = item.time.toString(), fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(
                        text = item.date) },
                    trailingContent = { Switch(checked = checked, onCheckedChange = {checked = it})}
                )
                HorizontalDivider(modifier = Modifier.padding(16.dp, 4.dp, 16.dp, 8.dp))
            }
        }
    }
}

val sampleAlarm =  arrayListOf(
    Alarm(LocalTime.now(), "First Alarm", "Tokyo", "2023/10/31"),
    Alarm(LocalTime.now(), "First Alarm", "Seoul", "2023/10/23"),
    Alarm(LocalTime.now(), "First Alarm", "Shanghai", "2023/10/04")
    )

@Preview
@Composable
fun AlarmPagePreview(){
    AlarmScreen(navController = rememberNavController())
}