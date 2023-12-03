package com.example.crossclock

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.crossclock.data.AppDatabase
import com.example.crossclock.ui.theme.CrossClockTheme
import kotlin.system.exitProcess

class FullScreenAlarmNotification : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrossClockTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val alarmId = intent.getIntExtra("alarmId", 0)
                    WakeupAlarmPage(this)
                }
            }
        }
    }
}

@Composable
fun WakeupAlarmPage(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var pageStatus by remember {
        mutableStateOf(false)
    }
    Column {
        Button(onClick = { pageStatus = !pageStatus }) {
            Text(text = "Close")
        }
        if (pageStatus) {
            notificationManager.cancel(24778)
            exitProcess(0)
        }
    }
}

@Composable
fun Greeting(name: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CrossClockTheme {
        Greeting(0)
    }
}