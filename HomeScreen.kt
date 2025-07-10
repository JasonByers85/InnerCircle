package com.google.mediapipe.examples.llminference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeRoute(
    onNavigateToQuickChat: () -> Unit,
    onNavigateToMeditation: () -> Unit,
    onNavigateToBreathing: () -> Unit,
    onNavigateToMoodTracker: () -> Unit,
    onNavigateToDreamInterpreter: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val userProfile = remember { UserProfile.getInstance(context) }

    HomeScreen(
        userProfile = userProfile,
        onNavigateToQuickChat = onNavigateToQuickChat,
        onNavigateToMeditation = onNavigateToMeditation,
        onNavigateToBreathing = onNavigateToBreathing,
        onNavigateToMoodTracker = onNavigateToMoodTracker,
        onNavigateToDreamInterpreter = onNavigateToDreamInterpreter
    )
}

@Composable
fun HomeScreen(
    userProfile: UserProfile,
    onNavigateToQuickChat: () -> Unit,
    onNavigateToMeditation: () -> Unit,
    onNavigateToBreathing: () -> Unit,
    onNavigateToMoodTracker: () -> Unit,
    onNavigateToDreamInterpreter: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WelcomeCard(userProfile)
        }

        item {
            Text(
                text = "Wellness Tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WellnessFeatureCard(
                    title = "Mood Tracker",
                    description = "Track and understand your emotions",
                    icon = Icons.Default.Mood,
                    onClick = onNavigateToMoodTracker,
                    modifier = Modifier.weight(1f)
                )

                WellnessFeatureCard(
                    title = "Quick Support",
                    description = "Get instant wellness advice",
                    icon = Icons.Default.Psychology,
                    onClick = onNavigateToQuickChat,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WellnessFeatureCard(
                    title = "Guided Meditation",
                    description = "Personalized meditation sessions",
                    icon = Icons.Default.SelfImprovement,
                    onClick = onNavigateToMeditation,
                    modifier = Modifier.weight(1f)
                )

                WellnessFeatureCard(
                    title = "Breathing Exercises",
                    description = "Calm your mind and body",
                    icon = Icons.Default.Air,
                    onClick = onNavigateToBreathing,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            WellnessFeatureCard(
                title = "Dream Interpreter",
                description = "Understand the meaning behind your dreams",
                icon = Icons.Default.Bedtime,
                onClick = onNavigateToDreamInterpreter,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            PrivacyReminderCard()
        }
    }
}

@Composable
private fun WelcomeCard(userProfile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            val greeting = when {
                userProfile.mood.isNotEmpty() -> "Hi there! Ready to continue your wellness journey?"
                else -> "Welcome to your wellness dashboard!"
            }

            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Explore mindfulness, track your emotions, and discover insights about yourself through our wellness tools.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun WellnessFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PrivacyReminderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Privacy",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "All AI processing happens on your device. Your conversations and data stay completely private.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}