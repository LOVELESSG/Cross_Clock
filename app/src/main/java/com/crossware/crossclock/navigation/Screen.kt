package com.crossware.crossclock.navigation

sealed class Screen(val route: String) {
    object Home: Screen(route = "home_screen")
    object Alarm: Screen(route = "alarm_screen")
    object StopWatch: Screen(route = "stopWatch_screen")
}
