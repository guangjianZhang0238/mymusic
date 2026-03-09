package com.music.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 增强版垂直滑块组件
 * 支持手势拖动，旋钮与进度条端点重合
 */
@Composable
fun EnhancedVerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    trackColor: Color = Color(0xFF00FFFF),
    thumbColor: Color = Color(0xFF00FFFF),
    modifier: Modifier = Modifier,
    trackHeight: Dp = 120.dp
) {
    val thumbSize = 24.dp
    val thumbRadius = thumbSize / 2
    
    // 计算进度比例 (0.0 到 1.0)
    val progress = remember(value, valueRange) {
        (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    }
    
    // 计算拇指位置：从顶部开始，progress=0时在底部，progress=1时在顶部
    // 轨道中心线位置
    val trackTopY = -trackHeight / 2 + thumbRadius
    val trackBottomY = trackHeight / 2 - thumbRadius
    val thumbOffsetY = remember(progress) {
        trackBottomY - (trackBottomY - trackTopY) * progress
    }

    // 记录容器的实际像素高度，用于拖动计算
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .onSizeChanged { size ->
                containerHeightPx = size.height.toFloat()
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(valueRange) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (containerHeightPx <= 0f) return@detectDragGestures
                            // 向上拖动（dragAmount.y < 0）增加值，向下减少值
                            val delta = -dragAmount.y / containerHeightPx
                            val range = valueRange.endInclusive - valueRange.start
                            val newValue = (value + delta * range).coerceIn(valueRange.start, valueRange.endInclusive)
                            onValueChange(newValue)
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // 轨道背景
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(trackHeight)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            trackColor.copy(alpha = 0.3f),
                            Color(0xFF333333)
                        )
                    ),
                    shape = RoundedCornerShape(3.dp)
                )
        )

        // 活动轨道（从底部向上填充到拇指位置）
        val activeHeight = trackHeight * progress

        Box(
            modifier = Modifier
                .width(6.dp)
                .height(activeHeight)
                .align(Alignment.BottomCenter)
                .background(
                    color = trackColor,
                    shape = RoundedCornerShape(3.dp)
                )
        )

        // 拇指指示器 - 作为进度条的端点，位于活动轨道的顶端
        Box(
            modifier = Modifier
                .size(thumbSize)
                .offset(y = thumbOffsetY)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (enabled) thumbColor else Color(0xFF666666),
                            if (enabled) thumbColor.copy(alpha = 0.7f) else Color(0xFF444444)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    color = if (enabled) Color.White else Color(0xFF888888),
                    shape = CircleShape
                )
        )
    }
}
