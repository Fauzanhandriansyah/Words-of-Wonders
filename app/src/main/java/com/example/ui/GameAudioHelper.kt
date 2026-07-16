package com.example.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object GameAudioHelper {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playDragTone() {
        playTone(frequency = 600f, durationMs = 40)
    }

    fun playWordSolvedTone() {
        scope.launch {
            playTone(frequency = 523.25f, durationMs = 100) // C5
            delay(80)
            playTone(frequency = 659.25f, durationMs = 120) // E5
            delay(100)
            playTone(frequency = 783.99f, durationMs = 150) // G5
        }
    }

    fun playLevelCompleteTone() {
        scope.launch {
            val notes = listOf(523.25f, 587.33f, 659.25f, 698.46f, 783.99f, 880.00f, 987.77f, 1046.50f)
            for (note in notes) {
                playTone(frequency = note, durationMs = 90)
                delay(70)
            }
        }
    }

    private fun playTone(frequency: Float, durationMs: Int) {
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (durationMs * sampleRate) / 1000
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)

                for (i in 0 until numSamples) {
                    sample[i] = sin(2 * Math.PI * i / (sampleRate / frequency))
                }

                var idx = 0
                for (dVal in sample) {
                    // Scale to maximum amplitude
                    // Apply linear envelope to avoid clicking/crackling sounds at the end of the sound
                    val fadeOutThreshold = (numSamples * 0.85).toInt()
                    val envelope = if (idx / 2 > fadeOutThreshold) {
                        val remaining = numSamples - (idx / 2)
                        remaining.toDouble() / (numSamples - fadeOutThreshold)
                    } else {
                        1.0
                    }
                    val valShort = (dVal * 15000 * envelope).toInt().toShort() // Moderate volume (15000 / 32767)
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                
                // Delay to prevent premature release of track
                delay(durationMs.toLong() + 50)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
