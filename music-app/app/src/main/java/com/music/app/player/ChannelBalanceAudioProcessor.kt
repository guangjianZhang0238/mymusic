package com.music.app.player

import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * 左右声道音量平衡处理器（继承 BaseAudioProcessor）
 * balance: -1f = 纯左声道, 0f = 居中（默认，不激活处理器）, 1f = 纯右声道
 */
class ChannelBalanceAudioProcessor : BaseAudioProcessor() {

    @Volatile
    var balance: Float = 0f
        set(value) {
            field = value.coerceIn(-1f, 1f)
        }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        // 仅支持立体声（2声道）
        if (inputAudioFormat.channelCount != 2) {
            return AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun isActive(): Boolean {
        return super.isActive() && balance != 0f
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // 每帧 = 左声道 short（2字节）+ 右声道 short（2字节）= 4字节
        val frameCount = remaining / 4
        val outputBuf = replaceOutputBuffer(frameCount * 4)

        val bal = balance
        // 左声道增益：居中=1.0，全右=0.0，全左=1.0（左声道不减）
        // 实际逻辑：往右拉，左声道音量降低；往左拉，右声道音量降低
        val leftGain = if (bal <= 0f) 1f else (1f - bal)
        val rightGain = if (bal >= 0f) 1f else (1f + bal)

        repeat(frameCount) {
            val leftSample = inputBuffer.short.toInt()
            val rightSample = inputBuffer.short.toInt()

            val newLeft = (leftSample * leftGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            val newRight = (rightSample * rightGain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())

            outputBuf.putShort(newLeft.toShort())
            outputBuf.putShort(newRight.toShort())
        }

        outputBuf.flip()
    }
}
