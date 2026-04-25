package com.crossware.crossclock.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossware.crossclock.data.worldclock.WorldClock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * 世界时钟的 ViewModel，负责处理 UI 逻辑并与仓库层 (Repository) 交互。
 * 使用 Hilt 进行依赖注入。
 */
@HiltViewModel
class WorldClockViewModel @Inject constructor(
    private val repository: Repository
): ViewModel(){
    
    // UI 状态，包含用户添加的世界时钟列表
    var state by mutableStateOf(WorldClockState())
        private set

    // 时间滴答状态流，每秒更新一次，用于触发 UI 中的时间刷新
    private val _currentTimeTick = MutableStateFlow(Instant.now().toEpochMilli())
    val currentTimeTick: StateFlow<Long> = _currentTimeTick.asStateFlow()

    init {
        // 初始化时加载所有世界时钟并启动时间滴答
        getAllWorldClock()
        startClockTick()
    }

    /**
     * 启动一个协程，每秒更新一次时间戳，以驱动 UI 实时刷新时间显示。
     */
    private fun startClockTick() {
        viewModelScope.launch {
            while (true) {
                _currentTimeTick.value = Instant.now().toEpochMilli()
                delay(1000) // 延迟 1 秒
            }
        }
    }
    
    /**
     * 从仓库获取所有已保存的世界时钟，并观察其变化。
     */
    private fun getAllWorldClock(){
        viewModelScope.launch {
            repository.allWorldClock.collectLatest {
                state = state.copy(
                    items = it
                )
            }
        }
    }

    /**
     * 添加一个新的世界时钟。
     */
    fun addWorldClock(worldClock: WorldClock){
        viewModelScope.launch {
            repository.insertWorldClock(worldClock)
        }
    }

    /**
     * 删除一个世界时钟对象。
     */
    fun deleteWorldClock(worldClock: WorldClock){
        viewModelScope.launch {
            repository.deleteWorldClock(worldClock)
        }
    }

    /**
     * 根据城市名称删除世界时钟。
     */
    fun deleteWorldClockByName(city: String){
        viewModelScope.launch{
            repository.deleteWorldClockByName(city)
        }
    }

    /**
     * 检查某个城市是否已存在于世界时钟列表中。
     * 返回一个 Flow<Boolean>。
     */
    fun existWorldClock(city: String): Flow<Boolean> {
        return repository.existsWorldClock(city)
    }
}

/**
 * 表示世界时钟页面的 UI 状态。
 */
data class WorldClockState(
    val items: List<WorldClock> = emptyList(),
)
