package com.crossware.crossclock

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.compose.CrossClockTheme
import kotlin.system.exitProcess

/**
 * 闹钟响起时的全屏展示 Activity。
 * 该界面会在锁屏状态下弹出，允许用户查看闹钟备注并点击取消。
 */
class FullScreenAlarmNotification : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrossClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 获取从 AlarmReceiver 传递过来的闹钟消息
                    val message = intent.getStringExtra("alarmMessage")
                    WakeupAlarmPage(this, message)
                }
            }
        }
    }
}

/**
 * 唤醒闹钟界面的内容组合项。
 * 
 * @param context 上下文对象。
 * @param message 闹钟显示的标签消息。
 */
@Composable
fun WakeupAlarmPage(context: Context, message: String?) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // 控制是否执行取消动作
    var pageStatus by remember { mutableStateOf(false) }
    
    // 拦截物理返回键，防止用户误触关闭界面而不取消闹钟
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backCallback = remember {
        object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                // 不执行任何操作，强制用户点击界面上的取消按钮
            }
        }
    }
    backDispatcher?.addCallback(backCallback)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 闹钟消息显示区
            Box(
                modifier = Modifier.weight(2f),
                contentAlignment = Alignment.Center
            ) {
                if (message != "" && message != null)
                    Text(
                        text = message,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                else
                    Text(
                        text = stringResource(R.string.alarm_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
            }
            
            // 取消按钮区
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp),
                    onClick = { pageStatus = !pageStatus }
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel the alarm")
                }
            }
        }
    }

    // 执行取消闹钟的清理逻辑
    if (pageStatus) {
        notificationManager.cancel(24778) // 移除通知
        exitProcess(0) // 结束进程以停止铃声
    }
}
