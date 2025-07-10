package com.google.mediapipe.examples.llminference

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.mediapipe.examples.llminference.ui.theme.LLMInferenceTheme

const val START_SCREEN = "start_screen"
const val LOAD_SCREEN = "load_screen"
const val HOME_SCREEN = "home_screen"
const val QUICK_CHAT_SCREEN = "quick_chat_screen"
const val MEDITATION_SCREEN = "meditation_screen"
const val BREATHING_SCREEN = "breathing_screen"
const val MOOD_TRACKER_SCREEN = "mood_tracker_screen"
const val DREAM_INTERPRETER_SCREEN = "dream_interpreter_screen"
const val SETTINGS_SCREEN = "settings_screen"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LLMInferenceTheme {
                Scaffold(
                    topBar = { AppBar() }
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        val navController = rememberNavController()
                        val startDestination = intent.getStringExtra("NAVIGATE_TO") ?: START_SCREEN

                        NavHost(
                            navController = navController,
                            startDestination = startDestination
                        ) {
                            composable(START_SCREEN) {
                                SelectionRoute(
                                    onModelSelected = {
                                        navController.navigate(LOAD_SCREEN) {
                                            popUpTo(START_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(LOAD_SCREEN) {
                                LoadingRoute(
                                    onModelLoaded = {
                                        navController.navigate(HOME_SCREEN) {
                                            popUpTo(LOAD_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onGoBack = {
                                        navController.navigate(START_SCREEN) {
                                            popUpTo(LOAD_SCREEN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(HOME_SCREEN) {
                                HomeRoute(
                                    onNavigateToQuickChat = {
                                        navController.navigate(QUICK_CHAT_SCREEN)
                                    },
                                    onNavigateToMeditation = {
                                        navController.navigate(MEDITATION_SCREEN)
                                    },
                                    onNavigateToBreathing = {
                                        navController.navigate(BREATHING_SCREEN)
                                    },
                                    onNavigateToMoodTracker = {
                                        navController.navigate(MOOD_TRACKER_SCREEN)
                                    },
                                    onNavigateToDreamInterpreter = {
                                        navController.navigate(DREAM_INTERPRETER_SCREEN)
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate(SETTINGS_SCREEN)
                                    }
                                )
                            }

                            composable(QUICK_CHAT_SCREEN) {
                                QuickChatRoute(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(MEDITATION_SCREEN) {
                                MeditationRoute(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(BREATHING_SCREEN) {
                                BreathingRoute(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(MOOD_TRACKER_SCREEN) {
                                MoodTrackerRoute(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(DREAM_INTERPRETER_SCREEN) {
                                DreamInterpreterRoute(
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(SETTINGS_SCREEN) {
                                SettingsRoute(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppBar() {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TopAppBar(
                title = {
                    Text(
                        "🌟 WellnessFriend",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                    .fillMaxWidth()
            ) {
                Text(
                    text = "💙 Your AI wellness companion for mindfulness, mood tracking, and dream insights.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}