package com.crossware.crossclock.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossware.crossclock.R
import com.crossware.crossclock.util.CitySearchResult
import com.crossware.crossclock.util.CitySearchService
import com.crossware.crossclock.util.WorldClockUtils
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * 添加世界城市/混合模式搜索组件（全面支持语种感知的本地预置城市与在线 Geocoding 检索）。
 */
@Composable
fun CitySearchContent(
    modifier: Modifier = Modifier,
    onCloseSheet: (() -> Unit)? = null,
    onCitySelected: ((CitySearchResult) -> Unit)? = null,
    isMultiSelect: Boolean = false,
    isChecked: (@Composable (item: CitySearchResult) -> Boolean)? = null,
    onCityToggled: ((CitySearchResult, Boolean) -> Unit)? = null
) {
    val locale = LocalConfiguration.current.locales[0]

    var searchQuery by remember { mutableStateOf("") }
    var localResults by remember { mutableStateOf(CitySearchService.searchLocal("", locale)) }
    var onlineResults by remember { mutableStateOf<List<CitySearchResult>>(emptyList()) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var hasSearchedOnline by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchQuery, locale) {
        localResults = CitySearchService.searchLocal(searchQuery, locale)
        hasSearchedOnline = false
        onlineResults = emptyList()
    }

    fun performOnlineSearch() {
        if (searchQuery.trim().length < 2 || isSearchingOnline) return
        coroutineScope.launch {
            isSearchingOnline = true
            val results = CitySearchService.searchOnline(searchQuery, locale)
            onlineResults = results
            isSearchingOnline = false
            hasSearchedOnline = true
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Sheet 顶栏：标题 + 右侧关闭 X 按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "添加世界城市",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (onCloseSheet != null) {
                IconButton(onClick = onCloseSheet) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 搜索框（圆角胶囊外观）
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
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 本地搜索结果区块
            if (localResults.isNotEmpty()) {
                itemsIndexed(
                    localResults,
                    key = { index, item -> "local_${item.cityName}_${item.zoneId.id}_$index" }
                ) { _, item ->
                    CityResultCardItem(
                        item = item,
                        isMultiSelect = isMultiSelect,
                        isChecked = isChecked,
                        onCitySelected = onCitySelected,
                        onCityToggled = onCityToggled
                    )
                }
            }

            // 在线搜索触发/结果区块
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
                    itemsIndexed(
                        onlineResults,
                        key = { index, item -> "online_${item.displayLocationName}_${item.zoneId.id}_$index" }
                    ) { _, item ->
                        CityResultCardItem(
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

/**
 * 高保真搜索结果卡片组件。
 */
@Composable
private fun CityResultCardItem(
    item: CitySearchResult,
    isMultiSelect: Boolean,
    isChecked: (@Composable (item: CitySearchResult) -> Boolean)?,
    onCitySelected: ((CitySearchResult) -> Unit)?,
    onCityToggled: ((CitySearchResult, Boolean) -> Unit)?
) {
    val checked = isChecked?.invoke(item) ?: false
    val locale = LocalConfiguration.current.locales[0]

    val nowZdt = ZonedDateTime.now(item.zoneId)
    val timeStr = nowZdt.format(DateTimeFormatter.ofPattern("HH:mm", locale))

    val offsetStr = WorldClockUtils.getTimeZoneOffsetString(item.zoneId)
    val diffStr = WorldClockUtils.getTimeDifferenceString(item.zoneId)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isMultiSelect) {
                    onCitySelected?.invoke(item)
                } else {
                    onCityToggled?.invoke(item, !checked)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 左侧状态勾选圈（已加入主列表显示绿色 ✔，未加入显示灰色空心圈 ◯）
                Surface(
                    shape = CircleShape,
                    color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (checked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已添加",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    // 城市全名与国家（已根据 Locale 自动多语种本地化）
                    Text(
                        text = item.displayLocationName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // 时区 Offset + 与本地时间差
                    Text(
                        text = "$offsetStr · 比当前$diffStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 右侧时间与动作状态字样
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = timeStr,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (checked) "已加入主列表" else "点击添加",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
