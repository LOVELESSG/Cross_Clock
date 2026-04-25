package com.crossware.crossclock.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossware.crossclock.data.alarm.Alarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 闹钟页面的 ViewModel，负责管理闹钟列表的状态并执行数据库操作。
 * 
 * @param repository 数据仓库，提供统一的数据访问接口。
 */
@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: Repository
): ViewModel() {
    
    // UI 状态，驱动 AlarmScreen 的列表渲染
    var state by mutableStateOf(AlarmState())
        private set

    init {
        // 初始化时开始监听数据库中的所有闹钟记录
        getAllAlarm()
    }

    /**
     * 从仓库获取所有闹钟。
     * 使用 collectLatest 确保当数据库更新时，UI 状态能自动同步。
     */
    private fun getAllAlarm(){
        viewModelScope.launch {
            repository.allAlarm.collectLatest {
                state = state.copy(
                    items = it
                )
            }
        }
    }

    /**
     * 向数据库添加新闹钟。
     */
    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.insertAlarm(alarm)
        }
    }

    /**
     * 从数据库删除指定闹钟。
     */
    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    /**
     * 快速切换闹钟的开启/关闭状态。
     */
    fun updateAlarmStatus(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarmStatus(alarm)
        }
    }

    /**
     * 更新已有闹钟的全部信息（如修改了时间、标签或时区）。
     */
    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
        }
    }
}

/**
 * 闹钟页面的 UI 状态模型。
 * 
 * @param items 闹钟列表数据。
 */
data class AlarmState(
    val items: List<Alarm> = emptyList()
)
