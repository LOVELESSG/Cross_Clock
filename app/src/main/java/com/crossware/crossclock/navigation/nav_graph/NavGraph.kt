package com.crossware.crossclock.navigation.nav_graph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.crossware.crossclock.navigation.Screen
import com.crossware.crossclock.service.StopWatchService
import com.crossware.crossclock.ui.alarm.AlarmScreen
import com.crossware.crossclock.ui.CrossClockApp
import com.crossware.crossclock.ui.alarm.CrossAlarmScheduler
import com.crossware.crossclock.ui.stopwatch.StopWatchScreen

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    alarmScheduler: CrossAlarmScheduler,
    stopWatchService: StopWatchService
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(
            route = Screen.Home.route
        ) {
            CrossClockApp(navController = navController)
        }
        composable(
            route = Screen.Alarm.route
        ) {
            AlarmScreen(navController = navController, alarmScheduler)
        }
        composable(
            route = Screen.StopWatch.route
        ) {
            StopWatchScreen(navController = navController, stopWatchService = stopWatchService)
        }
    }
}