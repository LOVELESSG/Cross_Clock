package com.crossware.crossclock.service

import android.content.Context
import android.content.Intent

object ServiceHelper {

    fun triggerForegroundService(context: Context, action: String) {
        Intent(context, StopWatchService::class.java).apply {
            this.action = action
            context.startService(this)
        }
    }
}