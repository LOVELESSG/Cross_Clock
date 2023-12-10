package com.example.crossclock.data

import com.example.crossclock.R

data class DrawerItem(
    val icon: Int,
    val name: String,
    val route: String
)

val DRAWER_ITEMS = arrayListOf(
    DrawerItem(R.drawable.world_clock, "home", "home_screen"),
    DrawerItem(R.drawable.baseline_access_alarms_24, "alarm", "alarm_screen"),
)
