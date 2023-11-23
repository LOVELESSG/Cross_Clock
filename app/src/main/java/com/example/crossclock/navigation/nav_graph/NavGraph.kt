package com.example.crossclock.navigation.nav_graph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.crossclock.navigation.Screen
import com.example.crossclock.ui.alarm.AlarmScreen
import com.example.crossclock.ui.CrossClockApp
import com.example.crossclock.ui.alarm.CrossAlarmScheduler

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    alarmScheduler: CrossAlarmScheduler
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
    }
}