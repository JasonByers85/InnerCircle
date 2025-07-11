package com.google.mediapipe.examples.llminference

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// Breathing Session Screen
@Composable
fun BreathingSession(
    program: BreathingProgram,
    onBack: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var isActive by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf("Ready") }
    var timeRemaining by remember { mutableIntStateOf(program.totalDuration) }
    var currentCycleTime by remember { mutableIntStateOf(0) }
    var phaseProgress by remember { mutableFloatStateOf(0f) }
    var phaseCountdown by remember { mutableIntStateOf(0) }
    
    // Audio Manager with full capabilities
    val audioManager = remember { BreathingAudioManager(context) }
    var isAudioReady by remember { mutableStateOf(false) }
    
    // Audio settings using same system as meditation
    var showSettingsDialog by remember { mutableStateOf(false) }
    val settings = remember { MeditationSettings.getInstance(context) }
    
    // Initialize Audio
    LaunchedEffect(Unit) {
        audioManager.initialize {
            isAudioReady = true
        }
    }
    
    // Cleanup audio on dispose
    DisposableEffect(Unit) {
        onDispose {
            audioManager.destroy()
        }
    }
    
    // Audio settings management using meditation settings
    LaunchedEffect(settings) {
        audioManager.setBackgroundSound(settings.getBackgroundSound())
        audioManager.setBackgroundVolume(settings.getVolume())
        audioManager.setBinauralTone(settings.getBinauralTone())
        audioManager.setBinauralVolume(settings.getBinauralVolume())
    }
    
    // Breathing cycle logic
    LaunchedEffect(isActive, isAudioReady) {
        if (isActive && isAudioReady) {
            val totalCycleTime = program.inhaleSeconds + program.holdSeconds + program.exhaleSeconds
            var lastSpokenPhase = ""
            
            while (isActive && timeRemaining > 0) {
                delay(1000) // Update every 1 second for timer
                currentCycleTime += 1000
                timeRemaining = (timeRemaining - 1).coerceAtLeast(0)
                
                val cyclePosition = (currentCycleTime / 1000) % totalCycleTime
                
                when {
                    cyclePosition < program.inhaleSeconds -> {
                        if (currentPhase != "Breathe In") {
                            currentPhase = "Breathe In"
                            if (lastSpokenPhase != currentPhase) {
                                audioManager.speak(program.inhaleMessage)
                                lastSpokenPhase = currentPhase
                            }
                        }
                        phaseProgress = (cyclePosition / program.inhaleSeconds.toFloat()).coerceIn(0f, 1f)
                        phaseCountdown = (program.inhaleSeconds - cyclePosition).coerceAtLeast(1)
                    }
                    cyclePosition < program.inhaleSeconds + program.holdSeconds -> {
                        if (currentPhase != "Hold") {
                            currentPhase = "Hold"
                            if (lastSpokenPhase != currentPhase) {
                                audioManager.speak("Hold your breath")
                                lastSpokenPhase = currentPhase
                            }
                        }
                        phaseProgress = 1f
                        val holdPosition = cyclePosition - program.inhaleSeconds
                        phaseCountdown = (program.holdSeconds - holdPosition).coerceAtLeast(1)
                    }
                    else -> {
                        if (currentPhase != "Breathe Out") {
                            currentPhase = "Breathe Out"
                            if (lastSpokenPhase != currentPhase) {
                                audioManager.speak(program.exhaleMessage)
                                lastSpokenPhase = currentPhase
                            }
                        }
                        val exhaleProgress = (cyclePosition - program.inhaleSeconds - program.holdSeconds) / program.exhaleSeconds.toFloat()
                        phaseProgress = (1f - exhaleProgress).coerceIn(0f, 1f)
                        val exhalePosition = cyclePosition - program.inhaleSeconds - program.holdSeconds
                        phaseCountdown = (program.exhaleSeconds - exhalePosition).coerceAtLeast(1)
                    }
                }
            }
            
            if (timeRemaining <= 0) {
                isActive = false
                currentPhase = "Complete!"
                audioManager.speak("Breathing session complete. Well done!")
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar with program info
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${timeRemaining / 60}:${String.format("%02d", timeRemaining % 60)} remaining",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { showSettingsDialog = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Audio Settings")
            }
            IconButton(onClick = onExit) {
                Icon(Icons.Default.Close, contentDescription = "Exit")
            }
        }
        
        // Program description card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = program.icon,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = program.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${program.inhaleSeconds}s in • ${program.holdSeconds}s hold • ${program.exhaleSeconds}s out",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Audio Settings Dialog (same as meditation)
        if (showSettingsDialog) {
            UnifiedMeditationSettingsDialog(
                settings = settings,
                context = context,
                soundEnabled = true,
                onSoundToggle = { /* Settings handles this */ },
                backgroundSound = settings.getBackgroundSound(),
                onBackgroundSoundChange = { sound -> 
                    settings.setBackgroundSound(sound)
                    audioManager.setBackgroundSound(sound)
                },
                binauralEnabled = settings.isBinauralEnabled(),
                onBinauralToggle = {
                    val newEnabled = !settings.isBinauralEnabled()
                    settings.setBinauralEnabled(newEnabled)
                    if (!newEnabled) audioManager.setBinauralTone(BinauralTone.NONE)
                },
                binauralTone = settings.getBinauralTone(),
                onBinauralToneChange = { tone ->
                    settings.setBinauralTone(tone)
                    audioManager.setBinauralTone(tone)
                },
                ttsEnabled = settings.isTtsEnabled(),
                onTtsToggle = {
                    val newEnabled = !settings.isTtsEnabled()
                    settings.setTtsEnabled(newEnabled)
                },
                onTtsSpeedChange = { speed ->
                    settings.setTtsSpeed(speed)
                    audioManager.updateTtsSettings()
                },
                onTtsPitchChange = { pitch ->
                    settings.setTtsPitch(pitch)
                    audioManager.updateTtsSettings()
                },
                onTtsVolumeChange = { volume ->
                    settings.setTtsVolume(volume)
                    audioManager.updateTtsSettings()
                },
                onTtsVoiceChange = { voice ->
                    settings.setTtsVoice(voice)
                    audioManager.updateTtsSettings()
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
        
        // Enhanced breathing visualization
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EnhancedBreathingCircle(
                    program = program,
                    isActive = isActive,
                    phase = currentPhase,
                    progress = phaseProgress,
                    countdown = phaseCountdown
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentPhase,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (isActive && currentPhase != "Complete!") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (currentPhase) {
                                "Breathe In" -> program.inhaleMessage
                                "Breathe Out" -> program.exhaleMessage
                                "Hold" -> "Feel the stillness and peace"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }
        
        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    isActive = !isActive
                    if (isActive) {
                        currentCycleTime = 0
                        currentPhase = "Breathe In"
                        if (timeRemaining <= 0) {
                            timeRemaining = program.totalDuration
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isActive) "Pause" else "Start")
            }
            
            OutlinedButton(
                onClick = {
                    isActive = false
                    timeRemaining = program.totalDuration
                    currentPhase = "Ready"
                    currentCycleTime = 0
                    phaseProgress = 0f
                    phaseCountdown = 0
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset")
            }
        }
    }
}

// Enhanced breathing circle with energy visualization
@Composable
fun EnhancedBreathingCircle(
    program: BreathingProgram,
    isActive: Boolean,
    phase: String,
    progress: Float,
    countdown: Int = 0
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive) progress else 0.5f,
        animationSpec = tween(durationMillis = 200, easing = EaseInOutCubic),
        label = "breathing_progress"
    )
    
    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = size.minDimension / 4
        val maxRadius = size.minDimension / 2.2f
        
        // Calculate current radius based on breathing phase
        val currentRadius = when (phase) {
            "Breathe In" -> baseRadius + (maxRadius - baseRadius) * animatedProgress
            "Hold" -> maxRadius
            "Breathe Out" -> maxRadius - (maxRadius - baseRadius) * (1f - animatedProgress)
            else -> baseRadius
        }
        
        // Determine colors based on phase
        val (primaryColor, secondaryColor) = when (phase) {
            "Breathe In" -> program.positiveColor to program.positiveColor.copy(alpha = 0.3f)
            "Breathe Out" -> program.negativeColor to program.negativeColor.copy(alpha = 0.3f)
            "Hold" -> program.positiveColor to program.positiveColor.copy(alpha = 0.5f)
            else -> Color.Gray to Color.Gray.copy(alpha = 0.2f)
        }
        
        // Draw energy particles for enhanced visual effect
        if (isActive) {
            drawEnergyParticles(
                center = center,
                radius = currentRadius,
                phase = phase,
                progress = animatedProgress,
                positiveColor = program.positiveColor,
                negativeColor = program.negativeColor
            )
        }
        
        // Draw main breathing circle with gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.6f),
                    secondaryColor,
                    primaryColor.copy(alpha = 0.1f)
                ),
                center = center,
                radius = currentRadius
            ),
            radius = currentRadius,
            center = center
        )
        
        // Draw outer ring
        drawCircle(
            color = primaryColor.copy(alpha = 0.8f),
            radius = currentRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
        )
        
        // Draw inner core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.8f),
                    primaryColor.copy(alpha = 0.2f)
                ),
                center = center,
                radius = currentRadius * 0.3f
            ),
            radius = currentRadius * 0.3f,
            center = center
        )
    }
        
        // Countdown display overlay
        if (isActive && countdown > 0) {
            Text(
                text = countdown.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// Energy particles visualization
fun DrawScope.drawEnergyParticles(
    center: Offset,
    radius: Float,
    phase: String,
    progress: Float,
    positiveColor: Color,
    negativeColor: Color
) {
    val particleCount = 12
    val particleRadius = 4.dp.toPx()
    
    for (i in 0 until particleCount) {
        val angle = (i * 2 * PI / particleCount).toFloat()
        val particleDistance = when (phase) {
            "Breathe In" -> radius * 0.6f + (radius * 0.4f * progress)
            "Breathe Out" -> radius * 1.2f - (radius * 0.4f * (1f - progress))
            else -> radius * 0.8f
        }
        
        val particleCenter = Offset(
            center.x + cos(angle) * particleDistance,
            center.y + sin(angle) * particleDistance
        )
        
        val particleColor = when (phase) {
            "Breathe In" -> positiveColor.copy(alpha = 0.7f)
            "Breathe Out" -> negativeColor.copy(alpha = 0.7f)
            else -> Color.Gray.copy(alpha = 0.3f)
        }
        
        drawCircle(
            color = particleColor,
            radius = particleRadius,
            center = particleCenter
        )
    }
}

