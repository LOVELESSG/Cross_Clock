package com.crossware.crossclock.navigation.nav_graph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.crossware.crossclock.ui.CrossClockApp
import com.crossware.crossclock.ui.alarm.CrossAlarmScheduler
import com.crossware.crossclock.service.StopWatchService

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    alarmScheduler: CrossAlarmScheduler,
    stopWatchService: StopWatchService
) {
    CrossClockApp(
        navController = navController,
        alarmScheduler = alarmScheduler,
        stopWatchService = stopWatchService
    )
}
