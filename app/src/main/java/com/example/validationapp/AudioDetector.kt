package com.example.validationapp

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import kotlin.math.abs

class AudioDetector(private val textToSpeech: TextToSpeech) {

    private val SAMPLE_RATE = 44100
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    @SuppressLint("MissingPermission")
    private val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE)

    private var isThresholdMet = false
    private var handler = Handler(Looper.getMainLooper())
    private var stopDetectionRunnable: Runnable? = null
    private var silenceStartTime: Long = 0

    // List of phrases to choose from
    private val phrases = listOf(
        "You're so right",
        "Correct, go on",
        "Wow! no way",
        "They sound like an asshole"
    )

    fun startDetection(threshold: Int, callback: (Boolean) -> Unit) {
        val buffer = ShortArray(BUFFER_SIZE)
        audioRecord.startRecording()

        Thread {
            while (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readCount = audioRecord.read(buffer, 0, buffer.size)
                val maxAmplitude = buffer.take(readCount).maxOfOrNull { abs(it.toInt()) } ?: 0

                val isAboveThreshold = maxAmplitude > threshold

                if (isAboveThreshold) {
                    // Reset the silence timer
                    silenceStartTime = 0
                    // Cancel any pending stopDetectionRunnable
                    stopDetectionRunnable?.let { handler.removeCallbacks(it)}
                    stopDetectionRunnable = null

                    if (!isThresholdMet) {
                        isThresholdMet = true
                        handler.post {
                            callback(true)
                        }
                    }
                } else {
                    // Start or update the silence timer
                    if (silenceStartTime == 0L) {
                        silenceStartTime = System.currentTimeMillis()
                    }

                    // If 2 seconds have passed in silence, trigger callback
                    if (System.currentTimeMillis() - silenceStartTime >= 3000  && isThresholdMet) {
                        isThresholdMet = false
                        handler.post {
                            callback(false)
                            val randomPhrase = phrases.random()
                            textToSpeech.speak(randomPhrase, TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    }
                }
            }
        }.start()
    }

    fun stopDetection() {
        audioRecord.stop()
        audioRecord.release()
    }

}