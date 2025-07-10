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

class DreamInterpreterViewModel(
    private val inferenceModel: InferenceModel,
    private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _interpretation = MutableStateFlow("")
    val interpretation: StateFlow<String> = _interpretation.asStateFlow()

    private val _isInputEnabled = MutableStateFlow(true)
    val isInputEnabled: StateFlow<Boolean> = _isInputEnabled.asStateFlow()

    private val _dreamHistory = MutableStateFlow<List<DreamEntry>>(emptyList())
    val dreamHistory: StateFlow<List<DreamEntry>> = _dreamHistory.asStateFlow()

    private val dreamStorage = DreamStorage.getInstance(context)
    private val userProfile = UserProfile.getInstance(context)

    init {
        loadDreamHistory()
    }

    private fun loadDreamHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _dreamHistory.value = dreamStorage.getAllDreamEntries()
        }
    }

    private val systemPrompt = """You are an AI dream interpreter and wellness companion. You help people understand their dreams through psychological insights, symbolism, and emotional connections.

Your approach to dream interpretation:
• Provide thoughtful, balanced interpretations without claiming absolute truth
• Consider multiple possible meanings and perspectives
• Connect dreams to common psychological themes and emotions
• Avoid superstitious or overly mystical interpretations
• Focus on personal growth and self-reflection
• Be supportive and encouraging
• Keep responses comprehensive but readable (3-4 paragraphs)
• Include practical questions for self-reflection

Provide insightful dream interpretation:"""

    fun interpretDream(dreamDescription: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _isInputEnabled.value = false
                _interpretation.value = ""

                val fullPrompt = """$systemPrompt

Dream description: "$dreamDescription"

Please provide a thoughtful interpretation of this dream, considering:
1. Common symbolic meanings of the elements
2. Possible emotional or psychological significance
3. How it might relate to the dreamer's waking life
4. Questions for further reflection

Response:"""

                var fullResponse = ""
                val responseJob = inferenceModel.generateResponseAsync(fullPrompt) { partialResult, done ->
                    if (partialResult.isNotEmpty()) {
                        fullResponse += partialResult
                        _interpretation.value = fullResponse

                        if (_isLoading.value) {
                            _isLoading.value = false
                        }
                    }

                    if (done) {
                        _isLoading.value = false
                        _isInputEnabled.value = true

                        // Save dream entry
                        saveDreamEntry(dreamDescription, fullResponse)
                    }
                }

                responseJob.get()

            } catch (e: Exception) {
                _interpretation.value = """I'm having trouble interpreting your dream right now. Here are some general insights about dreams:

**Common Dream Themes:**
Dreams often reflect our daily experiences, emotions, and subconscious thoughts. They can help us process feelings, solve problems, or explore our fears and desires.

**Reflection Questions:**
• How did you feel during the dream?
• What emotions lingered after waking?
• Do any elements remind you of recent experiences?
• What might your subconscious be trying to tell you?

**Remember:** Dreams are highly personal. The most meaningful interpretation is often the one that resonates with you. Consider keeping a dream journal to track patterns over time."""

                _isLoading.value = false
                _isInputEnabled.value = true
            }
        }
    }

    private fun saveDreamEntry(description: String, interpretation: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dreamEntry = DreamEntry(
                description = description,
                interpretation = interpretation,
                timestamp = System.currentTimeMillis()
            )

            dreamStorage.saveDreamEntry(dreamEntry)

            // Update user profile
            userProfile.addTopic("dreams")
            userProfile.saveProfile(context)

            // Reload history
            loadDreamHistory()
        }
    }

    companion object {
        fun getFactory(context: Context) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val inferenceModel = InferenceModel.getInstance(context)
                return DreamInterpreterViewModel(inferenceModel, context) as T
            }
        }
    }
}