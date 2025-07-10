package com.google.mediapipe.examples.llminference

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MoodTrackerViewModel(
    private val inferenceModel: InferenceModel,
    private val context: Context
) : ViewModel() {

    private val _moodHistory = MutableStateFlow<List<MoodEntry>>(emptyList())
    val moodHistory: StateFlow<List<MoodEntry>> = _moodHistory.asStateFlow()

    private val _aiInsights = MutableStateFlow("")
    val aiInsights: StateFlow<String> = _aiInsights.asStateFlow()

    private val _isLoadingInsights = MutableStateFlow(false)
    val isLoadingInsights: StateFlow<Boolean> = _isLoadingInsights.asStateFlow()

    private val userProfile = UserProfile.getInstance(context)
    private val moodStorage = MoodStorage.getInstance(context)

    fun loadMoodHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _moodHistory.value = moodStorage.getAllMoodEntries()
        }
    }

    fun saveMood(mood: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = MoodEntry(
                mood = mood,
                note = note,
                timestamp = System.currentTimeMillis()
            )

            moodStorage.saveMoodEntry(entry)

            // Update user profile for compatibility
            userProfile.updateMood(mood)
            if (note.isNotEmpty()) {
                userProfile.addTopic(note)
            }
            userProfile.saveProfile(context)

            // Reload history
            loadMoodHistory()
        }
    }

    fun clearMoodHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            moodStorage.clearAllMoodEntries()
            userProfile.clearProfile(context)
            _moodHistory.value = emptyList()
            _aiInsights.value = ""
        }
    }

    fun generateMoodInsights() {
        viewModelScope.launch(Dispatchers.IO) {
            val history = _moodHistory.value
            if (history.isEmpty()) return@launch

            try {
                _isLoadingInsights.value = true
                _aiInsights.value = ""

                val moodSummary = analyzeMoodHistory(history)

                val systemPrompt = """You are a supportive wellness AI analyzing mood patterns. Provide encouraging, actionable insights based on the user's mood history.

Mood Analysis Data:
$moodSummary

Provide insights including:
1. Patterns you notice in their mood trends
2. Positive observations and progress
3. Practical suggestions for maintaining good moods
4. Gentle recommendations for challenging periods
5. Encouragement and validation

Keep the tone supportive, hopeful, and focused on growth. Avoid clinical language or diagnosing. Focus on empowerment and self-care strategies."""

                val prompt = """$systemPrompt

Based on this mood history, provide supportive insights and practical wellness recommendations:"""

                val responseJob = inferenceModel.generateResponseAsync(prompt) { partialResult, done ->
                    if (partialResult.isNotEmpty()) {
                        _aiInsights.value = _aiInsights.value + partialResult

                        if (_isLoadingInsights.value) {
                            _isLoadingInsights.value = false
                        }
                    }

                    if (done) {
                        _isLoadingInsights.value = false
                    }
                }

                responseJob.get()

            } catch (e: Exception) {
                _aiInsights.value = """I'm having trouble analyzing your mood data right now, but I can see you're taking positive steps by tracking your feelings! 

**General Mood Wellness Tips:**
• **Celebrate small wins** - Notice and appreciate positive moments each day
• **Practice self-compassion** - Be kind to yourself during challenging times
• **Stay connected** - Reach out to friends, family, or support when needed
• **Maintain routines** - Regular sleep, exercise, and meals support emotional balance
• **Mindful moments** - Take brief pauses throughout the day to check in with yourself

Remember, mood fluctuations are completely normal. The fact that you're tracking and reflecting on your emotions shows great self-awareness and commitment to your wellbeing!"""

                _isLoadingInsights.value = false
            }
        }
    }

    private fun analyzeMoodHistory(history: List<MoodEntry>): String {
        val recentEntries = history.takeLast(30) // Last 30 entries
        val moodCounts = recentEntries.groupingBy { it.mood }.eachCount()
        val totalEntries = recentEntries.size

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val startDate = if (recentEntries.isNotEmpty()) dateFormat.format(Date(recentEntries.first().timestamp)) else "N/A"
        val endDate = if (recentEntries.isNotEmpty()) dateFormat.format(Date(recentEntries.last().timestamp)) else "N/A"

        // Calculate trends
        val recentWeek = recentEntries.takeLast(7)
        val previousWeek = recentEntries.dropLast(7).takeLast(7)

        val recentPositive = recentWeek.count { it.mood in listOf("happy", "calm") }
        val previousPositive = previousWeek.count { it.mood in listOf("happy", "calm") }

        // Common notes/themes
        val allNotes = recentEntries.mapNotNull { entry ->
            if (entry.note.isNotBlank()) entry.note else null
        }

        return """
        **Mood Tracking Summary:**
        - Total entries: $totalEntries (from $startDate to $endDate)
        - Most common mood: ${moodCounts.maxByOrNull { it.value }?.key?.capitalize() ?: "N/A"}
        
        **Mood Distribution:**
        ${moodCounts.entries.joinToString("\n") { "- ${it.key.capitalize()}: ${it.value} times (${(it.value * 100 / totalEntries)}%)" }}
        
        **Recent Trends:**
        - Positive moods this week: $recentPositive/7
        - Positive moods previous week: $previousPositive/7
        - Trend: ${if (recentPositive >= previousPositive) "Stable or improving" else "Some challenges lately"}
        
        **User Notes & Themes:**
        ${if (allNotes.isNotEmpty()) allNotes.takeLast(5).joinToString("\n") { "- \"$it\"" } else "No detailed notes provided"}
        """.trimIndent()
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                return MoodTrackerViewModel(inferenceModel, context) as T
            }
        }
    }
}