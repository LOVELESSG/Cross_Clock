package com.crossware.crossclock.ui

import android.app.Application
import com.crossware.crossclock.Graph

class CrossClockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}