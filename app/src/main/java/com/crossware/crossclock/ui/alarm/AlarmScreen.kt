package com.crossware.crossclock.ui.alarm

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.crossware.crossclock.R
import com.crossware.crossclock.data.ALL_CITIES
import com.crossware.crossclock.data.AlarmState
import com.crossware.crossclock.data.AlarmViewModel
import com.crossware.crossclock.data.alarm.Alarm
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 闹钟页面的主入口组合项。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    paddingValues: PaddingValues,
    scheduler: CrossAlarmScheduler,
    openAddAlarmDialog: Boolean,
    onDismissAddAlarmDialog: () -> Unit,
    alarmViewModel: AlarmViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    
    var hasPermission by remember { mutableStateOf(notificationManager.areNotificationsEnabled()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = notificationManager.areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 同步闹钟定时任务调度
    LaunchedEffect(hasPermission, alarmViewModel.state.items) {
        alarmViewModel.state.items.forEach { item ->
            if (hasPermission && item.onOrOff && !item.isExpired()) {
                scheduler.scheduler(item)
            } else {
                scheduler.cancel(item)
            }
        }
    }

    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }

    LaunchedEffect(openAddAlarmDialog) {
        if (openAddAlarmDialog) {
            val now = LocalDateTime.now()
            val defaultTime = now.plusHours(1).withMinute(0).withSecond(0).withNano(0)
            editingAlarm = Alarm(
                time = defaultTime,
                message = "", // 未输入时留空，呈现占位词“请输入闹钟标题”，保存时若仍为空则自动置为“跨时区闹钟”
                timeZone = ZoneId.systemDefault(),
                onOrOff = true
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
        if (!hasPermission) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.notification_warn_title), fontWeight = FontWeight.Bold)
                        Text(text = stringResource(R.string.notification_warn_text), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.enable_notification))
                    }
                }
            }
        }

        // 闹钟列表主区域
        AlarmContent(
            state = alarmViewModel.state,
            deleteAlarm = alarmViewModel::deleteAlarm,
            padding = PaddingValues(0.dp),
            changeAlarmStatus = alarmViewModel::updateAlarmStatus,
            onEditAlarm = { alarm -> editingAlarm = alarm }
        )
    }
    
    if (editingAlarm != null) {
        AlarmEditSheet(
            alarm = editingAlarm!!,
            onDismiss = {
                editingAlarm = null
                if (openAddAlarmDialog) onDismissAddAlarmDialog()
            },
            onSave = { updatedAlarm ->
                if (updatedAlarm.id == 0) {
                    alarmViewModel.addAlarm(updatedAlarm)
                } else {
                    alarmViewModel.updateAlarm(updatedAlarm)
                }
                editingAlarm = null
                if (openAddAlarmDialog) onDismissAddAlarmDialog()
            }
        )
    }
}

/**
 * 渲染跨时区闹钟列表，支持左滑露红底删除按键。
 */
@Composable
fun AlarmContent(
    state: AlarmState,
    deleteAlarm: (Alarm) -> Unit,
    padding: PaddingValues,
    changeAlarmStatus: (Alarm) -> Unit,
    onEditAlarm: (Alarm) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopStart
    ) {
        if (state.items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_alarm_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.items, key = { it.id }) { alarm ->
                    SwipeableAlarmCard(
                        alarm = alarm,
                        onDelete = { deleteAlarm(alarm) },
                        onEdit = { onEditAlarm(alarm) },
                        onToggle = { changeAlarmStatus(alarm) }
                    )
                }
            }
        }
    }
}

/**
 * 可左滑显示右侧纯图标删除按钮的跨时区闹钟卡片组件。
 */
@Composable
private fun SwipeableAlarmCard(
    alarm: Alarm,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "swipe_offset")

    val maxSwipePx = -160f // 左滑的最大偏移量

    val draggableState = rememberDraggableState { delta ->
        val newOffset = (offsetX + delta).coerceIn(maxSwipePx, 0f)
        offsetX = newOffset
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // 背景侧滑显露的红色删除按键（仅保留图标，无文字）
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier
                .fillMaxSize()
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
                    contentDescription = "删除闹钟",
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
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        // 松手自动吸附开/关
                        offsetX = if (offsetX < maxSwipePx / 2) maxSwipePx else 0f
                    }
                )
                .clickable {
                    if (offsetX < 0f) {
                        offsetX = 0f // 若处于侧滑展开状态，点击收回
                    } else {
                        onEdit() // 点击卡片主体进入编辑 Sheet
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 第一行：时间大字号 (06:30 AM / 14:00 PM / 09:00) + 开关 Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val locale = LocalConfiguration.current.locales[0]
                    val isPm = alarm.time.hour >= 12
                    val timeStr = String.format(locale, "%02d:%02d", alarm.time.hour, alarm.time.minute)

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeStr,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (alarm.isExpired()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (alarm.isExpired()) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPm) "PM" else "AM",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    Switch(
                        checked = alarm.onOrOff && !alarm.isExpired(),
                        onCheckedChange = { onToggle() },
                        enabled = !alarm.isExpired()
                    )
                }

                // 第二行：标题与标签名 + 重复周期 Pill 标签
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alarm.message.ifBlank { "跨时区闹钟" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (alarm.isExpired()) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = if (alarm.isExpired()) "已过期 (Expired)" else alarm.getRepeatDisplayText(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isExpired()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // 第三行：目标时区名称 + 对应本地时间 + 倒计时/状态文本
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cityInfo = ALL_CITIES.find { it.cityTimeZoneId == alarm.timeZone }
                    val zoneName = cityInfo?.city ?: alarm.timeZone.id
                    val nowZdt = ZonedDateTime.now(alarm.timeZone)
                    val offsetStr = nowZdt.format(DateTimeFormatter.ofPattern("O"))
                    val localEqTime = alarm.getLocalEquivalentTimeString()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$zoneName $offsetStr · 对应本地 $localEqTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = alarm.getTimeRemainingText(),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (alarm.isExpired()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
