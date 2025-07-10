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

// MEDITATION SCREEN
@Composable
internal fun MeditationRoute(onBack: () -> Unit) {
    MeditationScreen(onBack = onBack)
}

@Composable
fun MeditationScreen(onBack: () -> Unit) {
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
                    text = "Guided Meditation",
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
                        text = "🧘‍♀️ Find Your Peace",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose a meditation session tailored to your current needs and available time.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = "Quick Sessions (5-10 min)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MeditationCard(
                    title = "Stress Relief",
                    duration = "5 min",
                    description = "Quick calm for busy moments",
                    modifier = Modifier.weight(1f)
                )
                MeditationCard(
                    title = "Focus Boost",
                    duration = "8 min",
                    description = "Enhance concentration",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MeditationCard(
                    title = "Sleep Prep",
                    duration = "10 min",
                    description = "Wind down for better rest",
                    modifier = Modifier.weight(1f)
                )
                MeditationCard(
                    title = "Anxiety Ease",
                    duration = "7 min",
                    description = "Gentle anxiety relief",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Longer Sessions (15-20 min)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            MeditationCard(
                title = "Deep Relaxation",
                duration = "20 min",
                description = "Complete body and mind reset",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MeditationCard(
                title = "Mindful Awareness",
                duration = "15 min",
                description = "Develop present-moment awareness",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MeditationCard(
    title: String,
    duration: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { /* TODO: Start meditation session */ },
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

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



@Composable
fun MoodTrackerScreen(onBack: () -> Unit, context: Context) {
    var selectedMood by remember { mutableStateOf("") }
    var moodNote by remember { mutableStateOf("") }
    val userProfile = remember { UserProfile.getInstance(context) }

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
                    text = "Mood Tracker",
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
                        text = "📊 Track Your Feelings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Understanding your mood patterns helps you recognize triggers and celebrate progress.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = "How are you feeling right now?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(200.dp)
            ) {
                val moods = listOf(
                    "😊 Happy" to "happy",
                    "😌 Calm" to "calm",
                    "😔 Sad" to "sad",
                    "😰 Anxious" to "anxious",
                    "😴 Tired" to "tired",
                    "😠 Frustrated" to "frustrated"
                )

                items(moods) { (display, value) ->
                    MoodButton(
                        text = display,
                        isSelected = selectedMood == value,
                        onClick = { selectedMood = value }
                    )
                }
            }
        }

        item {
            TextField(
                value = moodNote,
                onValueChange = { moodNote = it },
                label = { Text("Add a note (optional)") },
                placeholder = { Text("What's influencing your mood today?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        item {
            Button(
                onClick = {
                    if (selectedMood.isNotEmpty()) {
                        userProfile.updateMood(selectedMood)
                        if (moodNote.isNotEmpty()) {
                            userProfile.addTopic(moodNote)
                        }
                        userProfile.saveProfile(context)
                        // Show success message or navigate back
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedMood.isNotEmpty()
            ) {
                Text("Save Mood Entry")
            }
        }

        item {
            if (userProfile.mood.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Recent Mood: ${userProfile.mood}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keep tracking to see patterns and progress over time.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center
        )
    }
}