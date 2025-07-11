package com.google.mediapipe.examples.llminference

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
internal fun MeditationSessionRoute(
    meditationType: String,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: MeditationSessionViewModel = viewModel(
        factory = MeditationSessionViewModel.getFactory(context, meditationType)
    )
    val settings = remember { MeditationSettings.getInstance(context) }

    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val totalTimeRemaining by viewModel.totalTimeRemaining.collectAsStateWithLifecycle()
    val currentStepIndex by viewModel.currentStepIndex.collectAsStateWithLifecycle()
    val totalSteps by viewModel.totalSteps.collectAsStateWithLifecycle()
    val isGeneratingNextStep by viewModel.isGeneratingNextStep.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val backgroundSound by viewModel.backgroundSound.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val binauralTone by viewModel.binauralTone.collectAsStateWithLifecycle()
    val binauralEnabled by viewModel.binauralEnabled.collectAsStateWithLifecycle()
    val ttsEnabled by viewModel.ttsEnabled.collectAsStateWithLifecycle()

    // Handle completion
    LaunchedEffect(sessionState) {
        if (sessionState == MeditationSessionState.COMPLETED) {
            delay(2000) // Show completion message for 2 seconds
            onComplete()
        }
    }

    MeditationSessionScreen(
        sessionState = sessionState,
        currentStep = currentStep,
        totalTimeRemaining = totalTimeRemaining,
        currentStepIndex = currentStepIndex,
        totalSteps = totalSteps,
        meditationType = meditationType,
        isPlaying = isPlaying,
        backgroundSound = backgroundSound,
        binauralTone = binauralTone,
        soundEnabled = soundEnabled,
        binauralEnabled = binauralEnabled,
        ttsEnabled = ttsEnabled,
        onPlayPause = viewModel::togglePlayPause,
        onStop = { viewModel.stopSession(); onBack() },
        onSoundToggle = viewModel::toggleSound,
        onBinauralToggle = viewModel::toggleBinaural,
        onTtsToggle = viewModel::toggleTts,
        onBackgroundSoundChange = viewModel::setBackgroundSound,
        onBinauralToneChange = viewModel::setBinauralTone,
        onBackgroundVolumeChange = viewModel::setBackgroundVolume,
        onBinauralVolumeChange = viewModel::setBinauralVolume,
        onTtsVolumeChange = viewModel::setTtsVolume,
        onTtsSpeedChange = viewModel::setTtsSpeed,
        onTtsPitchChange = viewModel::setTtsPitch,
        onBack = onBack,
        settings = settings,
        context = context
    )
}
@Composable
fun MeditationSessionScreen(
    sessionState: MeditationSessionState,
    currentStep: MeditationStep,
    totalTimeRemaining: Int,
    currentStepIndex: Int,
    isPlaying: Boolean,
    backgroundSound: BackgroundSound,
    binauralTone: BinauralTone,
    soundEnabled: Boolean,
    binauralEnabled: Boolean,
    ttsEnabled: Boolean,
    totalSteps: Int = 1, // Add total steps parameter
    meditationType: String = "", // Add meditation type for custom detection
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSoundToggle: () -> Unit,
    onBinauralToggle: () -> Unit,
    onTtsToggle: () -> Unit,
    onBackgroundSoundChange: (BackgroundSound) -> Unit,
    onBinauralToneChange: (BinauralTone) -> Unit,
    onBackgroundVolumeChange: ((Float) -> Unit)? = null,
    onBinauralVolumeChange: ((Float) -> Unit)? = null,
    onTtsVolumeChange: ((Float) -> Unit)? = null,
    onTtsSpeedChange: ((Float) -> Unit)? = null,
    onTtsPitchChange: ((Float) -> Unit)? = null,
    onBack: () -> Unit,
    settings: MeditationSettings,
    context: android.content.Context
) {
    var showSoundMenu by remember { mutableStateOf(false) }
    var showBinauralMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar with back button and audio controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Row {
                // TTS toggle
                IconButton(onClick = onTtsToggle) {
                    Icon(
                        if (ttsEnabled) Icons.Default.RecordVoiceOver else Icons.Default.VoiceOverOff,
                        contentDescription = if (ttsEnabled) "Disable Voice" else "Enable Voice",
                        tint = if (ttsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Background sound toggle
                IconButton(onClick = onSoundToggle) {
                    Icon(
                        if (soundEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
                        contentDescription = if (soundEnabled) "Disable Background Sound" else "Enable Background Sound",
                        tint = if (soundEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Binaural tone toggle
                IconButton(onClick = onBinauralToggle) {
                    Icon(
                        if (binauralEnabled) Icons.Default.GraphicEq else Icons.Default.GraphicEq,
                        contentDescription = if (binauralEnabled) "Disable Binaural" else "Enable Binaural",
                        tint = if (binauralEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Unified settings dialog (gear/settings icon)
                IconButton(onClick = { showSoundMenu = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Audio & Meditation Settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Session progress indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Step ${currentStepIndex + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                LinearProgressIndicator(
                    progress = { (currentStepIndex + 1).toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Session title and description
        Text(
            text = currentStep.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = currentStep.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Visual meditation guide (clickable)
        Card(
            onClick = onPlayPause,
            modifier = Modifier.size(240.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MeditationVisual(
                    isPlaying = isPlaying,
                    sessionState = sessionState,
                    modifier = Modifier.fillMaxSize()
                )

                // Center icon (clickable area)
                Icon(
                    when (sessionState) {
                        MeditationSessionState.COMPLETED -> Icons.Default.CheckCircle
                        else -> if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Time remaining - now shows total session time
        Text(
            text = formatTime(totalTimeRemaining),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Total time remaining",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Current guidance text - show when TTS is disabled
        val isCustomMeditation = meditationType.startsWith("custom_ai_")
        val shouldShowText = currentStep.guidance.isNotEmpty() && (
            !isCustomMeditation || // Always show for regular meditations
            (isCustomMeditation && !ttsEnabled) // Show for custom when TTS is off
        )

        if (shouldShowText) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = currentStep.guidance,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Audio status indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ttsEnabled) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Voice",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (backgroundSound != BackgroundSound.NONE) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = backgroundSound.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (binauralEnabled && binauralTone != BinauralTone.NONE) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${binauralTone.frequency.toInt()}Hz",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }

            Button(
                onClick = onPlayPause,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isPlaying) "Pause" else "Play")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Unified settings dialog (replaces background sound menu)
    if (showSoundMenu) {
        UnifiedMeditationSettingsDialog(
            settings = settings,
            context = context,
            soundEnabled = soundEnabled,
            onSoundToggle = onSoundToggle,
            backgroundSound = backgroundSound,
            onBackgroundSoundChange = onBackgroundSoundChange,
            binauralEnabled = binauralEnabled,
            onBinauralToggle = onBinauralToggle,
            binauralTone = binauralTone,
            onBinauralToneChange = onBinauralToneChange,
            ttsEnabled = ttsEnabled,
            onTtsToggle = onTtsToggle,
            onBackgroundVolumeChange = onBackgroundVolumeChange,
            onBinauralVolumeChange = onBinauralVolumeChange,
            onTtsVolumeChange = onTtsVolumeChange,
            onTtsSpeedChange = onTtsSpeedChange,
            onTtsPitchChange = onTtsPitchChange,
            onDismiss = { showSoundMenu = false }
        )
    }

    // Binaural tone selection menu
    if (showBinauralMenu) {
        BinauralToneMenu(
            currentTone = binauralTone,
            onToneSelected = { tone ->
                onBinauralToneChange(tone)
                showBinauralMenu = false
            },
            onDismiss = { showBinauralMenu = false }
        )
    }
}

@Composable
private fun MeditationVisual(
    isPlaying: Boolean,
    sessionState: MeditationSessionState,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "meditation")

    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val baseRadius = size.minDimension / 6

            when (sessionState) {
                MeditationSessionState.COMPLETED -> {
                    drawCompletionVisual(centerX, centerY, baseRadius)
                }
                else -> {
                    if (isPlaying) {
                        drawBreathingVisual(centerX, centerY, baseRadius, breathingScale)
                        drawRippleEffect(centerX, centerY, baseRadius, rippleScale)
                    } else {
                        drawStaticVisual(centerX, centerY, baseRadius)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBreathingVisual(centerX: Float, centerY: Float, baseRadius: Float, scale: Float) {
    val radius = baseRadius * scale
    drawCircle(
        color = Color(0xFF4CAF50).copy(alpha = 0.3f),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
    drawCircle(
        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
        radius = radius * 1.5f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
}

private fun DrawScope.drawRippleEffect(centerX: Float, centerY: Float, baseRadius: Float, scale: Float) {
    val radius = baseRadius * (2 + scale)
    val alpha = (1 - scale) * 0.2f
    drawCircle(
        color = Color(0xFF2196F3).copy(alpha = alpha),
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
}

private fun DrawScope.drawStaticVisual(centerX: Float, centerY: Float, baseRadius: Float) {
    drawCircle(
        color = Color(0xFF9C27B0).copy(alpha = 0.3f),
        radius = baseRadius,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
}

private fun DrawScope.drawCompletionVisual(centerX: Float, centerY: Float, baseRadius: Float) {
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = 0.4f),
        radius = baseRadius * 1.2f,
        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
    )
}

@Composable
private fun BackgroundSoundMenu(
    currentSound: BackgroundSound,
    onSoundSelected: (BackgroundSound) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Background Sound") },
        text = {
            Column {
                BackgroundSound.values().forEach { sound ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSound == sound,
                            onClick = { onSoundSelected(sound) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sound.displayName)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun BinauralToneMenu(
    currentTone: BinauralTone,
    onToneSelected: (BinauralTone) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Binaural Tones") },
        text = {
            Column {
                Text(
                    text = "Binaural beats may help enhance meditation by promoting specific brainwave states. Note: You need stereo headphones to experience the binaural effect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                BinauralTone.values().forEach { tone ->
                    val isSelected = currentTone == tone
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onToneSelected(tone) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Frequency badge
                            if (tone != BinauralTone.NONE) {
                                Card(
                                    modifier = Modifier.size(40.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        }
                                    ),
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${tone.frequency.toInt()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                // None option - show a different icon
                                Card(
                                    modifier = Modifier.size(40.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.outline
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        }
                                    ),
                                    shape = CircleShape
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "—",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Content
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tone.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                                
                                if (tone != BinauralTone.NONE) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tone.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "No binaural tone",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        }
                                    )
                                }
                            }
                            
                            // Selection indicator
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}