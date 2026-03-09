package com.music.app.ui.equalizer

import com.music.app.data.remote.EqualizerSettings

val EqualizerBandsHz = listOf(100, 200, 400, 600, 1000, 3000, 6000, 12000, 14000, 16000)

val EqualizerBandLabels = listOf("100Hz", "200Hz", "400Hz", "600Hz", "1KHz", "3KHz", "6KHz", "12KHz", "14KHz", "16KHz")

enum class EqualizerPreset(val code: String, val displayName: String, val gains: List<Float>) {
    OFF("OFF", "关闭", List(10) { 0f }),
    // 专业推荐预设
    HIFI_AUDIOPHILE("HIFI_AUDIOPHILE", "HiFi发烧友", listOf(+1f, +1f, 0f, -1f, 0f, +1f, +2f, +2f, +3f, +3f)),
    LIVE_PERFORMANCE("LIVE_PERFORMANCE", "现场演出", listOf(+4f, +3f, -2f, 0f, +1f, +2f, +3f, +4f, +5f, +4f)),
    VOCAL_FOCUS("VOCAL_FOCUS", "人声特写", listOf(-1f, 0f, +2f, +3f, +4f, +3f, +1f, -1f, -2f, -2f)),
    // 原有预设
    PERFECT_BASS("PERFECT_BASS", "完美低音", listOf(+6f, +4f, -5f, +2f, +3f, +4f, +4f, +5f, +5f, +6f)),
    ROCK("ROCK", "极致摇滚", listOf(+6f, +4f, 0f, -2f, -6f, +1f, +4f, +6f, +7f, +9f)),
    VOCAL("VOCAL", "最毒人声", listOf(+4f, 0f, +1f, +2f, +3f, +4f, +5f, +4f, +3f, +3f)),
    CUSTOM("CUSTOM", "自定义", List(10) { 0f });

    companion object {
        fun fromCode(code: String?): EqualizerPreset = values().firstOrNull { it.code == code } ?: OFF
    }
}

fun EqualizerPreset.toSettings(masterGainDb: Float = 0f): EqualizerSettings {
    val isOff = this == EqualizerPreset.OFF
    return EqualizerSettings(
        enabled = !isOff,
        preset = code,
        masterGainDb = masterGainDb,
        bandGainsDb = gains
    )
}

fun sanitizeBandGains(input: List<Float>): List<Float> {
    val defaults = List(10) { 0f }
    if (input.size != 10) return defaults
    return input.map { it.coerceIn(-12f, 12f) }
}

