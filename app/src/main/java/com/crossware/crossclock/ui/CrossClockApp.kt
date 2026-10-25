package com.crossware.crossclock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.crossware.crossclock.R
import com.crossware.crossclock.data.DRAWER_ITEMS
import com.crossware.crossclock.data.TimerViewModel
import com.crossware.crossclock.data.WorldClockViewModel
import com.crossware.crossclock.data.timer.TimerEntity
import com.crossware.crossclock.data.worldclock.WorldClock
import com.crossware.crossclock.navigation.Screen
import com.crossware.crossclock.ui.alarm.AlarmScreen
import com.crossware.crossclock.ui.alarm.CrossAlarmScheduler
import com.crossware.crossclock.ui.stopwatch.StopWatchScreen
import com.crossware.crossclock.ui.timer.TimerEditBottomSheet
import com.crossware.crossclock.ui.timer.TimerRunningScreen
import com.crossware.crossclock.ui.timer.TimerScreen
import com.crossware.crossclock.ui.timer.TimerWidgetPill
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 应用的主 UI 组合项。
 * 负责构建应用的整体布局，包括顶部栏 (TopAppBar)、底部导航栏 (NavigationBar) 和悬浮按钮 (FloatingActionButton)。
 * 同时集成了 NavHost 来处理页面切换。
 *
 * @param navController 导航控制器，用于页面跳转。
 * @param alarmScheduler 闹钟调度器，用于在闹钟页面管理闹钟。
 * @param deepLinkRoute 外部 DeepLink 或通知打入的目标路由名称。
 * @param deepLinkTimerId 通知关联的目标计时器 ID。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossClockApp(
    navController: NavHostController = rememberNavController(),
    alarmScheduler: CrossAlarmScheduler,
    deepLinkRoute: String? = null,
    deepLinkTimerId: Long? = null
) {
    // 控制世界时钟选择底部列表的显示状态
    var openWorldClockBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }
    // 控制添加闹钟对话框的显示状态
    var openAlarmAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    // 控制添加/编辑计时器 BottomSheet 的显示状态
    var openTimerAddSheet by rememberSaveable {
        mutableStateOf(false)
    }
    var editingTimer by remember {
        mutableStateOf<TimerEntity?>(null)
    }

    // 控制多计时器候选展开列表状态
    var isTimerListExpanded by remember {
        mutableStateOf(false)
    }

    // 顶部栏滚动行为，实现在滚动列表时顶部栏收起的效果
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // ViewModel 获取
    val worldClockViewModel: WorldClockViewModel = hiltViewModel()
    val worldClockState = worldClockViewModel.state
    val homepageList = worldClockState.items

    val timerViewModel: TimerViewModel = hiltViewModel()

    // 监听外部通知或 DeepLink 打入，精准选定目标计时器并跳转至全屏界面
    LaunchedEffect(deepLinkRoute, deepLinkTimerId) {
        if (deepLinkRoute == Screen.TimerRunning.route) {
            if (deepLinkTimerId != null) {
                timerViewModel.selectActiveTimer(deepLinkTimerId)
            }
            if (timerViewModel.selectedActiveTimer != null) {
                navController.navigate(Screen.TimerRunning.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    // 获取当前导航栈的信息，用于确定当前处于哪个页面
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // 如果处于全屏倒计时界面，隐藏主界面 Scaffold（返回时不取消/停止计时器）
    if (currentRoute == Screen.TimerRunning.route) {
        TimerRunningScreen(
            timerViewModel = timerViewModel,
            onExit = {
                navController.popBackStack()
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                // 大型顶部标题栏
                LargeTopAppBar(
                    title = {
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
                        FloatingActionButton(onClick = { openWorldClockBottomSheet = !openWorldClockBottomSheet }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add World Clock")
                        }
                    }
                    Screen.Alarm.route -> {
                        FloatingActionButton(onClick = { openAlarmAddDialog = true }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm")
                        }
                    }
                    Screen.Timer.route -> {
                        // 仅当没有活跃运行的计时器时，显示 Scaffold 的默认 FAB
                        if (timerViewModel.activeTimers.isEmpty()) {
                            FloatingActionButton(onClick = {
                                editingTimer = null
                                openTimerAddSheet = true
                            }) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Timer")
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            // 导航容器，承载不同的屏幕内容
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
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
                // 计时器页面
                composable(Screen.Timer.route) {
                    TimerScreen(
                        paddingValues = PaddingValues(0.dp),
                        timerViewModel = timerViewModel,
                        onStartTimer = { timer ->
                            timerViewModel.startActiveTimer(timer)
                            navController.navigate(Screen.TimerRunning.route)
                        },
                        onEditTimer = { timer ->
                            editingTimer = timer
                            openTimerAddSheet = true
                        },
                        onOpenRunningScreen = { timerId ->
                            timerViewModel.selectActiveTimer(timerId)
                            navController.navigate(Screen.TimerRunning.route)
                        },
                        onAddTimerClick = {
                            editingTimer = null
                            openTimerAddSheet = true
                        },
                        isExpandedListOpen = isTimerListExpanded,
                        onToggleExpand = { isTimerListExpanded = !isTimerListExpanded }
                    )
                }
                // 计时器走时全屏画面
                composable(Screen.TimerRunning.route) {
                    TimerRunningScreen(
                        timerViewModel = timerViewModel,
                        onExit = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            // 显示世界时钟选择页面
            if (openWorldClockBottomSheet) {
                WorldClockSelectPage(
                    state = worldClockViewModel.state,
                    changeStatus = { openWorldClockBottomSheet = !openWorldClockBottomSheet },
                    addWorldClock = worldClockViewModel::addWorldClock,
                    deleteWorldClock = worldClockViewModel::deleteWorldClock,
                    removeWorldClockByName = worldClockViewModel::deleteWorldClockByName
                )
            }

            // 显示添加/编辑计时器表单 Sheet
            if (openTimerAddSheet) {
                TimerEditBottomSheet(
                    timer = editingTimer,
                    onDismiss = {
                        openTimerAddSheet = false
                        editingTimer = null
                    },
                    onSave = { timerEntity ->
                        if (timerEntity.id == 0L) {
                            timerViewModel.addTimer(timerEntity)
                        } else {
                            timerViewModel.updateTimer(timerEntity)
                        }
                        openTimerAddSheet = false
                        editingTimer = null
                    }
                )
            }
        }

        // 全局半透明蒙版 & 运行中的候选计时器列表（覆盖包含 TopAppBar 和 NavigationBar 在内的整个屏幕）
        val activeTimers = timerViewModel.activeTimers
        val focusedTimer = timerViewModel.selectedActiveTimer
        if (isTimerListExpanded && activeTimers.size > 1 && focusedTimer != null) {
            // 拦截展开候选计时器列表时的系统返回按键与手势：在蒙版显示时注册 BackHandler，使其注册顺序晚于 NavHost，从而具备最高拦截优先级，优先关闭展开列表而非退出当前页面
            BackHandler(enabled = true) {
                isTimerListExpanded = false
            }

            // 1. 全屏半透明蒙版：遮罩包括 TopAppBar、NavigationBar、FAB在内的整个屏幕，点击蒙版空白处直接关闭列表
            AnimatedVisibility(
                visible = isTimerListExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { isTimerListExpanded = false }
                )
            }

            // 2. 蒙版之上的最顶层内容：在 NavigationBar 正上方展开候选计时器列表 + 底部聚焦计时器
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 候选区运行中的其他计时器列表（不含任何标题栏或关闭按键）
                val candidateTimers = activeTimers.filter { it.timer.id != focusedTimer.timer.id }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(candidateTimers, key = { it.timer.id }) { candidate ->
                        TimerWidgetPill(
                            runningData = candidate,
                            activeCount = activeTimers.size,
                            showExpandButton = false,
                            onPlayPause = {
                                if (candidate.isRunning && !candidate.isPaused) {
                                    timerViewModel.pauseTimer(candidate.timer.id)
                                } else {
                                    timerViewModel.resumeTimer(candidate.timer.id)
                                }
                            },
                            onNextStep = {
                                timerViewModel.skipToNextStep(candidate.timer.id)
                            },
                            onStop = {
                                timerViewModel.stopActiveTimer(candidate.timer.id)
                            },
                            onExpandClick = { },
                            onCardClick = {
                                // 点击候选区非按钮区域：切换底部聚焦计时器并关闭展开列表（不进入全屏）
                                timerViewModel.selectActiveTimer(candidate.timer.id)
                                isTimerListExpanded = false
                            }
                        )
                    }
                }

                // 底部聚焦计时器卡片（在蒙版之上悬浮保持清晰展示）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerWidgetPill(
                        runningData = focusedTimer,
                        activeCount = activeTimers.size,
                        showExpandButton = true,
                        onPlayPause = {
                            if (focusedTimer.isRunning && !focusedTimer.isPaused) {
                                timerViewModel.pauseTimer(focusedTimer.timer.id)
                            } else {
                                timerViewModel.resumeTimer(focusedTimer.timer.id)
                            }
                        },
                        onNextStep = {
                            timerViewModel.skipToNextStep(focusedTimer.timer.id)
                        },
                        onStop = {
                            timerViewModel.stopActiveTimer(focusedTimer.timer.id)
                        },
                        onExpandClick = {
                            isTimerListExpanded = false
                        },
                        onCardClick = {
                            isTimerListExpanded = false
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // 右侧占位（保持底部 Row 与 FAB 宽度精准对齐）
                    Box(modifier = Modifier.size(56.dp))
                }
            }
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.no_world_clock_hint))
            }
        } else {
            val cityGroupMap = remember(homepageList) {
                homepageList.groupBy { it.getDisplayCityName() }
            }

            LazyColumn {
                itemsIndexed(homepageList, key = { index, item -> "world_clock_${item.id}_$index" }) { _, item ->
                    val cityName = item.getDisplayCityName()
                    val countryName = item.getDisplayCountry()
                    val adminArea = item.getDisplayAdminArea()

                    val sameCityList = cityGroupMap[cityName] ?: emptyList()
                    val isDuplicateCity = sameCityList.size > 1

                    val displayHeadline = if (!isDuplicateCity) {
                        cityName
                    } else {
                        val sameCountryCount = sameCityList.count {
                            val c = it.getDisplayCountry()
                            !c.isNullOrBlank() && c.equals(countryName, ignoreCase = true)
                        }
                        val isDuplicateCountry = sameCountryCount > 1

                        if (isDuplicateCountry && !adminArea.isNullOrBlank() && !countryName.isNullOrBlank()) {
                            "$cityName ($adminArea, $countryName)"
                        } else if (!countryName.isNullOrBlank()) {
                            "$cityName ($countryName)"
                        } else {
                            cityName
                        }
                    }

                    val currentTime = remember(tick) {
                        LocalTime.now(item.cityTimeZoneId)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    }
                    ListItem(
                        headlineContent = { Text(text = displayHeadline, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                text = LocalDate.now(item.cityTimeZoneId)
                                    .format(DateTimeFormatter.ISO_DATE)
                            )
                        },
                        trailingContent = {
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
