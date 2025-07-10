package com.google.mediapipe.examples.llminference

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

// BREATHING EXERCISES SCREEN
@Composable
internal fun BreathingRoute(onBack: () -> Unit) {
    BreathingScreen(onBack = onBack)
}

@Composable
fun BreathingScreen(onBack: () -> Unit) {
    var isActive by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf("Breathe In") }
    var timeRemaining by remember { mutableStateOf(60) }

    LaunchedEffect(isActive) {
        if (isActive) {
            val phases = listOf("Breathe In" to 4, "Hold" to 4, "Breathe Out" to 6)
            var phaseIndex = 0
            var phaseTime = phases[phaseIndex].second

            while (isActive && timeRemaining > 0) {
                delay(1000)
                phaseTime--
                timeRemaining--

                if (phaseTime <= 0) {
                    phaseIndex = (phaseIndex + 1) % phases.size
                    currentPhase = phases[phaseIndex].first
                    phaseTime = phases[phaseIndex].second
                }
            }

            if (timeRemaining <= 0) {
                isActive = false
                currentPhase = "Complete!"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Breathing Exercises",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🫁 Breathe & Relax",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Use the 4-4-6 breathing technique to calm your nervous system and reduce stress.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Breathing circle
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
                BreathingCircle(isActive = isActive, phase = currentPhase)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentPhase,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isActive) {
                        Text(
                            text = "${timeRemaining}s remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
                        timeRemaining = 60
                        currentPhase = "Breathe In"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isActive) "Pause" else "Start")
            }

            OutlinedButton(
                onClick = {
                    isActive = false
                    timeRemaining = 60
                    currentPhase = "Ready to begin"
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun BreathingCircle(isActive: Boolean, phase: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val color = when (phase) {
        "Breathe In" -> MaterialTheme.colorScheme.primary
        "Hold" -> MaterialTheme.colorScheme.secondary
        "Breathe Out" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.3f))
            .then(
                if (isActive) Modifier.size((200 * scale).dp)
                else Modifier.size(200.dp)
            )
    )
}