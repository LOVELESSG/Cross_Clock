package com.crossware.crossclock.data

import androidx.compose.ui.res.stringResource
import com.crossware.crossclock.R

data class DrawerItem(
    val icon: Int,
    val name: Int,
    val route: String
)

val DRAWER_ITEMS = arrayListOf(
    DrawerItem(R.drawable.world_clock, R.string.drawer_item1, "home_screen"),
    DrawerItem(R.drawable.baseline_access_alarms_24, R.string.drawer_item2, "alarm_screen"),
    DrawerItem(R.drawable.outline_timer_24, R.string.drawer_item3, "stopWatch_screen")
)
