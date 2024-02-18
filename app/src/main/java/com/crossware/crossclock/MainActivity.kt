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
import com.crossware.crossclock.ui.theme.CrossClockTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp



class MainActivity : ComponentActivity() {

    private lateinit var navController: NavHostController
    private lateinit var stopWatchService: StopWatchService
    private var isBound by mutableStateOf(false)
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as StopWatchService.StopwatchBinder
            stopWatchService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, StopWatchService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scheduler = CrossAlarmScheduler(this)
        setContent {
            CrossClockTheme {
                // A surface container using the 'background' color from the theme
                if (isBound) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        navController = rememberNavController()
                        SetupNavGraph(
                            navController = navController,
                            alarmScheduler = scheduler,
                            stopWatchService = stopWatchService
                        )
                    }
                }

            }
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        isBound = false
    }
}
