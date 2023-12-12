package com.crossware.crossclock.ui.alarm

import com.crossware.crossclock.data.alarm.Alarm

interface AlarmScheduler {
    fun scheduler(item: Alarm)
    fun cancel(item: Alarm)
}