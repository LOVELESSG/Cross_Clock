package com.crossware.crossclock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crossware.crossclock.R
import com.crossware.crossclock.data.ALL_CITIES
import com.crossware.crossclock.data.DRAWER_ITEMS
import com.crossware.crossclock.data.WorldClockViewModel
import com.crossware.crossclock.data.worldclock.WorldClock
import com.crossware.crossclock.navigation.Screen
import com.crossware.crossclock.service.StopWatchService
import com.crossware.crossclock.ui.alarm.AlarmScreen
import com.crossware.crossclock.ui.alarm.CrossAlarmScheduler
import com.crossware.crossclock.ui.stopwatch.StopWatchScreen
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant

/**
 * 应用的主 UI 组合项。
 * 负责构建应用的整体布局，包括顶部栏 (TopAppBar)、底部导航栏 (NavigationBar) 和悬浮按钮 (FloatingActionButton)。
 * 同时集成了 NavHost 来处理页面切换。
 *
 * @param navController 导航控制器，用于页面跳转。
 * @param alarmScheduler 闹钟调度器，用于在闹钟页面管理闹钟。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossClockApp(
    navController: NavHostController = rememberNavController(),
    alarmScheduler: CrossAlarmScheduler
) {
    // 控制世界时钟选择底部列表的显示状态
    var openWorldClockBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }
    // 控制添加闹钟对话框的显示状态
    var openAlarmAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // 顶部栏滚动行为，实现在滚动列表时顶部栏收起的效果
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // 获取世界时钟的 ViewModel，用于管理时钟数据
    val worldClockViewModel: WorldClockViewModel = hiltViewModel()
    val worldClockState = worldClockViewModel.state
    val homepageList = worldClockState.items

    // 获取当前导航栈的信息，用于确定当前处于哪个页面
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val context = LocalContext.current
    val alarmScheduler = alarmScheduler

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 大型顶部标题栏
            LargeTopAppBar(
                title = {
                    // 根据当前路由确定显示的标题
                    val titleResId = DRAWER_ITEMS.find { it.route == currentRoute }?.name ?: R.string.app_name
                    Text(
                        text = stringResource(titleResId),
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            // 底部导航栏
            NavigationBar {
                DRAWER_ITEMS.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(painter = painterResource(id = item.icon), contentDescription = stringResource(item.name)) },
                        label = { Text(stringResource(item.name)) },
                        selected = item.route == currentRoute,
                        onClick = {
                            // 导航到点击的项，并配置返回栈以避免重复累积
                            navController.navigate(route = item.route) {
                                navController.graph.startDestinationRoute?.let { route ->
                                    popUpTo(route) {
                                        saveState = true
                                    }
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            // 根据当前页面显示不同的悬浮按钮功能
            when (currentRoute) {
                Screen.Home.route -> {
                    // 首页（世界时钟）：点击打开城市选择列表
                    FloatingActionButton(onClick = { openWorldClockBottomSheet = !openWorldClockBottomSheet }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add World Clock")
                    }
                }
                Screen.Alarm.route -> {
                    // 闹钟页：点击打开添加闹钟对话框
                    FloatingActionButton(onClick = { openAlarmAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm")
                    }
                }
            }
        }
    ) { paddingValues ->
        // 导航容器，承载不同的屏幕内容
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues) // 应用 Scaffold 提供的内边距
        ) {
            // 世界时钟页面
            composable(Screen.Home.route) {
                val tick by worldClockViewModel.currentTimeTick.collectAsState()
                WorldClockContent(homepageList, tick, PaddingValues(0.dp))
            }
            // 闹钟管理页面
            composable(Screen.Alarm.route) {
                AlarmScreen(
                    paddingValues = PaddingValues(0.dp),
                    scheduler = alarmScheduler,
                    openAddAlarmDialog = openAlarmAddDialog,
                    onDismissAddAlarmDialog = { openAlarmAddDialog = false }
                )
            }
            // 秒表页面
            composable(Screen.StopWatch.route) {
                StopWatchScreen(
                    paddingValues = PaddingValues(0.dp)
                )
            }
        }

        // 如果状态为真，显示世界时钟选择页面（底部抽屉效果）
        if (openWorldClockBottomSheet) {
            WorldClockSelectPage(
                worldClockViewModel.state,
                changeStatus = { openWorldClockBottomSheet = !openWorldClockBottomSheet },
                addWorldClock = worldClockViewModel::addWorldClock,
                removeWorldClockByName = worldClockViewModel::deleteWorldClockByName,
                isChecked = worldClockViewModel::existWorldClock
            )
        }
    }
}

/**
 * 世界时钟主内容的组合项。
 * 展示用户已添加的所有城市时钟列表。
 *
 * @param homepageList 用户添加的世界时钟列表。
 * @param tick 触发时间更新的计数器，用于实时刷新 UI。
 * @param padding 内边距。
 */
@Composable
fun WorldClockContent(
    homepageList: List<WorldClock>,
    tick: Long,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopStart
    ) {
        if (homepageList.isEmpty()) {
            // 如果列表为空，显示提示信息
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.no_world_clock_hint))
            }
        } else {
            // 滚动列表展示时钟
            LazyColumn {
                itemsIndexed(homepageList) { _, item ->
                    // 根据时区 ID 获取并格式化当前时间
                    val currentTime = remember(tick) {
                        LocalTime.now(item.cityTimeZoneId)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    }
                    ListItem(
                        headlineContent = { Text(text = item.city, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            // 显示对应城市的日期
                            Text(
                                text = LocalDate.now(item.cityTimeZoneId)
                                    .format(DateTimeFormatter.ISO_DATE)
                            )
                        },
                        trailingContent = {
                            // 显示对应城市的时间
                            Text(
                                text = currentTime,
                                style = MaterialTheme.typography.displayMedium
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(16.dp, 4.dp, 16.dp, 8.dp))
                }
            }
        }
    }
}



