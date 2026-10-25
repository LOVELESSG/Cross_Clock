package com.crossware.crossclock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.crossware.crossclock.data.EXTRA_NAVIGATE_TO
import com.crossware.crossclock.data.EXTRA_TIMER_ID
import com.crossware.crossclock.navigation.nav_graph.SetupNavGraph
import com.crossware.crossclock.ui.alarm.CrossAlarmScheduler
import com.example.compose.CrossClockTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用的主 Activity。
 * 使用 @AndroidEntryPoint 注解以支持 Hilt 依赖注入。
 * 它是应用的入口点，负责设置 Compose 内容和初始化导航。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private var deepLinkRoute by mutableStateOf<String?>(null)
    private var deepLinkTimerId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scheduler = CrossAlarmScheduler(this)
        parseIntent(intent)

        setContent {
            CrossClockTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    navController = rememberNavController()

                    SetupNavGraph(
                        navController = navController,
                        alarmScheduler = scheduler,
                        deepLinkRoute = deepLinkRoute,
                        deepLinkTimerId = deepLinkTimerId
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent(intent)
    }

    private fun parseIntent(intent: Intent?) {
        deepLinkRoute = intent?.getStringExtra(EXTRA_NAVIGATE_TO)
        val timerId = intent?.getLongExtra(EXTRA_TIMER_ID, -1L) ?: -1L
        deepLinkTimerId = if (timerId != -1L) timerId else null
    }
}
