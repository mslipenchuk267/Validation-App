package com.example.validationapp

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.validationapp.ui.theme.ValidationAppTheme
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var audioDetector: AudioDetector? = null
    private lateinit var textToSpeech: TextToSpeech


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init text to speech
        textToSpeech = TextToSpeech(this, this)

        enableEdgeToEdge()
        setContent {
            ValidationAppTheme {
                val permissionGranted = remember { mutableStateOf(false) }

                RequestAudioPermission {
                    permissionGranted.value = true
                }
                if (permissionGranted.value) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            audioDetector = AudioDetector(textToSpeech)
                            NoiseDetectionSurface(
                                audioDetector = audioDetector,
                                modifier = Modifier.padding(innerPadding),
                                isPreview = false
                            )
                    }
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
            textToSpeech.setSpeechRate(0.75f)  // Normal speech rate
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioDetector?.stopDetection()

        // Shutdown TextToSpeech when done
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}

@Composable
fun NoiseDetectionSurface(audioDetector: AudioDetector?, modifier: Modifier = Modifier, isPreview: Boolean = false) {
    var isAboveThreshold by remember { mutableStateOf(false) }

    if (isPreview) {
        // Simulate detection for preview
        LaunchedEffect(Unit) {
            while (true) {
                isAboveThreshold = !isAboveThreshold
                delay(8000) // Toggle every second
            }
        }
    } else {
        LaunchedEffect(Unit) {
            audioDetector?.startDetection(750) {
                detected -> isAboveThreshold = detected
            }
        }
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isAboveThreshold) Color.Green else Color.Red),
        contentAlignment = Alignment.Center) {
        
    }
}

@Composable
fun RequestAudioPermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    val permissionState = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                permissionState.value = true
                onPermissionGranted()
            } else {
                // Handle the case where permission was denied.
            }
        }
    )

    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                permissionState.value = true
                onPermissionGranted()
            }
            else -> {
                launcher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ValidationAppTheme {
        NoiseDetectionSurface(audioDetector = null, isPreview = true)
    }
}