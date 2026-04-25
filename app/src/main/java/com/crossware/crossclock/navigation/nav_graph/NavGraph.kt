package com.crossware.crossclock.navigation.nav_graph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.crossware.crossclock.ui.CrossClockApp
import com.crossware.crossclock.ui.alarm.CrossAlarmScheduler
import com.crossware.crossclock.service.StopWatchService

/**
 * 设置应用的导航图。
 * 该函数是应用导航的入口点，负责初始化主界面 [CrossClockApp]。
 *
 * @param navController 导航控制器，用于在不同的 Compose 界面之间导航。
 * @param alarmScheduler 闹钟调度器，用于管理闹钟的设置和取消。
 */
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    alarmScheduler: CrossAlarmScheduler
) {
    // 启动主应用界面，并将导航控制器和闹钟调度器传递下去。
    CrossClockApp(
        navController = navController,
        alarmScheduler = alarmScheduler
    )
}
