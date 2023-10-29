package com.example.crossclock.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.vector.ImageVector

data class DrawerItem(
    val icon: ImageVector,
    val name: String,
    val route: String
)

val DRAWER_ITEMS = arrayListOf(
    DrawerItem(Icons.Default.Favorite, "home", "home_screen"),
    DrawerItem(Icons.Default.Face, "alarm", "alarm_screen"),
    //DrawerItem(Icons.Default.Email, "another")
)
