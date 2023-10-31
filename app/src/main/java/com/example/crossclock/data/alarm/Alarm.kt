package com.example.crossclock.data.alarm

import java.time.LocalTime

data class Alarm(
    val time: LocalTime,
    val message: String,
    val timeZone: String,
    val date: String
)
