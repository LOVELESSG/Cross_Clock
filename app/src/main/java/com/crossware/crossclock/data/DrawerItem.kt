package com.crossware.crossclock.data

import com.crossware.crossclock.R

data class DrawerItem(
    val icon: Int,
    val name: String,
    val route: String
)

val DRAWER_ITEMS = arrayListOf(
    DrawerItem(R.drawable.world_clock, "home", "home_screen"),
    DrawerItem(R.drawable.baseline_access_alarms_24, "alarm", "alarm_screen"),
    DrawerItem(R.drawable.outline_timer_24, "stopWatch", "stopWatch_screen")
)
