package com.music.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 动态颜色方案生成器
 * 根据用户选择的基础色自动生成完整的Material Design 3配色方案
 */
object DynamicColorScheme {
    
    /**
     * 根据基础色生成深色主题配色方案
     */
    fun generateDarkColorScheme(primaryColor: Color): ColorScheme {
        val baseHsl = primaryColor.toHsl()
        
        // 主色调保持不变
        val primary = primaryColor
        
        // 计算onPrimary颜色（确保足够的对比度）
        val onPrimary = calculateContrastColor(primary, Color.White, Color.Black)
        
        // 计算背景色（基于主色但更暗）
        val background = adjustLightness(baseHsl, 0.06f)
        
        // 计算onBackground颜色
        val onBackground = calculateContrastColor(background, Color.White, Color.Black)
        
        // 计算surface颜色
        val surface = adjustLightness(baseHsl, 0.11f)
        
        // 计算onSurface颜色
        val onSurface = calculateContrastColor(surface, Color.White, Color.Black)
        
        // 计算secondary颜色（主色的变体）
        val secondaryHsl = floatArrayOf(
            baseHsl[0], // 保持色相
            max(0.1f, baseHsl[1] * 0.8f), // 降低饱和度
            min(0.9f, baseHsl[2] * 1.2f)  // 提高亮度
        )
        val secondary = Color.hsl(secondaryHsl[0], secondaryHsl[1], secondaryHsl[2])
        
        val onSecondary = calculateContrastColor(secondary, Color.White, Color.Black)
        
        // 计算error颜色（固定为红色系）
        val error = Color(0xFFBA1A1A)
        val onError = Color.White
        
        return darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = adjustLightness(baseHsl, 0.3f),
            onPrimaryContainer = adjustLightness(baseHsl, 0.9f),
            inversePrimary = adjustLightness(baseHsl, 0.8f),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = adjustLightness(secondary.toHsl(), 0.2f),
            onSecondaryContainer = adjustLightness(secondary.toHsl(), 0.9f),
            tertiary = adjustHue(baseHsl, 60f),
            onTertiary = Color.White,
            tertiaryContainer = adjustLightness(adjustHue(baseHsl, 60f).toHsl(), 0.3f),
            onTertiaryContainer = adjustLightness(adjustHue(baseHsl, 60f).toHsl(), 0.9f),
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = adjustLightness(baseHsl, 0.15f),
            onSurfaceVariant = adjustLightness(baseHsl, 0.7f),
            surfaceTint = primary,
            inverseSurface = adjustLightness(baseHsl, 0.9f),
            inverseOnSurface = adjustLightness(baseHsl, 0.2f),
            error = error,
            onError = onError,
            errorContainer = adjustLightness(error.toHsl(), 0.3f),
            onErrorContainer = adjustLightness(error.toHsl(), 0.9f),
            outline = adjustLightness(baseHsl, 0.4f),
            outlineVariant = adjustLightness(baseHsl, 0.2f),
            scrim = Color.Black.copy(alpha = 0.32f)
        )
    }
    
    /**
     * 根据基础色生成浅色主题配色方案
     */
    fun generateLightColorScheme(primaryColor: Color): ColorScheme {
        val baseHsl = primaryColor.toHsl()
        
        // 主色调保持不变
        val primary = primaryColor
        
        // 计算onPrimary颜色
        val onPrimary = calculateContrastColor(primary, Color.White, Color.Black)
        
        // 计算背景色（较亮）
        val background = Color(0xFFFCFCFC)
        
        // 计算onBackground颜色
        val onBackground = Color(0xFF1C1B1B)
        
        // 计算surface颜色
        val surface = Color.White
        
        // 计算onSurface颜色
        val onSurface = Color(0xFF1C1B1B)
        
        // 计算secondary颜色
        val secondaryHsl = floatArrayOf(
            baseHsl[0],
            max(0.1f, baseHsl[1] * 0.8f),
            min(0.9f, baseHsl[2] * 0.8f)
        )
        val secondary = Color.hsl(secondaryHsl[0], secondaryHsl[1], secondaryHsl[2])
        
        val onSecondary = calculateContrastColor(secondary, Color.White, Color.Black)
        
        // 计算error颜色
        val error = Color(0xFFBA1A1A)
        val onError = Color.White
        
        return lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = adjustLightness(baseHsl, 0.9f),
            onPrimaryContainer = adjustLightness(baseHsl, 0.1f),
            inversePrimary = adjustLightness(baseHsl, 0.2f),
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = adjustLightness(secondary.toHsl(), 0.9f),
            onSecondaryContainer = adjustLightness(secondary.toHsl(), 0.1f),
            tertiary = adjustHue(baseHsl, 60f),
            onTertiary = Color.Black,
            tertiaryContainer = adjustLightness(adjustHue(baseHsl, 60f).toHsl(), 0.9f),
            onTertiaryContainer = adjustLightness(adjustHue(baseHsl, 60f).toHsl(), 0.1f),
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = adjustLightness(baseHsl, 0.9f),
            onSurfaceVariant = adjustLightness(baseHsl, 0.3f),
            surfaceTint = primary,
            inverseSurface = Color(0xFF303030),
            inverseOnSurface = Color.White,
            error = error,
            onError = onError,
            errorContainer = adjustLightness(error.toHsl(), 0.9f),
            onErrorContainer = adjustLightness(error.toHsl(), 0.1f),
            outline = adjustLightness(baseHsl, 0.5f),
            outlineVariant = adjustLightness(baseHsl, 0.8f),
            scrim = Color.Black.copy(alpha = 0.32f)
        )
    }
    
    /**
     * 计算颜色的相对亮度
     */
    private fun Color.relativeLuminance(): Float {
        val rgb = this.toArgb()
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        
        val rLinear = if (r <= 0.03928f) r / 12.92f else ((r + 0.055f) / 1.055f).pow(2.4f)
        val gLinear = if (g <= 0.03928f) g / 12.92f else ((g + 0.055f) / 1.055f).pow(2.4f)
        val bLinear = if (b <= 0.03928f) b / 12.92f else ((b + 0.055f) / 1.055f).pow(2.4f)
        
        return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
    }
    
    /**
     * 计算具有足够对比度的颜色
     */
    private fun calculateContrastColor(
        backgroundColor: Color,
        lightCandidate: Color,
        darkCandidate: Color
    ): Color {
        val bgLuminance = backgroundColor.relativeLuminance()
        val lightContrast = contrastRatio(lightCandidate, backgroundColor)
        val darkContrast = contrastRatio(darkCandidate, backgroundColor)
        
        // Material Design要求最小对比度为4.5:1用于正文
        return if (lightContrast >= 4.5f || lightContrast >= darkContrast) {
            lightCandidate
        } else {
            darkCandidate
        }
    }
    
    /**
     * 计算两个颜色之间的对比度比率
     */
    private fun contrastRatio(foreground: Color, background: Color): Float {
        val fgLuminance = foreground.relativeLuminance()
        val bgLuminance = background.relativeLuminance()
        
        val lighter = maxOf(fgLuminance, bgLuminance)
        val darker = minOf(fgLuminance, bgLuminance)
        
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    
    /**
     * 调整颜色的亮度
     */
    private fun adjustLightness(hsl: FloatArray, targetLightness: Float): Color {
        val adjustedHsl = hsl.copyOf()
        adjustedHsl[2] = targetLightness.coerceIn(0f, 1f)
        return Color.hsl(adjustedHsl[0], adjustedHsl[1], adjustedHsl[2])
    }
    
    /**
     * 调整颜色的色相
     */
    private fun adjustHue(hsl: FloatArray, hueShift: Float): Color {
        val adjustedHsl = hsl.copyOf()
        adjustedHsl[0] = (adjustedHsl[0] + hueShift) % 360f
        return Color.hsl(adjustedHsl[0], adjustedHsl[1], adjustedHsl[2])
    }
    
    /**
     * 将Color转换为HSL数组
     */
    private fun Color.toHsl(): FloatArray {
        val rgb = this.toArgb()
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr 8) and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        
        val l = (max + min) / 2f
        
        if (delta == 0f) {
            return floatArrayOf(0f, 0f, l)
        }
        
        val s = if (l < 0.5f) {
            delta / (max + min)
        } else {
            delta / (2f - max - min)
        }
        
        val h = when (max) {
            r -> (g - b) / delta + (if (g < b) 6f else 0f)
            g -> (b - r) / delta + 2f
            else -> (r - g) / delta + 4f
        } * 60f
        
        return floatArrayOf(h, s, l)
    }
    
    /**
     * 预定义的主题颜色选项
     */
    val predefinedColors = mapOf(
        "经典红" to Color(0xFFE53935),
        "活力橙" to Color(0xFFFF6D00),
        "阳光黄" to Color(0xFFFFD600),
        "清新绿" to Color(0xFF00C853),
        "海洋蓝" to Color(0xFF2979FF),
        "优雅紫" to Color(0xFFAA00FF),
        "浪漫粉" to Color(0xFFFF4081),
        "薄荷绿" to Color(0xFF1DE9B6),
        "珊瑚橙" to Color(0xFFFF6E40),
        "宝石蓝" to Color(0xFF01579B),
        "薰衣草紫" to Color(0xFF7E57C2),
        "玫瑰金" to Color(0xFFEC407A),
        "翡翠绿" to Color(0xFF00BFA5),
        "琥珀橙" to Color(0xFFFFAB00),
        "深海蓝" to Color(0xFF0277BD),
        "樱花粉" to Color(0xFFF50057)
    )
}