package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun MoodTrackerRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: MoodTrackerViewModel = viewModel(factory = MoodTrackerViewModel.getFactory(context))
    
    val moodHistory by viewModel.moodHistory.collectAsStateWithLifecycle()
    val aiInsights by viewModel.aiInsights.collectAsStateWithLifecycle()
    val isLoadingInsights by viewModel.isLoadingInsights.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.loadMoodHistory()
    }
    
    MoodTrackerScreen(
        moodHistory = moodHistory,
        aiInsights = aiInsights,
        isLoadingInsights = isLoadingInsights,
        onBack = onBack,
        onSaveMood = { mood, note -> viewModel.saveMood(mood, note) },
        onGenerateInsights = { viewModel.generateMoodInsights() },
        onClearHistory = { viewModel.clearMoodHistory() },
        context = context
    )
}

@Composable
fun MoodTrackerScreen(
    moodHistory: List<MoodEntry>,
    aiInsights: String,
    isLoadingInsights: Boolean,
    onBack: () -> Unit,
    onSaveMood: (String, String) -> Unit,
    onGenerateInsights: () -> Unit,
    onClearHistory: () -> Unit,
    context: Context
) {
    var selectedMood by remember { mutableStateOf("") }
    var moodNote by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Top bar with tabs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                    
                    // Tab selection
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Track Mood") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("History") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Insights") }
                        )
                    }
                }
            }
        }
        
        when (selectedTab) {
            0 -> {
                // Track Mood Tab
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📊 How are you feeling?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Track your mood to understand patterns and celebrate progress.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
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
                                onSaveMood(selectedMood, moodNote)
                                selectedMood = ""
                                moodNote = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedMood.isNotEmpty()
                    ) {
                        Text("Save Mood Entry")
                    }
                }
            }
            
            1 -> {
                // History Tab
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "📈 Your Mood Journey",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${moodHistory.size} entries recorded",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            OutlinedButton(
                                onClick = onClearHistory,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }
                }
                
                if (moodHistory.isNotEmpty()) {
                    item {
                        // Mood visualization
                        MoodVisualization(moodHistory = moodHistory)
                    }
                    
                    item {
                        // Recent mood entries
                        Text(
                            text = "Recent Entries",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(120.dp)
                        ) {
                            items(moodHistory.takeLast(10).reversed()) { entry ->
                                MoodHistoryCard(entry)
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Timeline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No mood history yet",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Start tracking your mood to see patterns over time",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            
            2 -> {
                // Insights Tab
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🧠 AI Mood Insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Get personalized insights about your mood patterns and suggestions for improvement.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                item {
                    Button(
                        onClick = onGenerateInsights,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoadingInsights && moodHistory.isNotEmpty()
                    ) {
                        if (isLoadingInsights) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing...")
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Mood Insights")
                        }
                    }
                }
                
                if (aiInsights.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Psychology,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Your Mood Analysis",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = aiInsights,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodVisualization(moodHistory: List<MoodEntry>) {
    val density = LocalDensity.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Mood Trend (Last 7 Days)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                val width = size.width
                val height = size.height
                val centerY = height / 2
                
                // Get last 7 days of data
                val recent = moodHistory.takeLast(7)
                if (recent.size < 2) return@Canvas
                
                val stepX = width / (recent.size - 1)
                val moodValues = recent.map { entry ->
                    when (entry.mood) {
                        "happy" -> 1f
                        "calm" -> 0.5f
                        "tired" -> 0f
                        "frustrated" -> -0.5f
                        "sad" -> -0.7f
                        "anxious" -> -0.8f
                        else -> 0f
                    }
                }
                
                // Draw mood line
                for (i in 0 until recent.size - 1) {
                    val x1 = i * stepX
                    val y1 = centerY - (moodValues[i] * centerY * 0.8f)
                    val x2 = (i + 1) * stepX
                    val y2 = centerY - (moodValues[i + 1] * centerY * 0.8f)
                    
                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 4f
                    )
                    
                    // Draw mood points
                    drawCircle(
                        color = Color(0xFF2196F3),
                        radius = 8f,
                        center = Offset(x1, y1)
                    )
                }
                
                // Draw last point
                if (recent.isNotEmpty()) {
                    val lastX = (recent.size - 1) * stepX
                    val lastY = centerY - (moodValues.last() * centerY * 0.8f)
                    drawCircle(
                        color = Color(0xFF2196F3),
                        radius = 8f,
                        center = Offset(lastX, lastY)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mood legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MoodLegendItem("😊", "Happy")
                MoodLegendItem("😌", "Calm")
                MoodLegendItem("😔", "Sad")
                MoodLegendItem("😰", "Anxious")
            }
        }
    }
}

@Composable
private fun MoodLegendItem(emoji: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emoji,
            fontSize = 16.sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MoodHistoryCard(entry: MoodEntry) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = getMoodEmoji(entry.mood),
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.mood.capitalize(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(entry.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
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

private fun getMoodEmoji(mood: String): String {
    return when (mood) {
        "happy" -> "😊"
        "calm" -> "😌"
        "sad" -> "😔"
        "anxious" -> "😰"
        "tired" -> "😴"
        "frustrated" -> "😠"
        else -> "😐"
    }
}

// Data classes
data class MoodEntry(
    val mood: String,
    val note: String,
    val timestamp: Long
)