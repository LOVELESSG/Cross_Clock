package com.crossware.crossclock.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.crossware.crossclock.R
import com.crossware.crossclock.data.timer.TimerStep

/**
 * 将十六进制颜色字符串转换为 Compose Color。
 */
fun parseHexColor(colorHex: String, defaultColor: Color = Color(0xFF3F51B5)): Color {
    return try {
        val cleanHex = colorHex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (cleanHex.length == 8) {
            Color(colorInt)
        } else {
            defaultColor
        }
    } catch (e: Exception) {
        defaultColor
    }
}

/**
 * 格式化秒数为 HH:MM:SS 或 MM:SS 文本。
 */
fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * 折叠形态下展示的时间流导轨。
 * 导轨展示该计时器串联了多少单独计时器，每个计时器按时间比例占据导轨长度。
 */
@Composable
fun TimeRailBar(
    steps: List<TimerStep>,
    modifier: Modifier = Modifier
) {
    val totalSeconds = steps.sumOf { it.durationSeconds }.coerceAtLeast(1L)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        steps.forEachIndexed { index, step ->
            val weight = (step.durationSeconds.toFloat() / totalSeconds.toFloat()).coerceAtLeast(0.01f)
            val stepColor = parseHexColor(step.colorHex)

            Box(
                modifier = Modifier
                    .weight(weight)
                    .height(10.dp)
                    .background(stepColor)
            )

            // 各分隔微小缝隙
            if (index < steps.size - 1) {
                Spacer(modifier = Modifier.width(2.dp).height(10.dp).background(MaterialTheme.colorScheme.surface))
            }
        }
    }
}

/**
 * 展开形态下的时间线展示组件。
 * 以垂直时间线形式细化列出串联的所有单独计时器。
 */
@Composable
fun TimerTimeline(
    steps: List<TimerStep>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        steps.forEachIndexed { index, step ->
            val stepColor = parseHexColor(step.colorHex)
            val isLast = index == steps.size - 1

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                // 时间线圆圈节点
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(stepColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // 步骤名称与时长
                Column(modifier = Modifier.weight(1f)) {
                    val fallbackStepName = stringResource(R.string.step_x, index + 1)
                    Text(
                        text = "${index + 1}. ${step.name.ifBlank { fallbackStepName }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = formatDuration(step.durationSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 节点连接线
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .width(2.dp)
                        .height(12.dp)
                        .background(stepColor.copy(alpha = 0.5f))
                )
            }
        }
    }
}
