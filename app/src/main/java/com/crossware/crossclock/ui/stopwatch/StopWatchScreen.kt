package com.crossware.crossclock.ui.stopwatch

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.crossware.crossclock.data.DRAWER_ITEMS
import com.crossware.crossclock.service.ServiceHelper
import com.crossware.crossclock.service.StopWatchService
import com.crossware.crossclock.service.StopWatchState
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_CANCEL
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_START
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_STOP
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopWatchScreen(navController: NavController, stopWatchService: StopWatchService){
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectedItem = remember {
        mutableStateOf(DRAWER_ITEMS[2])
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
                                modifier = Modifier.padding(8.dp))
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
                        Text(text = "StopWatch")
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
        ) { padding ->
            StopWatchContent(stopWatchService, padding)
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
fun StopWatchContent(stopWatchService: StopWatchService, padding: PaddingValues){

    val context = LocalContext.current
    val hours by stopWatchService.hours
    val minutes by stopWatchService.minutes
    val seconds by stopWatchService.seconds
    val currentState by stopWatchService.currentState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            //.padding(30.dp),
            .padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.weight(weight = 2f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(targetState = hours, transitionSpec = { addAnimation() }, label = "") { targetCount ->
                Text(
                    text = targetCount,
                    style = TextStyle(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (hours == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                    )
                )
            }
            AnimatedContent(targetState = minutes, transitionSpec = { addAnimation() },
                label = ""
            ) { targetCount ->
                Text(
                    text = targetCount,
                    style = TextStyle(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (minutes == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                    )
                )
            }
            AnimatedContent(targetState = seconds, transitionSpec = { addAnimation() },
                label = ""
            ) { targetCount ->
                Text(
                    text = targetCount,
                    style = TextStyle(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (seconds == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        Row(
            modifier = Modifier.weight(weight = 1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                onClick = {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = if (currentState == StopWatchState.Started) ACTION_SERVICE_STOP
                        else ACTION_SERVICE_START
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    contentColor = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(
                    text = when (currentState) {
                        StopWatchState.Started -> "Stop"
                        StopWatchState.Stopped -> "Resume"
                        else -> "Start"
                    }
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Button(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                onClick = {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_CANCEL
                    )
                },
                enabled = seconds != "00" && currentState != StopWatchState.Started,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(text = "Cancel")
            }
        }
    }
}

fun addAnimation(duration: Int = 100): ContentTransform {
    return slideInVertically(animationSpec = tween(durationMillis = duration)) { height -> height} + fadeIn(
        animationSpec = tween(durationMillis = duration)
    ) togetherWith slideOutVertically(animationSpec = tween(durationMillis = duration)) { height -> height } + fadeOut(
        animationSpec = tween(durationMillis = duration)
    )
}