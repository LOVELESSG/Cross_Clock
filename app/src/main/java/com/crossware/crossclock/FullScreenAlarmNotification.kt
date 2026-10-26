package com.crossware.crossclock

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossware.crossclock.ui.alarm.AlarmReceiver
import com.crossware.crossclock.ui.alarm.AlarmRingtoneService
import com.example.compose.CrossClockTheme
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 跨时区闹钟响起的锁屏/全屏展示 Activity。
 * 设置独立 Task 隔离与从多任务窗口排除 (excludeFromRecents)，防止后台紊乱。
 */
class FullScreenAlarmNotification : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        val message = intent.getStringExtra("alarmMessage") ?: "闹钟"
        val ringtoneTitle = intent.getStringExtra("ringtoneTitle")

        setContent {
            CrossClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121314)
                ) {
                    WakeupAlarmPage(message, ringtoneTitle)
                }
            }
        }
    }

    override fun onDestroy() {

        AlarmRingtoneService.stopRingtone(this)
        super.onDestroy()
    }
}

/**
 * 响铃全屏界面 UI 组件。
 */
@Composable
fun WakeupAlarmPage(message: String, ringtoneTitle: String?) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 不响应物理返回键，强制滑块或稍后提醒
            }
        }
    }
    backDispatcher?.addCallback(backCallback)

    val nowZdt = ZonedDateTime.now(ZoneId.systemDefault())
    val dateStr = nowZdt.format(DateTimeFormatter.ofPattern("MM月dd日 EEEE", locale))
    val timeStr = nowZdt.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val isPm = nowZdt.hour >= 12

    val displayRingtoneTitle = if (ringtoneTitle.isNullOrBlank() || ringtoneTitle == "系统默认闹铃") {
        "默认铃声"
    } else {
        ringtoneTitle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- 顶部状态指示栏 ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "屏幕已锁定 · 响铃中",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Text(
                    text = "$dateStr · 本地时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }

        // --- 中间主时间与闹钟标题信息区 ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = timeStr,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPm) "PM" else "AM",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA8E0BA),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AudioWaveVisualizer()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = message,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.08f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayRingtoneTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 稍后提醒（Snooze 10 分钟）按键
            Card(
                shape = RoundedCornerShape(50),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable {
                        // 立刻打断并停止声音
                        AlarmRingtoneService.stopRingtone(context)
                        notificationManager.cancel(24778)

                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra("message", "$message (稍后提醒)")
                            putExtra("time", LocalTime.now().plusMinutes(10).toString())
                            putExtra("ringtoneTitle", ringtoneTitle)
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            99999,
                            snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val snoozeTriggerMillis = System.currentTimeMillis() + 10 * 60 * 1000L
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTriggerMillis,
                            pendingIntent
                        )

                        Toast.makeText(context, "闹钟已设置在 10 分钟后再次提醒", Toast.LENGTH_SHORT).show()
                        (context as? Activity)?.finishAndRemoveTask()
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "稍后提醒",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "稍后提醒",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF3F51B5)
                    ) {
                        Text(
                            text = "小睡 10 分钟",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // --- 底部：向右滑动关闭闹钟滑块组件 ---
        SlideToDismissAlarmBar(
            onDismissAlarm = {
                // 一旦关停，立刻打断并停止声音
                AlarmRingtoneService.stopRingtone(context)
                notificationManager.cancel(24778)
                (context as? Activity)?.finishAndRemoveTask()
            }
        )
    }
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * f,
        green = start.green + (stop.green - start.green) * f,
        blue = start.blue + (stop.blue - start.blue) * f,
        alpha = start.alpha + (stop.alpha - start.alpha) * f
    )
}

/**
 * 向右滑动关闭闹钟的底栏组件（具备 50% 色彩渐变 & 80% 警示红/图标切换/吸附关闭，支持全宽动态测量）。
 */
@Composable
private fun SlideToDismissAlarmBar(
    onDismissAlarm: () -> Unit
) {
    val density = LocalDensity.current
    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    // 动态计算最大滑行距离 (组件总宽度 - 圆盘宽度 - 左右内边距)
    val thumbSizePx = with(density) { 56.dp.toPx() }
    val paddingPx = with(density) { 16.dp.toPx() }
    val maxSwipePx = (trackWidthPx - thumbSizePx - paddingPx).coerceAtLeast(1f)

    val progress = (offsetX / maxSwipePx).coerceIn(0f, 1f)

    val initialColor = Color(0xFFA8E0BA) // 初始柔和浅绿
    val warningColor = Color(0xFFFF5252) // 警示红色

    val thumbColor = remember(progress) {
        when {
            progress < 0.5f -> initialColor
            progress >= 0.8f -> warningColor
            else -> {
                val fraction = (progress - 0.5f) / 0.3f
                lerpColor(initialColor, warningColor, fraction)
            }
        }
    }

    val thumbIcon = if (progress >= 0.8f) {
        Icons.AutoMirrored.Filled.VolumeOff
    } else {
        Icons.Default.AlarmOff
    }

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        label = "slide_offset",
        finishedListener = { finalValue ->
            if (finalValue >= maxSwipePx * 0.95f) {
                onDismissAlarm()
            }
        }
    )

    val draggableState = rememberDraggableState { delta ->
        val newOffset = (offsetX + delta).coerceIn(0f, maxSwipePx)
        offsetX = newOffset
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(8.dp)
            .onSizeChanged { size ->
                trackWidthPx = size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 背景提示文字
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "向右滑动关闭闹钟",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(18.dp)
            )
        }

        // 可拖动的滑块
        Surface(
            shape = CircleShape,
            color = thumbColor,
            modifier = Modifier
                .size(56.dp)
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (progress >= 0.8f) {
                            offsetX = maxSwipePx
                        } else {
                            offsetX = 0f
                        }
                    }
                )
                .clickable {
                    onDismissAlarm()
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = thumbIcon,
                    contentDescription = "关闭闹钟",
                    tint = if (progress >= 0.8f) Color.White else Color(0xFF1B3722),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * 动态音频波动 Visualizer。
 */
@Composable
private fun AudioWaveVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave")

    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 28f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 24f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(36.dp)
    ) {
        Box(modifier = Modifier.width(4.dp).height(bar1Height.dp).background(Color(0xFFA8E0BA), RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(4.dp).height(bar2Height.dp).background(Color(0xFFA8E0BA), RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(4.dp).height(bar3Height.dp).background(Color(0xFFA8E0BA), RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.width(4.dp).height(bar4Height.dp).background(Color(0xFFA8E0BA), RoundedCornerShape(2.dp)))
    }
}
