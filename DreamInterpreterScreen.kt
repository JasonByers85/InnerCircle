package com.google.mediapipe.examples.llminference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun DreamInterpreterRoute(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DreamInterpreterViewModel = viewModel(factory = DreamInterpreterViewModel.getFactory(context))

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val interpretation by viewModel.interpretation.collectAsStateWithLifecycle()
    val isInputEnabled by viewModel.isInputEnabled.collectAsStateWithLifecycle()
    val dreamHistory by viewModel.dreamHistory.collectAsStateWithLifecycle()

    DreamInterpreterScreen(
        isLoading = isLoading,
        interpretation = interpretation,
        isInputEnabled = isInputEnabled,
        dreamHistory = dreamHistory,
        onInterpretDream = viewModel::interpretDream,
        onBack = onBack
    )
}

@Composable
fun DreamInterpreterScreen(
    isLoading: Boolean,
    interpretation: String,
    isInputEnabled: Boolean,
    dreamHistory: List<DreamEntry>,
    onInterpretDream: (String) -> Unit,
    onBack: () -> Unit
) {
    var dreamDescription by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Auto-scroll to bottom when content changes
    LaunchedEffect(isLoading, interpretation) {
        if (interpretation.isNotEmpty() || isLoading) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Top bar - Fixed at top
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "Dream Interpreter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // Scrollable content area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🌙 Explore Your Dreams",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Dreams often reflect our subconscious thoughts, emotions, and experiences. Share your dream and discover potential meanings and insights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Dream examples (only show when not loading and no interpretation)
            if (!isLoading && interpretation.isEmpty()) {
                DreamExampleButtons { example ->
                    dreamDescription = example
                }
            }

            // Recent dreams section
            if (dreamHistory.isNotEmpty() && !isLoading && interpretation.isEmpty()) {
                RecentDreamsSection(dreamHistory) { dreamText ->
                    dreamDescription = dreamText
                }
            }

            // Interpretation area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Interpreting your dream...")
                            }
                        }
                        interpretation.isNotEmpty() -> {
                            Text(
                                text = interpretation,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        else -> {
                            Text(
                                text = "Share your dream and I'll help you explore its possible meanings, symbolism, and connections to your waking life.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.align(Alignment.Center),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Extra space to ensure input area is visible
            Spacer(modifier = Modifier.height(120.dp))
        }

        // Input area - Fixed at bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    TextField(
                        value = dreamDescription,
                        onValueChange = { dreamDescription = it },
                        label = { Text("Describe your dream") },
                        placeholder = { Text("Tell me about your dream in as much detail as you remember...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 6,
                        enabled = isInputEnabled,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { dreamDescription = "" },
                            modifier = Modifier.weight(1f),
                            enabled = dreamDescription.isNotEmpty()
                        ) {
                            Text("Clear")
                        }

                        Button(
                            onClick = {
                                if (dreamDescription.isNotBlank()) {
                                    onInterpretDream(dreamDescription)
                                    dreamDescription = ""
                                }
                            },
                            modifier = Modifier.weight(2f),
                            enabled = isInputEnabled && dreamDescription.isNotBlank()
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Interpret Dream")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamExampleButtons(onExampleSelected: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Need inspiration? Try these common dream scenarios:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onExampleSelected("I was flying over a beautiful landscape, feeling completely free and peaceful.") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Flying Dream", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
                onClick = { onExampleSelected("I was being chased by someone I couldn't see, running through endless hallways.") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Being Chased", style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onExampleSelected("I found myself in my childhood home, but everything was different and I felt lost.") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Lost in Familiar Place", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(
                onClick = { onExampleSelected("I was talking to someone who passed away, and they gave me important advice.") },
                modifier = Modifier.weight(1f)
            ) {
                Text("Deceased Person", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun RecentDreamsSection(
    dreamHistory: List<DreamEntry>,
    onDreamSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Recent Dreams",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            dreamHistory.takeLast(3).forEach { dream ->
                OutlinedButton(
                    onClick = { onDreamSelected(dream.description) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = dream.description.take(60) + if (dream.description.length > 60) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// Data class for dream entries
data class DreamEntry(
    val description: String,
    val interpretation: String,
    val timestamp: Long
)