package com.crossware.crossclock.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

class AlarmCancelReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        exitProcess(0)
    }
}