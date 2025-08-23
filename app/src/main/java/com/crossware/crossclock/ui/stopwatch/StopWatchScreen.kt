package com.crossware.crossclock.ui.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossware.crossclock.R
import com.crossware.crossclock.service.ServiceHelper
import com.crossware.crossclock.service.StopWatchService
import com.crossware.crossclock.service.StopWatchState
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_CANCEL
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_START
import com.crossware.crossclock.util.Constants.ACTION_SERVICE_STOP

@Composable
fun StopWatchScreen(
    paddingValues: PaddingValues,
    stopWatchService: StopWatchService
) {
    StopWatchContent(stopWatchService, paddingValues)
}

@Composable
fun StopWatchContent(stopWatchService: StopWatchService, padding: PaddingValues) {

    val context = LocalContext.current
    val hours by stopWatchService.hours
    val minutes by stopWatchService.minutes
    val seconds by stopWatchService.seconds
    val currentState by stopWatchService.currentState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(padding), // Apply padding from NavHost
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.weight(weight = 2f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(targetState = hours, transitionSpec = { addAnimation() }, label = "") { targetCount ->
                Text(
                    text = targetCount,
                    style = TextStyle(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (hours == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                    )
                )
            }
            AnimatedContent(targetState = minutes, transitionSpec = { addAnimation() },
                label = ""
            ) { targetCount ->
                Text(
                    text = targetCount,
                    style = TextStyle(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (minutes == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                    )
                )
            }
            AnimatedContent(targetState = seconds, transitionSpec = { addAnimation() },
                label = ""
            ) { targetCount ->
                Text(
                    text = targetCount,
                    style = TextStyle(
                        fontSize = MaterialTheme.typography.displayLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (seconds == "00") MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
        Row(
            modifier = Modifier.weight(weight = 1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                modifier = Modifier
                    .padding(start = 24.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                onClick = {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = if (currentState == StopWatchState.Started) ACTION_SERVICE_STOP
                        else ACTION_SERVICE_START
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                    contentColor = if (currentState == StopWatchState.Started) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text(
                    text = when (currentState) {
                        StopWatchState.Started -> stringResource(R.string.stopwatch_stop)
                        StopWatchState.Stopped -> stringResource(R.string.stopwatch_resume)
                        else -> stringResource(R.string.stopwatch_start)
                    }
                )
            }
            Spacer(modifier = Modifier.width(24.dp))
            Button(
                modifier = Modifier
                    .padding(end = 24.dp)
                    .weight(1f)
                    .fillMaxWidth(),
                onClick = {
                    ServiceHelper.triggerForegroundService(
                        context = context,
                        action = ACTION_SERVICE_CANCEL
                    )
                },
                enabled = seconds != "00" && currentState != StopWatchState.Started,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(text = stringResource(R.string.stopwatch_cancel))
            }
        }
    }
}

fun addAnimation(duration: Int = 100): ContentTransform {
    return slideInVertically(animationSpec = tween(durationMillis = duration)) { height -> height } + fadeIn(
        animationSpec = tween(durationMillis = duration)
    ) togetherWith slideOutVertically(animationSpec = tween(durationMillis = duration)) { height -> height } + fadeOut(
        animationSpec = tween(durationMillis = duration)
    )
}
