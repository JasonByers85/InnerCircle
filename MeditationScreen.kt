package com.google.mediapipe.examples.llminference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.util.Log

@Composable
internal fun MeditationRoute(
    onBack: () -> Unit,
    onStartSession: (String) -> Unit = {}
) {
    val context = LocalContext.current
    MeditationScreen(
        onBack = onBack,
        onStartSession = onStartSession,
        context = context
    )
}

@Composable
fun MeditationScreen(
    onBack: () -> Unit,
    onStartSession: (String) -> Unit,
    context: android.content.Context
) {

    val meditationSettings = remember { MeditationSettings.getInstance(context) }
    val stats = remember { meditationSettings.getMeditationStatistics() }
    var showSettings by remember { mutableStateOf(false) }
    var showCustomMeditationDialog by remember { mutableStateOf(false) }

    // Unified settings state management
    var soundEnabled by remember { mutableStateOf(meditationSettings.isSoundEnabled()) }
    var backgroundSound by remember { mutableStateOf(meditationSettings.getBackgroundSound()) }
    var binauralEnabled by remember { mutableStateOf(meditationSettings.isBinauralEnabled()) }
    var binauralTone by remember { mutableStateOf(meditationSettings.getBinauralTone()) }
    var ttsEnabled by remember { mutableStateOf(meditationSettings.isTtsEnabled()) }

    fun onSoundToggle() {
        val newValue = !soundEnabled
        soundEnabled = newValue
        meditationSettings.setSoundEnabled(newValue)
    }

    fun onBackgroundSoundChange(newSound: BackgroundSound) {
        backgroundSound = newSound
        meditationSettings.setBackgroundSound(newSound)
    }

    fun onBinauralToggle() {
        val newValue = !binauralEnabled
        binauralEnabled = newValue
        meditationSettings.setBinauralEnabled(newValue)
    }

    fun onBinauralToneChange(newTone: BinauralTone) {
        binauralTone = newTone
        meditationSettings.setBinauralTone(newTone)
    }

    fun onTtsToggle() {
        val newValue = !ttsEnabled
        ttsEnabled = newValue
        meditationSettings.setTtsEnabled(newValue)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Top bar with settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                // Single unified settings button
                IconButton(
                    onClick = { showSettings = true }
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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

        // Statistics card
        if (stats.totalSessions > 0) {
            item {
                MeditationStatsCard(stats)
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
                    icon = Icons.Default.Healing,
                    onClick = { onStartSession("stress_relief") },
                    modifier = Modifier.weight(1f)
                )
                MeditationCard(
                    title = "Focus Boost",
                    duration = "8 min",
                    description = "Enhance concentration",
                    icon = Icons.Default.Visibility,
                    onClick = { onStartSession("focus_boost") },
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
                    icon = Icons.Default.Bedtime,
                    onClick = { onStartSession("sleep_prep") },
                    modifier = Modifier.weight(1f)
                )
                MeditationCard(
                    title = "Anxiety Ease",
                    duration = "7 min",
                    description = "Gentle anxiety relief",
                    icon = Icons.Default.FavoriteBorder,
                    onClick = { onStartSession("anxiety_ease") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Longer Sessions (15-60 min)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MeditationCard(
                    title = "Deep Relaxation",
                    duration = "20 min",
                    description = "Complete body and mind reset with progressive relaxation",
                    icon = Icons.Default.Spa,
                    onClick = { onStartSession("deep_relaxation") },
                    modifier = Modifier.weight(1f)
                )
                MeditationCard(
                    title = "Mindful Awareness",
                    duration = "15 min",
                    description = "Develop present-moment awareness and mindful observation",
                    icon = Icons.Default.Psychology,
                    onClick = { onStartSession("mindful_awareness") },
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
                    title = "Extended Focus",
                    duration = "38 min",
                    description = "Deep concentration practice for advanced practitioners",
                    icon = Icons.Default.CenterFocusStrong,
                    onClick = { onStartSession("extended_focus") },
                    modifier = Modifier.weight(1f)
                )
                MeditationCard(
                    title = "Complete Zen",
                    duration = "45 min",
                    description = "Full meditation experience with breathing, awareness, and stillness",
                    icon = Icons.Default.SelfImprovement,
                    onClick = { onStartSession("complete_zen") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                text = "Custom Meditation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            MeditationCard(
                title = "AI-Generated Session",
                duration = "Custom",
                description = "Personalized meditation created by AI based on your preferences",
                icon = Icons.Default.AutoAwesome,
                onClick = { showCustomMeditationDialog = true },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Voice guidance info card (removed separate settings button, just info)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Voice Guidance Available",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Customize voice, audio mix, and meditation settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    // Unified Settings Dialog
    if (showSettings) {
        UnifiedMeditationSettingsDialog(
            settings = meditationSettings,
            context = context,
            soundEnabled = soundEnabled,
            onSoundToggle = ::onSoundToggle,
            backgroundSound = backgroundSound,
            onBackgroundSoundChange = ::onBackgroundSoundChange,
            binauralEnabled = binauralEnabled,
            onBinauralToggle = ::onBinauralToggle,
            binauralTone = binauralTone,
            onBinauralToneChange = ::onBinauralToneChange,
            ttsEnabled = ttsEnabled,
            onTtsToggle = ::onTtsToggle,
            onBackgroundVolumeChange = { /* No-op for this screen */ },
            onBinauralVolumeChange = { /* No-op for this screen */ },
            onTtsVolumeChange = { /* No-op for this screen */ },
            onTtsSpeedChange = { /* No-op for this screen */ },
            onTtsPitchChange = { /* No-op for this screen */ },
            onDismiss = { showSettings = false }
        )
    }

    if (showCustomMeditationDialog) {
        CustomMeditationDialog(
            context = context,
            onDismiss = { showCustomMeditationDialog = false },
            onStartCustomSession = { sessionId ->
                showCustomMeditationDialog = false
                onStartSession(sessionId)
            }
        )
    }
}

@Composable
private fun MeditationCard(
    title: String,
    duration: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = duration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MeditationStatsCard(stats: MeditationStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = stats.totalSessions.toString(),
                    label = "Sessions",
                    icon = Icons.Default.PlayCircle
                )
                StatItem(
                    value = stats.totalMinutes.toString(),
                    label = "Minutes",
                    icon = Icons.Default.Timer
                )
                StatItem(
                    value = stats.currentStreak.toString(),
                    label = "Day Streak",
                    icon = Icons.Default.LocalFireDepartment
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CustomMeditationDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onStartCustomSession: (String) -> Unit
) {
    var selectedDuration by remember { mutableStateOf(15) } // minutes
    var selectedSteps by remember { mutableStateOf(5) }
    var meditationFocus by remember { mutableStateOf("") }
    var currentMood by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generationProgress by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Create Custom Meditation",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Text(
                        "AI will create a personalized meditation session based on your preferences",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                item {
                    Text(
                        "Duration: ${selectedDuration} minutes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = selectedDuration.toFloat(),
                        onValueChange = { selectedDuration = it.toInt() },
                        valueRange = 5f..60f,
                        steps = 10
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5 min", style = MaterialTheme.typography.labelSmall)
                        Text("60 min", style = MaterialTheme.typography.labelSmall)
                    }
                }

                item {
                    Text(
                        "Number of Steps: $selectedSteps",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = selectedSteps.toFloat(),
                        onValueChange = { selectedSteps = it.toInt() },
                        valueRange = 3f..8f,
                        steps = 4
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("3 steps", style = MaterialTheme.typography.labelSmall)
                        Text("8 steps", style = MaterialTheme.typography.labelSmall)
                    }
                }

                item {
                    OutlinedTextField(
                        value = meditationFocus,
                        onValueChange = { meditationFocus = it },
                        label = { Text("Focus/Goal") },
                        placeholder = { Text("e.g., stress relief, better sleep, creativity...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    OutlinedTextField(
                        value = currentMood,
                        onValueChange = { currentMood = it },
                        label = { Text("Current Mood") },
                        placeholder = { Text("e.g., anxious, tired, excited...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                item {
                    Text(
                        "Experience Level",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                            FilterChip(
                                onClick = { experience = level },
                                label = { Text(level) },
                                selected = experience == level,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (isGenerating) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "AI is creating your custom meditation...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (generationProgress.isNotEmpty()) {
                                        Text(
                                            generationProgress,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isGenerating) {
                        isGenerating = true
                        generateCustomMeditation(
                            context = context,
                            duration = selectedDuration,
                            steps = selectedSteps,
                            focus = meditationFocus,
                            mood = currentMood,
                            experience = experience,
                            onProgress = { progress ->
                                generationProgress = progress
                            },
                            onComplete = { sessionId ->
                                isGenerating = false
                                generationProgress = ""
                                onStartCustomSession(sessionId)
                            },
                            onError = {
                                isGenerating = false
                                generationProgress = ""
                                // Could show error dialog here
                            }
                        )
                    }
                },
                enabled = !isGenerating && experience.isNotEmpty()
            ) {
                Text(if (isGenerating) "Generating..." else "Create Meditation")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun generateCustomMeditation(
    context: android.content.Context,
    duration: Int,
    steps: Int,
    focus: String,
    mood: String,
    experience: String,
    onProgress: (String) -> Unit,
    onComplete: (String) -> Unit,
    onError: () -> Unit
) {
    try {
        onProgress("Initializing AI meditation generator...")
        val inferenceModel = InferenceModel.getInstance(context)

        val stepDuration = (duration * 60) / steps

        val prompt = buildSystemPrompt(duration, steps, stepDuration, focus, mood, experience)

        val progressListener = object : com.google.mediapipe.tasks.genai.llminference.ProgressListener<String> {
            private var currentStep = 1

            override fun run(partialResult: String?, done: Boolean) {
                if (partialResult?.contains("Step") == true || partialResult?.contains("title") == true) {
                    onProgress("Creating step $currentStep of $steps...")
                    currentStep++
                }

                if (done && partialResult != null) {
                    onProgress("Finalizing meditation session...")
                }
            }
        }

        val responseFuture = inferenceModel.generateResponseAsync(prompt, progressListener)

        responseFuture.addListener({
            try {
                val response = responseFuture.get()
                Log.d("CustomMeditation", "AI Response: $response")

                val sessionId = parseAndStoreCustomMeditation(context, response, duration, steps, stepDuration)
                onComplete(sessionId)
            } catch (e: Exception) {
                Log.e("CustomMeditation", "Error generating meditation", e)
                onError()
            }
        }, context.mainExecutor)

    } catch (e: Exception) {
        Log.e("CustomMeditation", "Error starting generation", e)
        onError()
    }
}

private fun buildSystemPrompt(
    duration: Int,
    steps: Int,
    stepDuration: Int,
    focus: String,
    mood: String,
    experience: String
): String {
    return """You are a meditation instructor creating a personalized ${duration}-minute meditation session.

REQUIREMENTS:
- Create exactly $steps meditation steps
- Each step should be ${stepDuration} seconds (about ${stepDuration/60} minutes)
- Each guidance should be 2-3 sentences maximum
- Use calming, clear, and simple language
- Focus: ${if (focus.isNotEmpty()) focus else "general relaxation"}
- Current mood: ${if (mood.isNotEmpty()) mood else "neutral"}
- Experience level: $experience

FORMAT: Respond with ONLY this JSON structure, no other text:
{
  "steps": [
    {
      "title": "Step Name",
      "description": "Brief description", 
      "guidance": "2-3 sentences of meditation instruction.",
      "durationSeconds": $stepDuration
    }
  ]
}

Create a meditation that flows naturally from beginning to end, starting with settling in and ending with integration."""
}

private fun parseAndStoreCustomMeditation(
    context: android.content.Context,
    aiResponse: String,
    duration: Int,
    steps: Int,
    stepDuration: Int
): String {
    val sessionId = "custom_ai_${System.currentTimeMillis()}"

    try {
        // Simple JSON extraction since we don't have a JSON library
        val steps = parseSimpleJsonSteps(aiResponse, steps, stepDuration)

        // Store the parsed steps
        val prefs = context.getSharedPreferences("custom_meditations", android.content.Context.MODE_PRIVATE)
        val editor = prefs.edit()

        steps.forEachIndexed { index, step ->
            editor.putString("${sessionId}_step_${index}_title", step.title)
            editor.putString("${sessionId}_step_${index}_description", step.description)
            editor.putString("${sessionId}_step_${index}_guidance", step.guidance)
            editor.putInt("${sessionId}_step_${index}_duration", step.durationSeconds)
        }

        editor.putInt("${sessionId}_total_steps", steps.size)
        editor.putInt("${sessionId}_duration", duration)
        editor.apply()

        Log.d("CustomMeditation", "Stored ${steps.size} steps for session $sessionId")
        Log.d("CustomMeditation", "Session ID being returned: $sessionId")

    } catch (e: Exception) {
        Log.e("CustomMeditation", "Error parsing AI response, using fallback", e)
        // Store fallback data
        storeFallbackSession(context, sessionId, duration, steps, stepDuration)
    }

    return sessionId
}

private fun parseSimpleJsonSteps(response: String, expectedSteps: Int, stepDuration: Int): List<MeditationStep> {
    val steps = mutableListOf<MeditationStep>()

    try {
        // Extract steps between "steps": [ and ]
        val stepsSection = response.substringAfter("\"steps\":")
            .substringAfter("[")
            .substringBefore("]")

        // Split by objects (rough parsing)
        val stepObjects = stepsSection.split("},{").map { it.trim() }

        stepObjects.forEach { stepData ->
            try {
                val title = extractJsonValue(stepData, "title").ifEmpty { "Meditation Step" }
                val description = extractJsonValue(stepData, "description").ifEmpty { "Mindful practice" }
                val guidance = extractJsonValue(stepData, "guidance").ifEmpty { "Focus on your breath and be present." }

                steps.add(MeditationStep(
                    title = title,
                    description = description,
                    guidance = guidance,
                    durationSeconds = stepDuration
                ))
            } catch (e: Exception) {
                Log.w("CustomMeditation", "Error parsing step: $stepData")
            }
        }

    } catch (e: Exception) {
        Log.e("CustomMeditation", "Error parsing JSON response", e)
    }

    // Fallback if parsing failed
    if (steps.isEmpty()) {
        return createFallbackSteps(expectedSteps, stepDuration)
    }

    return steps.take(expectedSteps)
}

private fun extractJsonValue(json: String, key: String): String {
    return try {
        json.substringAfter("\"$key\":")
            .substringAfter("\"")
            .substringBefore("\"")
            .trim()
    } catch (e: Exception) {
        ""
    }
}

private fun createFallbackSteps(stepCount: Int, stepDuration: Int): List<MeditationStep> {
    val fallbackSteps = listOf(
        MeditationStep("Welcome", "Beginning meditation", "Find a comfortable position and take three deep breaths. Allow yourself to settle into this moment.", stepDuration),
        MeditationStep("Breath Focus", "Connecting with breath", "Turn your attention to your natural breathing. Notice each inhale and exhale without changing anything.", stepDuration),
        MeditationStep("Body Awareness", "Scanning the body", "Slowly scan through your body from head to toe. Notice any sensations with gentle curiosity.", stepDuration),
        MeditationStep("Mind Stillness", "Calming thoughts", "When thoughts arise, acknowledge them kindly and return to your breath. Rest in this peaceful awareness.", stepDuration),
        MeditationStep("Heart Opening", "Cultivating compassion", "Send kindness to yourself and others. Feel your heart opening with warmth and compassion.", stepDuration),
        MeditationStep("Integration", "Completing practice", "Take a moment to appreciate this time you've given yourself. Slowly return to your surroundings.", stepDuration),
        MeditationStep("Peaceful Presence", "Resting in stillness", "Simply be present with whatever arises. There's nothing to fix or change, just peaceful being.", stepDuration),
        MeditationStep("Gratitude", "Feeling thankful", "Reflect on something you're grateful for. Let this appreciation fill your heart with warmth.", stepDuration)
    )

    return fallbackSteps.take(stepCount)
}

private fun storeFallbackSession(
    context: android.content.Context,
    sessionId: String,
    duration: Int,
    stepCount: Int,
    stepDuration: Int
) {
    val steps = createFallbackSteps(stepCount, stepDuration)
    val prefs = context.getSharedPreferences("custom_meditations", android.content.Context.MODE_PRIVATE)
    val editor = prefs.edit()

    steps.forEachIndexed { index, step ->
        editor.putString("${sessionId}_step_${index}_title", step.title)
        editor.putString("${sessionId}_step_${index}_description", step.description)
        editor.putString("${sessionId}_step_${index}_guidance", step.guidance)
        editor.putInt("${sessionId}_step_${index}_duration", step.durationSeconds)
    }

    editor.putInt("${sessionId}_total_steps", steps.size)
    editor.putInt("${sessionId}_duration", duration)
    editor.apply()
}