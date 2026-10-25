package com.crossware.crossclock.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.crossware.crossclock.data.stopwatch.LapItem
import com.crossware.crossclock.service.StopWatchState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 秒表界面的 ViewModel。
 * 负责保存秒表的计时毫秒数、分圈数据和运行状态，并由 StopWatchService 进行更新。
 */
@HiltViewModel
class StopWatchViewModel @Inject constructor() : ViewModel() {

    // 累计走时（毫秒）
    private val _elapsedMillis = mutableLongStateOf(0L)
    val elapsedMillis: State<Long> = _elapsedMillis

    // 秒表运行状态（Idle、Started、Stopped、Canceled）
    private val _currentState = mutableStateOf(StopWatchState.Idle)
    val currentState: State<StopWatchState> = _currentState

    // 历史分圈计次列表（最新记录排在最前）
    private val _laps = mutableStateOf<List<LapItem>>(emptyList())
    val laps: State<List<LapItem>> = _laps

    fun updateElapsedMillis(millis: Long) {
        _elapsedMillis.longValue = millis
    }

    fun updateState(state: StopWatchState) {
        _currentState.value = state
    }

    /**
     * 记录一圈计次数据。
     */
    fun recordLap() {
        val totalMillis = _elapsedMillis.longValue
        if (totalMillis <= 0L) return

        val previousTotal = _laps.value.firstOrNull()?.totalTimeMillis ?: 0L
        val lapTime = totalMillis - previousTotal
        val newLapIndex = _laps.value.size + 1

        val rawList = _laps.value.toMutableList()
        rawList.add(
            0,
            LapItem(
                lapIndex = newLapIndex,
                lapTimeMillis = lapTime,
                totalTimeMillis = totalMillis,
                isBest = false
            )
        )

        // 重新计算最佳单圈（最少用时）
        val minLapTime = rawList.minOfOrNull { it.lapTimeMillis } ?: lapTime
        val updatedList = rawList.map { item ->
            item.copy(isBest = rawList.size >= 2 && item.lapTimeMillis == minLapTime)
        }

        _laps.value = updatedList
    }

    /**
     * 清除历史分圈记录。
     */
    fun clearLaps() {
        _laps.value = emptyList()
    }

    /**
     * 重置秒表状态及数据。
     */
    fun resetStopwatch() {
        _elapsedMillis.longValue = 0L
        _currentState.value = StopWatchState.Idle
        _laps.value = emptyList()
    }
}
