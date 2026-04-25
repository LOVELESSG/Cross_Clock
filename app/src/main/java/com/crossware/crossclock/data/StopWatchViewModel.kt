package com.crossware.crossclock.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.crossware.crossclock.service.StopWatchState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 秒表界面的 ViewModel。
 * 负责保存秒表的计时数据和运行状态，并由 StopWatchService 进行更新。
 */
@HiltViewModel
class StopWatchViewModel @Inject constructor() : ViewModel() {
    
    // 小时数据状态
    private val _hours = mutableStateOf("00")
    val hours: State<String> = _hours

    // 分钟数据状态
    private val _minutes = mutableStateOf("00")
    val minutes: State<String> = _minutes

    // 秒钟数据状态
    private val _seconds = mutableStateOf("00")
    val seconds: State<String> = _seconds

    // 秒表运行状态（空闲、运行中、暂停等）
    private val _currentState = mutableStateOf(StopWatchState.Idle)
    val currentState: State<StopWatchState> = _currentState

    /**
     * 更新当前计时时间。
     */
    fun updateTime(h: String, m: String, s: String) {
        _hours.value = h
        _minutes.value = m
        _seconds.value = s
    }

    /**
     * 更新当前秒表状态。
     */
    fun updateState(state: StopWatchState) {
        _currentState.value = state
    }
}
