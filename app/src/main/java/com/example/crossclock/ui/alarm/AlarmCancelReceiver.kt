package com.example.crossclock.ui.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import kotlin.system.exitProcess

class AlarmCancelReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        exitProcess(0)
    }
}