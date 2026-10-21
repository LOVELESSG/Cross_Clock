package com.crossware.crossclock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossware.crossclock.R
import com.crossware.crossclock.util.CitySearchResult
import com.crossware.crossclock.util.CitySearchService
import kotlinx.coroutines.launch

/**
 * 混合模式城市与时区搜索组件。
 * 支持搜索框输入，实时筛选本地预设城市，并支持通过在线 API 搜索全球任意城市及自动解析时区。
 *
 * @param onCitySelected 单选回调，用于选择单项城市时区（如设置闹钟）。
 * @param isMultiSelect 是否为多选模式（如世界时钟列表勾选）。
 * @param isChecked 多选模式下检测城市是否已勾选的回调。
 * @param onCityToggled 多选模式下勾选/取消勾选城市的回调。
 */
@Composable
fun CitySearchContent(
    modifier: Modifier = Modifier,
    onCitySelected: ((CitySearchResult) -> Unit)? = null,
    isMultiSelect: Boolean = false,
    isChecked: (@Composable (item: CitySearchResult) -> Boolean)? = null,
    onCityToggled: ((CitySearchResult, Boolean) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var localResults by remember { mutableStateOf(CitySearchService.searchLocal("")) }
    var onlineResults by remember { mutableStateOf<List<CitySearchResult>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var hasSearchedOnline by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // 当输入框变动时，实时更新本地搜索结果
    LaunchedEffect(searchQuery) {
        localResults = CitySearchService.searchLocal(searchQuery)
        hasSearchedOnline = false
        onlineResults = emptyList()
    }

    fun performOnlineSearch() {
        if (searchQuery.trim().length < 2 || isSearchingOnline) return
        coroutineScope.launch {
            isSearchingOnline = true
            val results = CitySearchService.searchOnline(searchQuery)
            onlineResults = results
            isSearchingOnline = false
            hasSearchedOnline = true
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        // 搜索输入框
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_city_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            // 本地搜索结果区块
            if (localResults.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.local_cities_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }

                itemsIndexed(
                    localResults,
                    key = { index, item -> "local_${item.cityName}_${item.zoneId.id}_$index" }
                ) { _, item ->
                    CityResultItem(
                        item = item,
                        isMultiSelect = isMultiSelect,
                        isChecked = isChecked,
                        onCitySelected = onCitySelected,
                        onCityToggled = onCityToggled
                    )
                }
            }

            // 在线搜索触发表/结果区块
            if (searchQuery.trim().length >= 2) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!hasSearchedOnline && !isSearchingOnline) {
                        Button(
                            onClick = { performOnlineSearch() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.search_online, searchQuery.trim()))
                        }
                    } else if (isSearchingOnline) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.searching_online),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (onlineResults.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.online_cities_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp)
                        )
                    }

                    itemsIndexed(
                        onlineResults,
                        key = { index, item -> "online_${item.displayLocationName}_${item.zoneId.id}_$index" }
                    ) { _, item ->
                        CityResultItem(
                            item = item,
                            isMultiSelect = isMultiSelect,
                            isChecked = isChecked,
                            onCitySelected = onCitySelected,
                            onCityToggled = onCityToggled
                        )
                    }
                } else if (hasSearchedOnline && localResults.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_cities_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityResultItem(
    item: CitySearchResult,
    isMultiSelect: Boolean,
    isChecked: (@Composable (item: CitySearchResult) -> Boolean)?,
    onCitySelected: ((CitySearchResult) -> Unit)?,
    onCityToggled: ((CitySearchResult, Boolean) -> Unit)?
) {
    val checked = isChecked?.invoke(item) ?: false

    ListItem(
        modifier = Modifier.clickable {
            if (!isMultiSelect) {
                onCitySelected?.invoke(item)
            } else {
                onCityToggled?.invoke(item, !checked)
            }
        },
        headlineContent = {
            Text(
                text = item.displayLocationName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                text = item.displayTimeZoneName + " (${item.zoneId.id})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isMultiSelect) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { newCheckedState ->
                        onCityToggled?.invoke(item, newCheckedState)
                    }
                )
            }
        }
    )
}
