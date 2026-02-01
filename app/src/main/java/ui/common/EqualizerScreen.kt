package com.example.equalizer2.ui.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.common.EqualizerPreset
import com.example.equalizer2.ui.theme.*
import domain.usecase.EqualizerViewModel
import ui.common.AmplitudeVisualizer
import ui.common.EqualizerControl
import ui.common.PlaybackControl
import ui.common.PresetSelector
import ui.common.VolumeControl
import ui.common.WaveformVisualizer
import kotlin.math.sin
import kotlin.math.PI


@Composable
fun EqualizerScreen(viewModel: EqualizerViewModel) {

    val equalizerState by viewModel.equalizerState.collectAsState()
    val visualizerData by viewModel.visualizerData.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val signalState by viewModel.signalState.collectAsState()
    val volume by viewModel.volume.collectAsState()
    var isGeneratorEnabled by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.playUserAudio(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "UrBeat",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Button(
                onClick = { audioPicker.launch(arrayOf("audio/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Elegir canción", color = Color.White)
            }

            PlaybackControl(
                isPlaying = isPlaying,
                onPlayPauseClick = { viewModel.togglePlayback() }
            )

            // 🟢 Visualizador principal: audio o generador
            WaveformVisualizer(
                waveform = if (isGeneratorEnabled) {
                    // Generador: onda senoidal simple
                    FloatArray(256) { i ->
                        signalState.amplitude * sin(2 * PI * signalState.frequency * i / 256).toFloat()
                    }
                } else visualizerData.waveform,
                isAnimating = isPlaying || isGeneratorEnabled
            )

            AmplitudeVisualizer(
                amplitudes = visualizerData.amplitudes
            )

            VolumeControl(
                volume = volume,
                onVolumeChange = { newVolume ->
                    viewModel.setVolume(newVolume)
                }
            )

            // 🎚️ Preset selector
            var selectedPreset by remember { mutableStateOf(EqualizerPreset.FLAT) }
            PresetSelector(
                selectedPreset = selectedPreset,
                onPresetSelected = {
                    selectedPreset = it
                    viewModel.applyPreset(it)
                }
            )

            if (equalizerState.bands.isNotEmpty()) {
                EqualizerControl(
                    bands = equalizerState.bands,
                    onBandChanged = viewModel::setBandLevel
                )
            }

            Spacer(modifier = Modifier.height(16.dp))


        }
    }
}
