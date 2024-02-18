package com.crossware.crossclock.ui

import android.app.Application
import com.crossware.crossclock.Graph
import dagger.hilt.android.HiltAndroidApp

class CrossClockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}