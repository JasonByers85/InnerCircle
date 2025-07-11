package com.google.mediapipe.examples.llminference

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.*

// Breathing Program Data Classes
data class BreathingProgram(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val inhaleSeconds: Int,
    val holdSeconds: Int,
    val exhaleSeconds: Int,
    val totalDuration: Int, // in seconds
    val inhaleMessage: String,
    val exhaleMessage: String,
    val positiveColor: Color,
    val negativeColor: Color
)

// Predefined Breathing Programs
object BreathingPrograms {
    fun getAll(): List<BreathingProgram> = listOf(
        BreathingProgram(
            id = "quick_calm",
            name = "Quick Calm",
            description = "Fast relief for immediate stress",
            icon = "⚡",
            inhaleSeconds = 3,
            holdSeconds = 1,
            exhaleSeconds = 5,
            totalDuration = 90, // 1.5 minutes
            inhaleMessage = "Breathe in instant calm",
            exhaleMessage = "Release stress quickly",
            positiveColor = Color(0xFF4CAF50), // Calming green
            negativeColor = Color(0xFFFF5722)  // Release orange
        ),
        BreathingProgram(
            id = "minute_reset",
            name = "Minute Reset",
            description = "One-minute breathing reset for busy moments",
            icon = "⏱️",
            inhaleSeconds = 4,
            holdSeconds = 2,
            exhaleSeconds = 4,
            totalDuration = 60, // 1 minute
            inhaleMessage = "Breathe in fresh energy",
            exhaleMessage = "Release tension and reset",
            positiveColor = Color(0xFF00BCD4), // Fresh cyan
            negativeColor = Color(0xFF607D8B)  // Release gray
        ),
        BreathingProgram(
            id = "panic_relief",
            name = "Panic Relief",
            description = "Rapid calming for anxiety and panic attacks",
            icon = "🆘",
            inhaleSeconds = 4,
            holdSeconds = 2,
            exhaleSeconds = 8,
            totalDuration = 180, // 3 minutes
            inhaleMessage = "Breathe in safety and calm",
            exhaleMessage = "Release the panic and fear",
            positiveColor = Color(0xFF4CAF50), // Calming green
            negativeColor = Color(0xFFFF5722)  // Release orange
        ),
        BreathingProgram(
            id = "energize",
            name = "Energize",
            description = "Invigorating breath to boost energy and focus",
            icon = "⚡",
            inhaleSeconds = 5,
            holdSeconds = 3,
            exhaleSeconds = 5,
            totalDuration = 240, // 4 minutes
            inhaleMessage = "Breathe in energy and vitality",
            exhaleMessage = "Release fatigue and sluggishness",
            positiveColor = Color(0xFFFFEB3B), // Energizing yellow
            negativeColor = Color(0xFF795548)  // Release brown
        ),
        BreathingProgram(
            id = "deep_relaxation",
            name = "Deep Relaxation",
            description = "Gentle breathing for stress relief and peace",
            icon = "🧘",
            inhaleSeconds = 6,
            holdSeconds = 4,
            exhaleSeconds = 8,
            totalDuration = 300, // 5 minutes
            inhaleMessage = "Breathe in peace and tranquility",
            exhaleMessage = "Let go of all stress and tension",
            positiveColor = Color(0xFF2196F3), // Peaceful blue
            negativeColor = Color(0xFF9C27B0)  // Release purple
        ),
        BreathingProgram(
            id = "focus_clarity",
            name = "Focus & Clarity",
            description = "Sharp breathing for mental clarity and concentration",
            icon = "🎯",
            inhaleSeconds = 4,
            holdSeconds = 4,
            exhaleSeconds = 6,
            totalDuration = 360, // 6 minutes
            inhaleMessage = "Breathe in clarity and focus",
            exhaleMessage = "Release distractions and mental fog",
            positiveColor = Color(0xFF00BCD4), // Clear cyan
            negativeColor = Color(0xFF607D8B)  // Release gray
        ),
        BreathingProgram(
            id = "sleep_preparation",
            name = "Sleep Preparation (4-7-8)",
            description = "The famous 4-7-8 technique for deeper sleep",
            icon = "🌙",
            inhaleSeconds = 4,
            holdSeconds = 7,
            exhaleSeconds = 8,
            totalDuration = 380, // 6+ minutes (20 cycles)
            inhaleMessage = "Breathe in calm and drowsiness",
            exhaleMessage = "Release the day's worries and restlessness",
            positiveColor = Color(0xFF3F51B5), // Nighttime blue
            negativeColor = Color(0xFF424242)  // Release dark gray
        ),
        BreathingProgram(
            id = "emotional_balance",
            name = "Emotional Balance",
            description = "Stabilizing breath for emotional equilibrium",
            icon = "⚖️",
            inhaleSeconds = 6,
            holdSeconds = 2,
            exhaleSeconds = 6,
            totalDuration = 300, // 5 minutes
            inhaleMessage = "Breathe in balance and stability",
            exhaleMessage = "Release emotional turbulence and reactivity",
            positiveColor = Color(0xFF009688), // Balanced teal
            negativeColor = Color(0xFFE91E63)  // Release pink
        ),
        BreathingProgram(
            id = "box_breathing",
            name = "Box Breathing",
            description = "Navy SEAL technique for performance and focus",
            icon = "⬜",
            inhaleSeconds = 4,
            holdSeconds = 4,
            exhaleSeconds = 4,
            totalDuration = 240, // 4 minutes
            inhaleMessage = "Breathe in strength and focus",
            exhaleMessage = "Release tension and doubt",
            positiveColor = Color(0xFF795548), // Strong brown
            negativeColor = Color(0xFF424242)  // Release dark gray
        )
    )
}

