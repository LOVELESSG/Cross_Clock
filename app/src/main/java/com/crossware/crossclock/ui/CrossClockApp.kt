package com.crossware.crossclock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
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
import com.crossware.crossclock.data.PRESET_CITIES
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
import com.crossware.crossclock.util.WorldClockUtils
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 应用的主 UI 组合项。
 * 负责构建应用的整体布局，包括顶部栏 (TopAppBar)、底部导航栏 (NavigationBar) 和悬浮按钮 (FloatingActionButton)。
 * 同时集成了 NavHost 来处理页面切换。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossClockApp(
    navController: NavHostController = rememberNavController(),
    alarmScheduler: CrossAlarmScheduler,
    deepLinkRoute: String? = null,
    deepLinkTimerId: Long? = null
) {
    var openWorldClockBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openAlarmAddDialog by rememberSaveable { mutableStateOf(false) }
    var openTimerAddSheet by rememberSaveable { mutableStateOf(false) }
    var editingTimer by remember { mutableStateOf<TimerEntity?>(null) }
    var isTimerListExpanded by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    val worldClockViewModel: WorldClockViewModel = hiltViewModel()
    val worldClockState = worldClockViewModel.state
    val homepageList = worldClockState.items

    val timerViewModel: TimerViewModel = hiltViewModel()

    LaunchedEffect(deepLinkRoute, deepLinkTimerId) {
        if (deepLinkRoute == Screen.TimerRunning.route) {
            if (deepLinkTimerId != null) {
                timerViewModel.selectActiveTimer(deepLinkTimerId)
            }
            if (timerViewModel.selectedActiveTimer != null) {
                navController.navigate(Screen.TimerRunning.route) { launchSingleTop = true }
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    if (currentRoute == Screen.TimerRunning.route) {
        TimerRunningScreen(
            timerViewModel = timerViewModel,
            onExit = { navController.popBackStack() }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        val titleResId = DRAWER_ITEMS.find { it.route == currentRoute }?.name ?: R.string.app_name
                        Text(text = stringResource(titleResId))
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                NavigationBar {
                    DRAWER_ITEMS.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(painter = painterResource(id = item.icon), contentDescription = stringResource(item.name)) },
                            label = {
                                Text(
                                    text = stringResource(item.name),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            selected = item.route == currentRoute,
                            onClick = {
                                val activeRoute = navController.currentDestination?.route
                                if (item.route != activeRoute) {
                                    navController.navigate(route = item.route) {
                                        if (activeRoute != null) {
                                            popUpTo(activeRoute) {
                                                inclusive = true
                                                saveState = true
                                            }
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            },
            floatingActionButton = {
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
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Home.route) {
                    val tick by worldClockViewModel.currentTimeTick.collectAsState()
                    WorldClockContent(
                        homepageList = homepageList,
                        tick = tick,
                        padding = PaddingValues(0.dp),
                        onDeleteWorldClock = worldClockViewModel::deleteWorldClock
                    )
                }
                composable(Screen.Alarm.route) {
                    AlarmScreen(
                        paddingValues = PaddingValues(0.dp),
                        scheduler = alarmScheduler,
                        openAddAlarmDialog = openAlarmAddDialog,
                        onDismissAddAlarmDialog = { openAlarmAddDialog = false }
                    )
                }
                composable(Screen.StopWatch.route) {
                    StopWatchScreen(paddingValues = PaddingValues(0.dp))
                }
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
                composable(Screen.TimerRunning.route) {
                    TimerRunningScreen(
                        timerViewModel = timerViewModel,
                        onExit = { navController.popBackStack() }
                    )
                }
            }

            if (openWorldClockBottomSheet) {
                WorldClockSelectPage(
                    state = worldClockViewModel.state,
                    changeStatus = { openWorldClockBottomSheet = !openWorldClockBottomSheet },
                    addWorldClock = worldClockViewModel::addWorldClock,
                    deleteWorldClock = worldClockViewModel::deleteWorldClock,
                    removeWorldClockByName = worldClockViewModel::deleteWorldClockByName
                )
            }

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

        val activeTimers = timerViewModel.activeTimers
        val focusedTimer = timerViewModel.selectedActiveTimer
        if (isTimerListExpanded && activeTimers.size > 1 && focusedTimer != null) {
            BackHandler(enabled = true) {
                isTimerListExpanded = false
            }

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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                                timerViewModel.selectActiveTimer(candidate.timer.id)
                                isTimerListExpanded = false
                            }
                        )
                    }
                }

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

                    Box(modifier = Modifier.size(56.dp))
                }
            }
        }
    }
}

/**
 * 世界时钟主内容的组合项。
 */
@Composable
fun WorldClockContent(
    homepageList: List<WorldClock>,
    tick: Long,
    padding: PaddingValues,
    onDeleteWorldClock: (WorldClock) -> Unit = {}
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val nowInstant = remember(tick) { Instant.ofEpochMilli(tick) }

    // 本地系统时区及城市名称解析（基于当前 Locale 实时获取单城名）
    val localZoneId = remember { ZoneId.systemDefault() }
    val presetLocalCity = remember(localZoneId) {
        PRESET_CITIES.find { it.zoneId == localZoneId }
    }
    val localCityName = presetLocalCity?.getLocalizedCityName(locale)
        ?: ALL_CITIES.find { it.cityTimeZoneId == localZoneId }?.getDisplayCityName()
        ?: "Tokyo"

    val localZdt = remember(tick) { ZonedDateTime.ofInstant(nowInstant, localZoneId) }

    val localTimeStr = localZdt.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val localDateStr = localZdt.format(DateTimeFormatter.ofPattern("MM/dd EEEE", locale))
    val localOffsetStr = WorldClockUtils.getTimeZoneOffsetString(localZoneId, nowInstant)
    val (localPeriodDesc, localDaylightProgress) = WorldClockUtils.getTimePeriodAndProgress(context, localZoneId, nowInstant)
    val sunCountdownStr = WorldClockUtils.getSunCountdownText(context, localZoneId, nowInstant)
    val isDaytime = localZdt.hour in 6..18
    val sunIcon = if (isDaytime) "☀️" else "🌙"

    // 检测同名城市
    val cityGroupMap = remember(homepageList) {
        homepageList.groupBy { it.getDisplayCityName() }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. 顶部本地信息卡片 ---
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // 顶行：当前时区 · 城市名 + 时区 Offset Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = stringResource(R.string.current_timezone),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.current_timezone)} · $localCityName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = localOffsetStr,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 大号本地时间 + 缩写 + 右侧 Mini 表盘
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = localTimeStr,
                                fontSize = 52.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = localZoneId.id.takeLast(3).uppercase(locale),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        MiniAnalogClock(
                            time = localZdt,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 本地日期
                    Text(
                        text = localDateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 昼夜日出/日落倒计时与进度条
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$sunIcon $localPeriodDesc · $sunCountdownStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        LinearProgressIndicator(
                            progress = { localDaylightProgress },
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // --- 2. 关注城市标题栏 ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.followed_cities),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.city_count_format, homepageList.size),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // --- 3. 关注城市列表卡片 (带左滑删除与同名城市小字展示) ---
        if (homepageList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_world_clock_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(homepageList, key = { it.id }) { item ->
                val sameCityCount = cityGroupMap[item.getDisplayCityName()]?.size ?: 1
                SwipeableWorldClockCard(
                    item = item,
                    sameCityCount = sameCityCount,
                    tick = tick,
                    locale = locale,
                    nowInstant = nowInstant,
                    localZoneId = localZoneId,
                    onDelete = { onDeleteWorldClock(item) }
                )
            }
        }
    }
}

/**
 * 可左滑显示右侧删除按钮的关注城市卡片组件。
 */
@Composable
private fun SwipeableWorldClockCard(
    item: WorldClock,
    sameCityCount: Int,
    tick: Long,
    locale: Locale,
    nowInstant: Instant,
    localZoneId: ZoneId,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")
    val maxSwipePx = -160f

    val draggableState = rememberDraggableState { delta ->
        val newOffset = (offsetX + delta).coerceIn(maxSwipePx, 0f)
        offsetX = newOffset
    }

    val cityName = item.getLocalizedCityName(locale)
    val cityZdt = remember(tick) { ZonedDateTime.ofInstant(nowInstant, item.cityTimeZoneId) }
    val cityTimeStr = cityZdt.format(DateTimeFormatter.ofPattern("HH:mm", locale))

    val offsetStr = WorldClockUtils.getTimeZoneOffsetString(item.cityTimeZoneId, nowInstant)
    val diffStr = WorldClockUtils.getTimeDifferenceString(context, item.cityTimeZoneId, localZoneId, nowInstant)
    val relativeDateStr = WorldClockUtils.getRelativeDateString(context, item.cityTimeZoneId, localZoneId, nowInstant, locale)
    val (periodDesc, dayProgress) = WorldClockUtils.getTimePeriodAndProgress(context, item.cityTimeZoneId, nowInstant)

    val periodIcon = if (cityZdt.hour in 6..18) "☀️" else "🌙"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.CenterEnd
    ) {
        // 背景侧滑显露的红色删除按键（精确使用 matchParentSize 强行与卡片高度完全一致）
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .matchParentSize()
                .clickable { onDelete() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_timer),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 上层滑动卡片主体
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        offsetX = if (offsetX < maxSwipePx / 2) maxSwipePx else 0f
                    }
                )
                .clickable {
                    if (offsetX < 0f) {
                        offsetX = 0f
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶行：城市名 (+ 同名城市小字省份/国家) + 时区 Badge | 城市时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cityName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Text(
                                    text = offsetStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // 仅当列表中存在同名城市时，展示小字省份和国家
                        if (sameCityCount > 1) {
                            val countryName = item.getLocalizedCountryName(locale)
                            val adminArea = item.getDisplayAdminArea()
                            val subLocationText = if (!adminArea.isNullOrBlank() && !countryName.isNullOrBlank()) {
                                "$adminArea, $countryName"
                            } else {
                                countryName ?: ""
                            }

                            if (subLocationText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subLocationText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "$periodIcon $cityTimeStr",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 次行：相对日期与时间差 | 氛围描述
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$relativeDateStr · $diffStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = periodDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 底环：白天/夜间进度条
                LinearProgressIndicator(
                    progress = { dayProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * 迷你模拟表盘 Canvas 组件。
 */
@Composable
private fun MiniAnalogClock(
    time: ZonedDateTime,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = outlineColor,
            radius = radius,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )

        for (i in 0 until 12) {
            val angleRad = Math.toRadians((i * 30).toDouble())
            val startRadius = radius - 5.dp.toPx()
            val endRadius = radius - 1.5.dp.toPx()
            val startX = center.x + (startRadius * Math.sin(angleRad)).toFloat()
            val startY = center.y - (startRadius * Math.cos(angleRad)).toFloat()
            val endX = center.x + (endRadius * Math.sin(angleRad)).toFloat()
            val endY = center.y - (endRadius * Math.cos(angleRad)).toFloat()
            drawLine(
                color = outlineColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.dp.toPx()
            )
        }

        val hour = time.hour % 12
        val minute = time.minute
        val second = time.second

        val minuteAngle = Math.toRadians(((minute + second / 60f) * 6).toDouble())
        val hourAngle = Math.toRadians(((hour + minute / 60f) * 30).toDouble())

        val hourLength = radius * 0.5f
        val hourX = center.x + (hourLength * Math.sin(hourAngle)).toFloat()
        val hourY = center.y - (hourLength * Math.cos(hourAngle)).toFloat()
        drawLine(
            color = onSurfaceColor,
            start = center,
            end = Offset(hourX, hourY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        val minuteLength = radius * 0.75f
        val minuteX = center.x + (minuteLength * Math.sin(minuteAngle)).toFloat()
        val minuteY = center.y - (minuteLength * Math.cos(minuteAngle)).toFloat()
        drawLine(
            color = primaryColor,
            start = center,
            end = Offset(minuteX, minuteY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        drawCircle(
            color = primaryColor,
            radius = 2.5.dp.toPx(),
            center = center
        )
    }
}
