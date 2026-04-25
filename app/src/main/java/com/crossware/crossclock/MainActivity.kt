package com.crossware.crossclock

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import com.crossware.crossclock.navigation.nav_graph.SetupNavGraph
import com.crossware.crossclock.service.StopWatchService
import com.crossware.crossclock.ui.alarm.CrossAlarmScheduler
import com.example.compose.CrossClockTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp



/**
 * 应用的主 Activity。
 * 使用 @AndroidEntryPoint 注解以支持 Hilt 依赖注入。
 * 它是应用的入口点，负责设置 Compose 内容和初始化导航。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化闹钟调度器，用于在应用中管理闹钟任务。
        val scheduler = CrossAlarmScheduler(this)
        
        setContent {
            // 应用自定义主题
            CrossClockTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 初始化 NavHostController 以管理 Compose 导航栈
                    navController = rememberNavController()
                    
                    // 设置导航图，将导航控制器和闹钟调度器传递给 SetupNavGraph
                    SetupNavGraph(
                        navController = navController,
                        alarmScheduler = scheduler
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }
}
