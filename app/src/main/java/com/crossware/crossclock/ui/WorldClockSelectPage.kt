package com.crossware.crossclock.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.crossware.crossclock.data.WorldClockState
import com.crossware.crossclock.data.worldclock.WorldClock
import com.crossware.crossclock.ui.components.CitySearchContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockSelectPage(
    state: WorldClockState,
    changeStatus: () -> Unit,
    addWorldClock: (WorldClock) -> Unit,
    deleteWorldClock: (WorldClock) -> Unit,
    removeWorldClockByName: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { changeStatus() }
    ) {
        CitySearchContent(
            isMultiSelect = true,
            isChecked = { searchResult ->
                // 判断当前具体的搜索项是否已被精确添加到世界时钟列表中
                remember(state.items, searchResult) {
                    state.items.any { clock -> clock.isMatchingSearchResult(searchResult) }
                }
            },
            onCityToggled = { searchResult, checked ->
                if (checked) {
                    val worldClock = WorldClock(
                        city = searchResult.displayLocationName,
                        country = searchResult.country,
                        cityTimeZoneId = searchResult.zoneId
                    )
                    addWorldClock(worldClock)
                } else {
                    // 查找并准确删除对应的已添加时钟
                    val existing = state.items.find { clock -> clock.isMatchingSearchResult(searchResult) }
                    if (existing != null) {
                        deleteWorldClock(existing)
                    }
                }
            }
        )
    }
}
