package com.example.crossclock.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.vector.ImageVector
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
