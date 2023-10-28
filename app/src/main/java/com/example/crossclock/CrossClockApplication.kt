package com.example.crossclock

import android.app.Application

class CrossClockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}