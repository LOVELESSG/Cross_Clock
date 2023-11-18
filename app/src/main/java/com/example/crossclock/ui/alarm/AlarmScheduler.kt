package com.example.crossclock.ui.alarm

import com.example.crossclock.data.alarm.Alarm

interface AlarmScheduler {
    fun scheduler(item: Alarm)
    fun cancel(item: Alarm)
}