package com.crossware.crossclock.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.crossware.crossclock.service.StopWatchState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StopWatchViewModel @Inject constructor() : ViewModel() {
    private val _hours = mutableStateOf("00")
    val hours: State<String> = _hours

    private val _minutes = mutableStateOf("00")
    val minutes: State<String> = _minutes

    private val _seconds = mutableStateOf("00")
    val seconds: State<String> = _seconds

    private val _currentState = mutableStateOf(StopWatchState.Idle)
    val currentState: State<StopWatchState> = _currentState

    fun updateTime(h: String, m: String, s: String) {
        _hours.value = h
        _minutes.value = m
        _seconds.value = s
    }

    fun updateState(state: StopWatchState) {
        _currentState.value = state
    }
}
