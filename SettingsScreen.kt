package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
internal fun SettingsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    SettingsScreen(onBack = onBack, context = context)
}

@Composable
fun SettingsScreen(onBack: () -> Unit, context: Context) {
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showClearMoodDialog by remember { mutableStateOf(false) }
    var showClearDreamDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val userProfile = remember { UserProfile.getInstance(context) }
    val moodStorage = remember { MoodStorage.getInstance(context) }
    val dreamStorage = remember { DreamStorage.getInstance(context) }

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
                    text = "Settings",
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
                        text = "⚙️ App Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Manage your data, privacy settings, and app preferences.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Text(
                text = "Privacy & Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            SettingsCard(
                title = "Clear All Data",
                description = "Remove all stored mood entries, dream interpretations, and app data",
                icon = Icons.Default.DeleteForever,
                onClick = { showClearDataDialog = true },
                isDestructive = true
            )
        }

        item {
            SettingsCard(
                title = "Clear Mood History",
                description = "Remove only mood tracking data while keeping other settings",
                icon = Icons.Default.Mood,
                onClick = { showClearMoodDialog = true },
                isDestructive = true
            )
        }

        item {
            SettingsCard(
                title = "Clear Dream History",
                description = "Remove only dream entries and interpretations",
                icon = Icons.Default.Bedtime,
                onClick = { showClearDreamDialog = true },
                isDestructive = true
            )
        }

        item {
            Text(
                text = "App Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        item {
            SettingsCard(
                title = "About WellnessFriend",
                description = "Learn more about the app, privacy policy, and version info",
                icon = Icons.Default.Info,
                onClick = { showAboutDialog = true }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Reminder",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All your data is stored locally on your device. Nothing is shared with external services. The AI model runs entirely offline for complete privacy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }

    // Clear All Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = {
                Text("This will permanently delete all your mood entries, dream interpretations, user preferences, and app data. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Clear all data
                        userProfile.clearProfile(context)
                        moodStorage.clearAllMoodEntries()
                        dreamStorage.clearAllDreamEntries()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Mood History Dialog
    if (showClearMoodDialog) {
        AlertDialog(
            onDismissRequest = { showClearMoodDialog = false },
            title = { Text("Clear Mood History") },
            text = {
                Text("This will permanently delete all your mood tracking data. Your other app settings and dream history will be preserved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Clear only mood data
                        moodStorage.clearAllMoodEntries()
                        userProfile.updateMood("") // Clear current mood
                        userProfile.saveProfile(context)
                        showClearMoodDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Mood History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMoodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear Dream History Dialog
    if (showClearDreamDialog) {
        AlertDialog(
            onDismissRequest = { showClearDreamDialog = false },
            title = { Text("Clear Dream History") },
            text = {
                Text("This will permanently delete all your dream entries and interpretations. Your other app settings and mood history will be preserved.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Clear only dream data
                        dreamStorage.clearAllDreamEntries()
                        showClearDreamDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Dream History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDreamDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About WellnessFriend") },
            text = {
                Column {
                    Text("WellnessFriend is an AI-powered wellness companion that helps you track moods, practice mindfulness, and explore your dreams through thoughtful interpretation.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Features:")
                    Text("• Mood tracking and insights")
                    Text("• Guided meditation sessions")
                    Text("• Breathing exercises")
                    Text("• Dream interpretation")
                    Text("• 100% offline AI processing")
                    Text("• Complete data privacy")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Version 1.0.0 - Built with Google's Gemma model",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDestructive) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}