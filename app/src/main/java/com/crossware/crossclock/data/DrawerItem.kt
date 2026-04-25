package com.crossware.crossclock.data

import com.crossware.crossclock.R

/**
 * 底部导航栏或抽屉菜单的配置模型。
 * 
 * @param icon 矢量图标资源 ID。
 * @param name 字符串资源 ID，用于显示标签名。
 * @param route 导航路由字符串。
 */
data class DrawerItem(
    val icon: Int,
    val name: Int,
    val route: String
)

/**
 * 应用主界面的导航项定义。
 */
val DRAWER_ITEMS = arrayListOf(
    // 世界时钟页面项
    DrawerItem(R.drawable.world_clock, R.string.drawer_item1, "home_screen"),
    // 闹钟管理页面项
    DrawerItem(R.drawable.baseline_access_alarms_24, R.string.drawer_item2, "alarm_screen"),
    // 秒表页面项
    DrawerItem(R.drawable.outline_timer_24, R.string.drawer_item3, "stopWatch_screen")
)
