package com.musicflow.app.utils

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Software-based audio processor for bass boost and stereo widening.
 * Always active — passes audio through unchanged when no effects are needed.
 * When effects are enabled, processes PCM samples directly in ExoPlayer's pipeline.
 */
class SoftwareEqualizerProcessor : BaseAudioProcessor() {

    private var sampleRate = 44100
    private var channelCount = 2

    private var bassStrength = 0f
    private var prevLeft = 0f
    private var prevRight = 0f
    private var virtualizerStrength = 0f
    private var normalizationGain = 1f

    @Volatile
    var isEnabled = false
        private set

    fun setBassStrength(strength: Short) {
        bassStrength = strength.toInt().coerceIn(0, 1000) / 1000f
        updateActive()
    }

    fun setVirtualizerStrength(strength: Short) {
        virtualizerStrength = strength.toInt().coerceIn(0, 1000) / 1000f
        updateActive()
    }

    fun setNormalizationGain(gain: Float) {
        normalizationGain = gain
        updateActive()
    }

    private fun updateActive() {
        isEnabled = bassStrength > 0f || virtualizerStrength > 0f || normalizationGain != 1f
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRate = inputAudioFormat.sampleRate
        channelCount = inputAudioFormat.channelCount
        // Always return the same format — processor is always active
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val size = inputBuffer.remaining()

        if (!isEnabled) {
            // Passthrough — copy input directly to output
            val outBuf = replaceOutputBuffer(size)
            outBuf.put(inputBuffer)
            outBuf.flip()
            return
        }

        // Read input samples
        val inputOrder = inputBuffer.order()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val inputPos = inputBuffer.position()

        val frameSize = channelCount * 2
        val frameCount = size / frameSize
        if (frameCount == 0) {
            inputBuffer.order(inputOrder)
            return
        }

        val samples = ShortArray(frameCount * channelCount)
        inputBuffer.asShortBuffer().get(samples)
        inputBuffer.position(inputPos + size)
        inputBuffer.order(inputOrder)

        val cutoffCoeff = computeCutoffCoeff()

        var i = 0
        while (i < samples.size) {
            var left = samples[i].toFloat()
            var right = if (i + 1 < samples.size) samples[i + 1].toFloat() else left

            if (bassStrength > 0f) {
                val bassL = prevLeft + cutoffCoeff * (left - prevLeft)
                val bassR = prevRight + cutoffCoeff * (right - prevRight)
                prevLeft = bassL
                prevRight = bassR
                left += (bassL - left) * bassStrength * 1.5f
                right += (bassR - right) * bassStrength * 1.5f
            } else {
                prevLeft = left
                prevRight = right
            }

            if (virtualizerStrength > 0f && channelCount >= 2) {
                val mid = (left + right) * 0.5f
                val side = (left - right) * 0.5f
                val width = 1f + virtualizerStrength * 0.8f
                left = mid + side * width
                right = mid - side * width
            }

            left *= normalizationGain
            right *= normalizationGain

            samples[i] = left.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            if (i + 1 < samples.size) {
                samples[i + 1] = right.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            i += channelCount
        }

        val outBuf = replaceOutputBuffer(samples.size * 2)
        outBuf.order(ByteOrder.LITTLE_ENDIAN)
        outBuf.asShortBuffer().put(samples)
        outBuf.flip()
    }

    override fun onFlush() {
        prevLeft = 0f
        prevRight = 0f
    }

    override fun onReset() {
        prevLeft = 0f
        prevRight = 0f
        bassStrength = 0f
        virtualizerStrength = 0f
        normalizationGain = 1f
        isEnabled = false
    }

    private fun computeCutoffCoeff(): Float {
        val cutoffFreq = 150f
        val dt = 1f / sampleRate.toFloat()
        val rc = 1f / (2f * Math.PI.toFloat() * cutoffFreq)
        return dt / (rc + dt)
    }
}