// Enhanced Audio Manager for Breathing Sessions
class BreathingAudioManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val meditationAudioManager = MeditationAudioManager(context)
    private val settings = MeditationSettings.getInstance(context)
    
    fun initialize(onInitialized: () -> Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { tts ->
                    tts.language = Locale.getDefault()
                    // Apply settings from MeditationSettings
                    tts.setSpeechRate(settings.getTtsSpeed())
                    tts.setPitch(settings.getTtsPitch())
                    
                    // Apply saved voice if available
                    val savedVoice = settings.getTtsVoice()
                    if (savedVoice.isNotEmpty()) {
                        tts.voices?.find { it.name == savedVoice }?.let { voice ->
                            tts.voice = voice
                        }
                    }
                }
                isInitialized = true
                onInitialized()
            }
        }
    }
    
    fun speak(text: String) {
        // Check if TTS is enabled in settings before speaking
        if (isInitialized && settings.isTtsEnabled()) {
            tts?.let { tts ->
                // Apply current settings each time
                tts.setSpeechRate(settings.getTtsSpeed())
                tts.setPitch(settings.getTtsPitch())
                
                // Create bundle with volume parameter
                val params = android.os.Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, settings.getTtsVolume())
                }
                
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, null)
            }
        }
    }
    
    fun updateTtsSettings() {
        tts?.let { tts ->
            tts.setSpeechRate(settings.getTtsSpeed())
            tts.setPitch(settings.getTtsPitch())
            
            // Update voice if changed
            val savedVoice = settings.getTtsVoice()
            if (savedVoice.isNotEmpty()) {
                tts.voices?.find { it.name == savedVoice }?.let { voice ->
                    tts.voice = voice
                }
            }
        }
    }
    
    fun setBackgroundSound(sound: BackgroundSound) {
        meditationAudioManager.playBackgroundSound(sound)
    }
    
    fun setBinauralTone(tone: BinauralTone) {
        meditationAudioManager.playBinauralTone(tone)
    }
    
    fun setBackgroundVolume(volume: Float) {
        meditationAudioManager.setVolume(volume)
    }
    
    fun setBinauralVolume(volume: Float) {
        meditationAudioManager.setBinauralVolume(volume)
    }
    
    fun stopAudio() {
        meditationAudioManager.stopBackgroundSound()
        meditationAudioManager.stopBinauralTone()
    }
    
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        meditationAudioManager.release()
    }
}

// Main Breathing Route
@Composable
internal fun BreathingRoute(onBack: () -> Unit) {
    var selectedProgram by remember { mutableStateOf<BreathingProgram?>(null) }
    
    if (selectedProgram == null) {
        BreathingProgramSelection(
            onBack = onBack,
            onProgramSelected = { selectedProgram = it }
        )
    } else {
        BreathingSession(
            program = selectedProgram!!,
            onBack = { selectedProgram = null },
            onExit = onBack
        )
    }
}

// Program Selection Screen
@Composable
fun BreathingProgramSelection(
    onBack: () -> Unit,
    onProgramSelected: (BreathingProgram) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Breathing Programs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🫁 Guided Breathing Exercises",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose a breathing program designed for your current needs. Each session includes voice guidance and visual animations.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(600.dp)
            ) {
                items(BreathingPrograms.getAll()) { program ->
                    BreathingProgramCard(
                        program = program,
                        onClick = { onProgramSelected(program) }
                    )
                }
            }
        }
    }
}

// Program Card Component
@Composable
private fun BreathingProgramCard(
    program: BreathingProgram,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Program icon and indicator
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                program.positiveColor.copy(alpha = 0.3f),
                                program.negativeColor.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = program.icon,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${program.totalDuration / 60} min • ${program.inhaleSeconds}-${program.holdSeconds}-${program.exhaleSeconds} pattern",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}