package com.music.app.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.app.ui.MusicViewModel
import com.music.app.ui.component.EnhancedVerticalSlider
import com.music.app.ui.equalizer.EqualizerBandLabels
import com.music.app.ui.equalizer.EqualizerBandsHz
import com.music.app.ui.equalizer.EqualizerPreset

@Composable
fun EqualizerDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val eq = userSettings.equalizer
    val themeColor = userSettings.themeColor
    val presets = listOf(
        EqualizerPreset.OFF,
        EqualizerPreset.CUSTOM,
        // 专业推荐预设
        EqualizerPreset.HIFI_AUDIOPHILE,
        EqualizerPreset.LIVE_PERFORMANCE,
        EqualizerPreset.VOCAL_FOCUS,
        // 经典预设
        EqualizerPreset.PERFECT_BASS,
        EqualizerPreset.ROCK,
        EqualizerPreset.VOCAL
    )
    
    // 退出时保存设置
    val handleDismiss = {
        viewModel.saveEqualizerSettingsToServer()
        onDismiss()
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0A0A)
        ) {
            // 背景渐变效果
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0A0A),
                                Color(0xFF121212),
                                Color(0xFF0A0A0A)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, top = 8.dp, bottom = 16.dp)
            ) {
                // 使用 Column 进行垂直布局，每个模块独占一行
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 标题栏
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "效果器",
                                    color = themeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            IconButton(
                                onClick = handleDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .border(
                                        BorderStroke(1.dp, themeColor),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = themeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 启用开关
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "启用效果器",
                                color = themeColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Switch(
                                checked = eq.enabled,
                                onCheckedChange = { viewModel.toggleEqualizerEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = themeColor,
                                    checkedTrackColor = themeColor.copy(alpha = 0.3f),
                                    uncheckedThumbColor = Color(0xFF666666),
                                    uncheckedTrackColor = Color(0xFF333333)
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }

                    // 预设模式 - 横向滚动
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                "预设模式",
                                color = themeColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(presets) { preset ->
                                    val selected = eq.preset == preset.code
                                    Button(
                                        onClick = { viewModel.setEqualizerPreset(preset) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selected) themeColor else Color(0xFF2A2A2A),
                                            contentColor = if (selected) Color.Black else themeColor
                                        ),
                                        border = if (selected) null else BorderStroke(1.dp, themeColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = preset.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 总增益控制
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "总增益",
                                    color = themeColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "${String.format("%.1f", eq.masterGainDb)} dB",
                                    color = themeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Slider(
                                value = eq.masterGainDb,
                                onValueChange = { viewModel.updateEqualizerMasterGain(it) },
                                valueRange = -12f..12f,
                                enabled = eq.enabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = themeColor,
                                    activeTrackColor = themeColor,
                                    inactiveTrackColor = Color(0xFF333333)
                                )
                            )
                        }
                    }

                    // 声道平衡
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "声道平衡",
                                    color = themeColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (eq.stereoBalance != 0f) {
                                    androidx.compose.material3.TextButton(
                                        onClick = { viewModel.updateStereoBalance(0f) },
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            "居中",
                                            color = themeColor.copy(alpha = 0.8f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // 左中右标签
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "左",
                                    color = if (eq.stereoBalance < -0.05f) themeColor else Color(0xFF888888),
                                    fontSize = 11.sp
                                )
                                Text(
                                    when {
                                        eq.stereoBalance < -0.9f -> "纯左"
                                        eq.stereoBalance > 0.9f -> "纯右"
                                        eq.stereoBalance < -0.05f -> "L${String.format("%.0f", -eq.stereoBalance * 100)}"
                                        eq.stereoBalance > 0.05f -> "R${String.format("%.0f", eq.stereoBalance * 100)}"
                                        else -> "居中"
                                    },
                                    color = themeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "右",
                                    color = if (eq.stereoBalance > 0.05f) themeColor else Color(0xFF888888),
                                    fontSize = 11.sp
                                )
                            }

                            // 声道平衡滑块 - 左右不同颜色
                            Box(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 左侧轨道（蓝色）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(30.dp)
                                        .align(Alignment.CenterStart)
                                        .background(
                                            Color(0xFF1E90FF).copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp)
                                        )
                                )
                                
                                // 右侧轨道（红色）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(30.dp)
                                        .align(Alignment.CenterEnd)
                                        .background(
                                            Color(0xFFFF4757).copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                                        )
                                )
                                
                                // 滑块
                                Slider(
                                    value = eq.stereoBalance,
                                    onValueChange = { viewModel.updateStereoBalance(it) },
                                    valueRange = -1f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = themeColor,
                                        activeTrackColor = Color.Transparent, // 隐藏默认轨道
                                        inactiveTrackColor = Color.Transparent
                                    ),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }
                    }

                    // 十段均衡器 - 适中高度
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp), // 固定高度，避免占据过多空间
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "频段调节",
                                color = themeColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // 十段均衡器容器 - 适中高度
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        Color(0xFF222222),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        BorderStroke(1.dp, themeColor.copy(alpha = 0.2f)),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                            ) {
                                // 水平排列的频段控制
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    EqualizerBandLabels.forEachIndexed { index, label ->
                                        val value = eq.bandGainsDb.getOrElse(index) { 0f }

                                        // 每个频段的竖直控制单元
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // 数值显示
                                            Text(
                                                "${String.format("%+.0f", value)}",
                                                color = when {
                                                    value > 6f -> Color(0xFFFF4757)
                                                    value > 3f -> Color(0xFFFFA502)
                                                    value < -6f -> Color(0xFF2ED573)
                                                    value < -3f -> Color(0xFF1E90FF)
                                                    else -> themeColor
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )

                                            // 竖直滑动条容器 - 适中高度
                                            Box(
                                                modifier = Modifier
                                                    .width(32.dp)
                                                    .fillMaxHeight(0.75f)
                                                    .background(
                                                        Color(0xFF2A2A2A),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                                    .border(
                                                        BorderStroke(
                                                            2.dp,
                                                            themeColor.copy(alpha = 0.6f)
                                                        ),
                                                        shape = RoundedCornerShape(16.dp)
                                                    )
                                            ) {
                                                // 增强版竖直滑块
                                                EnhancedVerticalSlider(
                                                    value = value,
                                                    onValueChange = { viewModel.updateEqualizerBand(index, it) },
                                                    valueRange = -12f..12f,
                                                    enabled = eq.enabled,
                                                    trackColor = themeColor,
                                                    thumbColor = themeColor,
                                                    trackHeight = 120.dp
                                                )
                                            }

                                            // 频段标签
                                            Text(
                                                label,
                                                color = Color(0xFFDDDDDD),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 8.sp,  // 减小字体避免换行
                                                maxLines = 1
                                            )

                                            // 频段指示灯
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .size(6.dp)
                                                    .background(
                                                        themeColor,
                                                        shape = RoundedCornerShape(3.dp)
                                                    )
                                                    .alpha(if (eq.enabled) 1f else 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 升降调（独立模块）
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        border = BorderStroke(1.dp, themeColor.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "升降调（半音）",
                                    color = themeColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                val semitones = eq.pitchShiftSemitones
                                val display = when {
                                    semitones > 0 -> "+$semitones"
                                    semitones < 0 -> semitones.toString()
                                    else -> "0"
                                }
                                Text(
                                    "$display 半音",
                                    color = themeColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Slider(
                                value = eq.pitchShiftSemitones.toFloat(),
                                onValueChange = { viewModel.updatePitchShift(it.toInt()) },
                                valueRange = -12f..12f,
                                steps = 24,
                                enabled = eq.enabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = themeColor,
                                    activeTrackColor = themeColor,
                                    inactiveTrackColor = Color(0xFF333333)
                                )
                            )

                            Text(
                                text = "此配置独立于上面的所有模块，仅在开启效果器后生效，不会改变音量和频段等其他设置。",
                                color = Color(0xFF888888),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
