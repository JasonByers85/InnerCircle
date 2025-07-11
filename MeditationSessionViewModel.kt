package com.google.mediapipe.examples.llminference

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class MeditationSessionViewModel(
    private val context: Context,
    private val meditationType: String
) : ViewModel() {

    private val _sessionState = MutableStateFlow(MeditationSessionState.PREPARING)
    val sessionState: StateFlow<MeditationSessionState> = _sessionState.asStateFlow()

    private val _currentStep = MutableStateFlow(MeditationStep("", "", "", 0))
    val currentStep: StateFlow<MeditationStep> = _currentStep.asStateFlow()

    private val _timeRemaining = MutableStateFlow(0)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _totalTimeRemaining = MutableStateFlow(0)
    val totalTimeRemaining: StateFlow<Int> = _totalTimeRemaining.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _backgroundSound = MutableStateFlow(BackgroundSound.NONE)
    val backgroundSound: StateFlow<BackgroundSound> = _backgroundSound.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _binauralTone = MutableStateFlow(BinauralTone.NONE)
    val binauralTone: StateFlow<BinauralTone> = _binauralTone.asStateFlow()

    private val _binauralEnabled = MutableStateFlow(false)
    val binauralEnabled: StateFlow<Boolean> = _binauralEnabled.asStateFlow()

    private val _ttsEnabled = MutableStateFlow(true)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _totalSteps = MutableStateFlow(1)
    val totalSteps: StateFlow<Int> = _totalSteps.asStateFlow()

    // For custom meditation sequential generation
    private val _isGeneratingNextStep = MutableStateFlow(false)
    val isGeneratingNextStep: StateFlow<Boolean> = _isGeneratingNextStep.asStateFlow()

    private var customMeditationConfig: CustomMeditationConfig? = null
    private var generatedStepsCount = 0

    private val meditationSettings = MeditationSettings.getInstance(context)
    private val audioManager = MeditationAudioManager(context)

    private var timerJob: Job? = null
    private var meditationSteps: List<MeditationStep> = emptyList()
    private var currentStepIndexValue = 0
    private var totalSessionDuration = 0

    // TTS Setup
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingGuidanceText: String? = null // Store text if TTS not ready yet

    private val TAG = "MeditationSessionVM"

    init {
        setupMeditation()
        loadSettings()
        initializeTTS()
    }

    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported for TTS")
                        // Fallback to English
                        tts.setLanguage(Locale.ENGLISH)
                    }

                    // Apply saved TTS settings properly
                    tts.setPitch(meditationSettings.getTtsPitch())
                    tts.setSpeechRate(meditationSettings.getTtsSpeed())

                    // Store TTS volume level in audio manager for retrieval
                    audioManager.setTtsVolume(meditationSettings.getTtsVolume())

                    // Set saved voice if available
                    val savedVoice = meditationSettings.getTtsVoice()
                    if (savedVoice.isNotEmpty()) {
                        tts.voices?.find { it.name == savedVoice }?.let { voice ->
                            tts.voice = voice
                            Log.d(TAG, "Applied saved voice: ${voice.name}")
                        }
                    }

                    isTtsReady = true
                    Log.d(TAG, "TTS initialized successfully")

                    // Speak any pending guidance text
                    pendingGuidanceText?.let { text ->
                        speakGuidance(text)
                        pendingGuidanceText = null
                    }
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    private fun setupMeditation() {
        Log.d(TAG, "Setting up meditation for type: $meditationType")
        
        if (meditationType.startsWith("custom_ai_")) {
            setupCustomMeditation()
        } else {
            setupRegularMeditation()
        }
    }
    
    private fun setupCustomMeditation() {
        // Load custom meditation configuration
        customMeditationConfig = loadCustomMeditationConfig()
        customMeditationConfig?.let { config ->
            Log.d(TAG, "DEBUG: Loaded custom config - sessionId: ${config.sessionId}, focus: '${config.focus}', mood: '${config.mood}', experience: ${config.experience}")
            _totalSteps.value = config.totalSteps
            
            // Clear any existing generic/fallback content to force fresh generation
            clearGenericContent()
            
            // Try to load first step, if it doesn't exist, generate it immediately
            val existingStep = loadCustomMeditationStep(0)
            Log.d(TAG, "DEBUG: Existing first step found: ${existingStep != null}")
            
            val firstStep = existingStep ?: run {
                Log.d(TAG, "DEBUG: First step not found, generating it now with preferences")
                Log.d(TAG, "DEBUG: User focus: '${config.focus}', mood: '${config.mood}', experience: ${config.experience}")
                
                // Set generating state and show better description
                _isGeneratingNextStep.value = true
                _currentStep.value = MeditationStep(
                    title = "Generating Your Personalized Practice...",
                    description = "Creating custom meditation based on your preferences: Focus on ${config.focus.ifEmpty { "mindfulness" }}, Current mood: ${config.mood.ifEmpty { "neutral" }}, Experience: ${config.experience}",
                    guidance = "Please wait while we create your personalized meditation experience tailored to your focus on ${config.focus.ifEmpty { "mindfulness" }}...",
                    durationSeconds = config.stepDuration
                )
                
                // Generate the first step immediately
                generateFirstCustomStepWithTimeout(config)
                
                // Return the generating placeholder
                _currentStep.value
            }
            
            if (existingStep != null) {
                _currentStep.value = firstStep
                Log.d(TAG, "DEBUG: Using existing first step: '${firstStep.title}' - '${firstStep.guidance.take(50)}...'")
            }
            
            _timeRemaining.value = firstStep.durationSeconds
            _totalTimeRemaining.value = config.totalDuration * 60 // Convert to seconds
            _currentStepIndex.value = 0
            _sessionState.value = MeditationSessionState.ACTIVE
            
            // Initialize generation counter - if we generated first step, count it
            generatedStepsCount = if (existingStep != null) 1 else 0
            Log.d(TAG, "DEBUG: Generated steps count: $generatedStepsCount")
            
            Log.d(TAG, "Custom meditation setup: ${config.totalSteps} steps, ${config.totalDuration} minutes")
        } ?: run {
            // If config loading fails, fall back to a basic meditation
            Log.w(TAG, "Failed to load custom meditation config, falling back to basic meditation")
            setupBasicMeditation()
        }
    }
    
    private fun setupBasicMeditation() {
        val basicSteps = listOf(
            MeditationStep(
                title = "Basic Meditation",
                description = "Simple mindfulness practice",
                guidance = "Focus gently on your breath and be present in this moment.",
                durationSeconds = 300
            )
        )
        
        meditationSteps = basicSteps
        totalSessionDuration = basicSteps.sumOf { it.durationSeconds }
        
        _currentStep.value = basicSteps[0]
        _timeRemaining.value = basicSteps[0].durationSeconds
        _totalTimeRemaining.value = totalSessionDuration
        _currentStepIndex.value = 0
        _totalSteps.value = basicSteps.size
        _sessionState.value = MeditationSessionState.ACTIVE
        
        Log.d(TAG, "Basic meditation fallback setup complete")
    }
    
    private fun setupRegularMeditation() {
        meditationSteps = getMeditationSteps(meditationType)
        if (meditationSteps.isNotEmpty()) {
            // Calculate total session duration
            totalSessionDuration = meditationSteps.sumOf { it.durationSeconds }

            _currentStep.value = meditationSteps[0]
            _timeRemaining.value = meditationSteps[0].durationSeconds
            _totalTimeRemaining.value = totalSessionDuration // Show total time initially
            _currentStepIndex.value = 0
            _totalSteps.value = meditationSteps.size // Set total steps count
            _sessionState.value = MeditationSessionState.ACTIVE

            Log.d(TAG, "Meditation setup complete: ${meditationSteps.size} steps, total duration: $totalSessionDuration seconds")
            Log.d(TAG, "First step: ${meditationSteps[0].title}")
        } else {
            Log.e(TAG, "No meditation steps found for type: $meditationType")
        }
    }

    private fun loadSettings() {
        _backgroundSound.value = meditationSettings.getBackgroundSound()
        _soundEnabled.value = meditationSettings.isSoundEnabled()
        _binauralTone.value = meditationSettings.getBinauralTone()
        _binauralEnabled.value = meditationSettings.isBinauralEnabled()
        _ttsEnabled.value = meditationSettings.isTtsEnabled()

        // Apply volume settings to audio manager
        audioManager.setVolume(meditationSettings.getVolume())
        audioManager.setBinauralVolume(meditationSettings.getBinauralVolume())
        audioManager.setTtsVolume(meditationSettings.getTtsVolume())
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pauseSession()
        } else {
            startSession()
        }
    }

    private fun startSession() {
        _isPlaying.value = true
        _sessionState.value = MeditationSessionState.ACTIVE

        if (_soundEnabled.value) {
            audioManager.playBackgroundSound(_backgroundSound.value)
        }

        if (_binauralEnabled.value) {
            audioManager.playBinauralTone(_binauralTone.value)
        }

        // Speak the current step guidance if TTS is enabled
        if (_ttsEnabled.value && isTtsReady) {
            speakGuidance(_currentStep.value.guidance)
        }

        startTimer()
    }

    private fun pauseSession() {
        _isPlaying.value = false
        _sessionState.value = MeditationSessionState.PAUSED
        timerJob?.cancel()
        audioManager.pauseBackgroundSound()
        audioManager.pauseBinauralTone()
        textToSpeech?.stop() // Stop any ongoing speech
    }

    fun stopSession() {
        _isPlaying.value = false
        _sessionState.value = MeditationSessionState.COMPLETED
        timerJob?.cancel()
        audioManager.stopBackgroundSound()
        audioManager.stopBinauralTone()
        textToSpeech?.stop()
        
        // Reset custom meditation state
        customMeditationConfig = null
        generatedStepsCount = 0
        _isGeneratingNextStep.value = false
    }

    fun toggleSound() {
        val newSoundEnabled = !_soundEnabled.value
        _soundEnabled.value = newSoundEnabled
        meditationSettings.setSoundEnabled(newSoundEnabled)

        if (newSoundEnabled && _isPlaying.value) {
            audioManager.playBackgroundSound(_backgroundSound.value)
        } else {
            audioManager.stopBackgroundSound()
        }
    }

    fun toggleBinaural() {
        val newBinauralEnabled = !_binauralEnabled.value
        _binauralEnabled.value = newBinauralEnabled
        meditationSettings.setBinauralEnabled(newBinauralEnabled)

        if (newBinauralEnabled && _isPlaying.value) {
            audioManager.playBinauralTone(_binauralTone.value)
            Log.d(TAG, "Playing binaural tone: ${_binauralTone.value.displayName}")
        } else {
            audioManager.stopBinauralTone()
        }
    }

    fun toggleTts() {
        val newTtsEnabled = !_ttsEnabled.value
        _ttsEnabled.value = newTtsEnabled
        meditationSettings.setTtsEnabled(newTtsEnabled)

        if (!newTtsEnabled) {
            textToSpeech?.stop()
        }
    }

    fun setBackgroundSound(sound: BackgroundSound) {
        _backgroundSound.value = sound
        meditationSettings.setBackgroundSound(sound)

        if (_soundEnabled.value && _isPlaying.value) {
            audioManager.playBackgroundSound(sound)
        }
    }

    fun setBinauralTone(tone: BinauralTone) {
        _binauralTone.value = tone
        meditationSettings.setBinauralTone(tone)

        if (_binauralEnabled.value && _isPlaying.value) {
            audioManager.playBinauralTone(tone)
        }
    }

    // --- Real-time audio/TTS controls for live session settings ---
    fun setBackgroundVolume(volume: Float) {
        meditationSettings.setVolume(volume)
        audioManager.setVolume(volume)
    }

    fun setBinauralVolume(volume: Float) {
        meditationSettings.setBinauralVolume(volume)
        audioManager.setBinauralVolume(volume)
    }

    fun setTtsVolume(volume: Float) {
        meditationSettings.setTtsVolume(volume)
        // Store volume level in audio manager for retrieval
        audioManager.setTtsVolume(volume)
        Log.d(TAG, "TTS volume changed to $volume")
    }

    fun setTtsSpeed(speed: Float) {
        meditationSettings.setTtsSpeed(speed)
        // Apply speed change immediately to current TTS instance
        textToSpeech?.setSpeechRate(speed)
        Log.d(TAG, "TTS speed changed to $speed")
    }

    fun setTtsPitch(pitch: Float) {
        meditationSettings.setTtsPitch(pitch)
        // Apply pitch change immediately to current TTS instance
        textToSpeech?.setPitch(pitch)
        Log.d(TAG, "TTS pitch changed to $pitch")
    }

    fun setTtsVoice(voiceName: String) {
        meditationSettings.setTtsVoice(voiceName)
        // Apply voice change immediately to current TTS instance
        textToSpeech?.let { tts ->
            tts.voices?.find { it.name == voiceName }?.let { voice ->
                tts.voice = voice
                Log.d(TAG, "TTS voice changed to ${voice.name}")
            }
        }
    }

    // Function to restart TTS for settings that require it
    fun restartTtsForSettingsChange() {
        if (isTtsReady) {
            val wasReady = isTtsReady
            isTtsReady = false
            textToSpeech?.stop()
            textToSpeech?.shutdown()

            // Brief delay before reinitializing
            viewModelScope.launch {
                delay(100)
                initializeTTS()
            }
        }
    }

    // Get current volume levels for settings dialog
    fun getCurrentBackgroundVolume(): Float = meditationSettings.getVolume()
    fun getCurrentBinauralVolume(): Float = meditationSettings.getBinauralVolume()
    fun getCurrentTtsVolume(): Float = meditationSettings.getTtsVolume()

    private fun speakGuidance(text: String) {
        if (_ttsEnabled.value && text.isNotEmpty()) {
            if (!isTtsReady) {
                // Store text to speak later when TTS is ready
                pendingGuidanceText = text
                Log.d(TAG, "TTS not ready, storing guidance text for later")
                return
            }

            textToSpeech?.let { tts ->
                // Apply current settings
                tts.setPitch(meditationSettings.getTtsPitch())
                tts.setSpeechRate(meditationSettings.getTtsSpeed())

                // Apply saved voice
                val savedVoice = meditationSettings.getTtsVoice()
                if (savedVoice.isNotEmpty()) {
                    tts.voices?.find { it.name == savedVoice }?.let { voice ->
                        tts.voice = voice
                    }
                }

                // Create bundle with volume parameter
                val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, meditationSettings.getTtsVolume())
                }

                tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "guidance")
                Log.d(TAG, "Speaking guidance at volume ${meditationSettings.getTtsVolume()}: ${text.take(50)}...")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemaining.value > 0 && _isPlaying.value) {
                delay(1000)
                _timeRemaining.value -= 1
                _totalTimeRemaining.value -= 1
                
                // For custom meditations, generate next step when 60 seconds remaining
                if (meditationType.startsWith("custom_ai_") && _timeRemaining.value == 60) {
                    checkAndGenerateNextCustomStep()
                }
            }

            if (_timeRemaining.value <= 0) {
                moveToNextStep()
            }
        }
    }

    private fun moveToNextStep() {
        currentStepIndexValue++
        _currentStepIndex.value = currentStepIndexValue

        if (currentStepIndexValue < (_totalSteps.value ?: meditationSteps.size)) {
            val nextStep = if (meditationType.startsWith("custom_ai_")) {
                // For custom meditation, try to load or generate next step
                loadCustomMeditationStep(currentStepIndexValue) ?: run {
                    // Generate next step if it doesn't exist
                    checkAndGenerateNextCustomStep()
                    createPlaceholderStep(currentStepIndexValue, customMeditationConfig!!)
                }
            } else {
                meditationSteps[currentStepIndexValue]
            }
            
            _currentStep.value = nextStep
            _timeRemaining.value = nextStep.durationSeconds

            // Speak the new step guidance
            if (_ttsEnabled.value && isTtsReady) {
                speakGuidance(nextStep.guidance)
            }

            if (_isPlaying.value) {
                startTimer()
            }

            Log.d(TAG, "Moved to step ${currentStepIndexValue + 1}/${_totalSteps.value}: ${nextStep.title}")
        } else {
            completeSession()
        }
    }
    
    private fun checkAndGenerateNextCustomStep() {
        customMeditationConfig?.let { config ->
            val nextStepToGenerate = generatedStepsCount + 1
            if (nextStepToGenerate < config.totalSteps && currentStepIndexValue >= nextStepToGenerate - 1) {
                _isGeneratingNextStep.value = true
                Log.d(TAG, "Generating step $nextStepToGenerate for custom meditation")
                generateNextCustomStep(config, nextStepToGenerate)
            }
        }
    }
    
    private fun generateNextCustomStep(config: CustomMeditationConfig, stepNumber: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Generating step $stepNumber for custom meditation")
                
                // Get the inference model instance
                val inferenceModel = InferenceModel.getInstance(context)
                
                // Build prompt for this step
                val stepType = when {
                    stepNumber == 0 -> "opening and settling"
                    stepNumber == config.totalSteps - 1 -> "closing and integration"
                    stepNumber == 1 -> "initial focus and grounding"
                    stepNumber == config.totalSteps - 2 -> "deepening and preparation for completion"
                    else -> "main practice"
                }
                
                val prompt = """Create a ${stepType} meditation step (${stepNumber + 1} of ${config.totalSteps}) for a ${config.totalDuration}-minute meditation.

USER'S MEDITATION FOCUS:
- Primary focus: ${config.focus.ifEmpty { "general relaxation and mindfulness" }}
- Current mood: ${config.mood.ifEmpty { "calm and centered" }}
- Experience level: ${config.experience}
- Step duration: ${config.stepDuration} seconds

This is step ${stepNumber + 1} in their personalized journey. Create guidance that builds on their chosen focus area and continues their meditation flow naturally.

Title: [Brief step name related to their focus]
Guidance: [2-3 sentences of meditation instruction that incorporates their focus area: ${config.focus}]"""
                
                var fullResponse = ""
                
                val responseFuture = inferenceModel.generateResponseAsync(prompt) { partialResult, done ->
                    if (partialResult != null) {
                        fullResponse += partialResult
                    }
                    
                    if (done) {
                        try {
                            // Parse the response
                            val lines = fullResponse.trim().split("\n")
                            var title = "Meditation Step ${stepNumber + 1}"
                            var guidance = "Focus on your breath and be present in this moment."
                            
                            for (line in lines) {
                                when {
                                    line.startsWith("Title:", ignoreCase = true) -> {
                                        title = line.substringAfter(":").trim()
                                    }
                                    line.startsWith("Guidance:", ignoreCase = true) -> {
                                        guidance = line.substringAfter(":").trim()
                                    }
                                }
                            }
                            
                            val newStep = MeditationStep(
                                title = title,
                                description = "Custom meditation step",
                                guidance = guidance,
                                durationSeconds = config.stepDuration
                            )
                            
                            // Store the generated step
                            storeGeneratedStep(config.sessionId, stepNumber, newStep)
                            generatedStepsCount++
                            
                            Log.d(TAG, "Generated step $stepNumber: $title")
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing step response", e)
                            // Create fallback step
                            val fallbackStep = createFallbackMeditationStep(stepNumber, config.stepDuration)
                            storeGeneratedStep(config.sessionId, stepNumber, fallbackStep)
                            generatedStepsCount++
                        }
                        
                        _isGeneratingNextStep.value = false
                    }
                }
                
                // Wait for completion
                responseFuture.get()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error generating next custom step", e)
                // Create fallback step
                val fallbackStep = createFallbackMeditationStep(stepNumber, config.stepDuration)
                storeGeneratedStep(config.sessionId, stepNumber, fallbackStep)
                generatedStepsCount++
                _isGeneratingNextStep.value = false
            }
        }
    }
    
    private fun createFallbackMeditationStep(stepIndex: Int, stepDuration: Int): MeditationStep {
        val fallbackSteps = listOf(
            "Take three deep breaths and settle into your meditation space.",
            "Notice your natural breathing rhythm and follow its gentle flow.",
            "Observe any thoughts that arise and let them pass like clouds in the sky.",
            "Feel the support of the ground beneath you and relax into this moment.",
            "Bring your attention to the present moment with gentle awareness.",
            "Take a moment to appreciate this time you've given yourself for peace.",
            "Breathe deeply and prepare to carry this calm with you."
        )
        
        val guidance = fallbackSteps.getOrNull(stepIndex) ?: fallbackSteps[0]
        
        return MeditationStep(
            title = "Mindful Moment ${stepIndex + 1}",
            description = "Guided meditation practice",
            guidance = guidance,
            durationSeconds = stepDuration
        )
    }
    
    private fun storeGeneratedStep(sessionId: String, stepNumber: Int, step: MeditationStep) {
        try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            Log.d(TAG, "DEBUG: Storing step $stepNumber for session $sessionId")
            Log.d(TAG, "DEBUG: Step details - Title: '${step.title}', Guidance: '${step.guidance.take(100)}...'")
            
            editor.putString("${sessionId}_step_${stepNumber}_title", step.title)
            editor.putString("${sessionId}_step_${stepNumber}_description", step.description)
            editor.putString("${sessionId}_step_${stepNumber}_guidance", step.guidance)
            editor.putInt("${sessionId}_step_${stepNumber}_duration", step.durationSeconds)
            
            // Update generated count
            editor.putInt("${sessionId}_generated_steps", stepNumber + 1)
            
            val success = editor.commit() // Use commit instead of apply for immediate write
            Log.d(TAG, "DEBUG: Storage ${if (success) "successful" else "failed"} for step $stepNumber")
            
        } catch (e: Exception) {
            Log.e(TAG, "DEBUG: Error storing generated step", e)
        }
    }

    private fun completeSession() {
        _sessionState.value = MeditationSessionState.COMPLETED
        _isPlaying.value = false
        audioManager.stopBackgroundSound()
        audioManager.stopBinauralTone()

        // Speak completion message
        if (_ttsEnabled.value && isTtsReady) {
            speakGuidance("Your meditation session is complete. Take a moment to notice how you feel. Well done.")
        }

        // Calculate total duration for session completion
        val totalDuration = meditationSteps.sumOf { it.durationSeconds } / 60 // Convert to minutes
        meditationSettings.recordSessionCompletion(meditationType, totalDuration)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        audioManager.release()
        textToSpeech?.shutdown()
    }

    private fun getMeditationSteps(type: String): List<MeditationStep> {
        Log.d(TAG, "Getting meditation steps for type: $type")
        return when (type) {
            "stress_relief" -> listOf(
                MeditationStep(
                    title = "Welcome",
                    description = "Beginning stress relief meditation",
                    guidance = "Welcome to your stress relief meditation. Find a comfortable position and close your eyes. Take a deep breath in through your nose, and slowly exhale through your mouth.",
                    durationSeconds = 30
                ),
                MeditationStep(
                    title = "Body Scan",
                    description = "Notice areas of tension",
                    guidance = "Now, slowly scan your body from the top of your head down to your toes. Notice any areas of tension or stress. Don't try to change anything, just observe with kindness.",
                    durationSeconds = 120
                ),
                MeditationStep(
                    title = "Breathing Focus",
                    description = "Focus on your natural breath",
                    guidance = "Return your attention to your breath. Count each exhale: one, two, three, up to ten, then start again at one. If your mind wanders, gently bring it back to counting.",
                    durationSeconds = 150
                )
            )

            "focus_boost" -> listOf(
                MeditationStep(
                    title = "Focus Preparation",
                    description = "Preparing your mind for clarity",
                    guidance = "Sit upright with your spine straight but not rigid. Close your eyes and take three deep, cleansing breaths. Feel yourself becoming more alert and present.",
                    durationSeconds = 45
                ),
                MeditationStep(
                    title = "Mindful Awareness",
                    description = "Cultivate present-moment attention",
                    guidance = "Focus on the sensation of breathing at your nostrils. Feel the cool air coming in and the warm air going out. When your mind wanders, gently return to the breath sensation.",
                    durationSeconds = 300
                ),
                MeditationStep(
                    title = "Mental Clarity",
                    description = "Strengthen concentration",
                    guidance = "Visualize a bright, clear light at the center of your forehead. Hold this image steady. This light represents your focused awareness. Let it grow brighter with each breath.",
                    durationSeconds = 135
                )
            )

            "sleep_prep" -> listOf(
                MeditationStep(
                    title = "Sleep Preparation",
                    description = "Preparing your body for rest",
                    guidance = "Lie down comfortably in your bed. Let your body sink into the mattress beneath you. Feel yourself becoming heavy and relaxed with each breath.",
                    durationSeconds = 60
                ),
                MeditationStep(
                    title = "Progressive Relaxation",
                    description = "Release physical tension",
                    guidance = "Starting with your toes, gently tense and then completely relax each muscle group. Move slowly up through your legs, torso, arms, and finally your face. Feel the tension melting away.",
                    durationSeconds = 360
                ),
                MeditationStep(
                    title = "Gentle Drift",
                    description = "Drift toward sleep",
                    guidance = "Breathe naturally and softly. Let your thoughts become like gentle clouds drifting across a peaceful evening sky. Allow yourself to drift toward sleep.",
                    durationSeconds = 180
                )
            )

            "anxiety_ease" -> listOf(
                MeditationStep(
                    title = "Grounding",
                    description = "Find your safe space",
                    guidance = "Place one hand on your chest and one on your belly. Feel the natural rhythm of your breathing. You are safe in this moment. This feeling will pass.",
                    durationSeconds = 60
                ),
                MeditationStep(
                    title = "5-4-3-2-1 Technique",
                    description = "Connect with the present",
                    guidance = "Notice 5 things you can see around you, 4 things you can touch, 3 things you can hear, 2 things you can smell, and 1 thing you can taste. This brings you fully into the present moment.",
                    durationSeconds = 180
                ),
                MeditationStep(
                    title = "Self-Compassion",
                    description = "Cultivate kindness toward yourself",
                    guidance = "Place both hands on your heart. Send kind thoughts to yourself: May I be happy, may I be peaceful, may I be free from suffering. Repeat these words with genuine warmth.",
                    durationSeconds = 180
                )
            )

            "deep_relaxation" -> listOf(
                MeditationStep(
                    title = "Deep Relaxation Welcome",
                    description = "Beginning deep relaxation",
                    guidance = "Welcome to your deep relaxation session. Find a very comfortable position and allow your breathing to naturally slow and deepen. You have all the time you need.",
                    durationSeconds = 120
                ),
                MeditationStep(
                    title = "Progressive Muscle Release",
                    description = "Release tension systematically",
                    guidance = "Starting with your feet, tense and then completely relax each muscle group. Feel the contrast between tension and release as you work your way up through your entire body.",
                    durationSeconds = 240
                ),
                MeditationStep(
                    title = "Breathing Space",
                    description = "Deepen your breath",
                    guidance = "Allow your breath to become even slower and deeper. With each exhale, release any remaining stress or worry. Let your breath become a gentle wave of peace flowing through you.",
                    durationSeconds = 180
                ),
                MeditationStep(
                    title = "Full Body Scan",
                    description = "Complete body awareness",
                    guidance = "Scan your body from head to toe, noticing areas of warmth, coolness, lightness, or heaviness. Simply observe without trying to change anything.",
                    durationSeconds = 240
                ),
                MeditationStep(
                    title = "Inner Peace Visualization",
                    description = "Rest in stillness",
                    guidance = "Imagine yourself in a place of complete peace and safety. Feel the deep tranquility that is always available within you. Rest here for as long as you wish.",
                    durationSeconds = 300
                ),
                MeditationStep(
                    title = "Integration",
                    description = "Gentle return",
                    guidance = "Slowly begin to wiggle your fingers and toes. Take a deep breath and gently open your eyes when you're ready, carrying this peace with you.",
                    durationSeconds = 120
                )
            )

            "mindful_awareness" -> listOf(
                MeditationStep(
                    title = "Mindful Awareness Foundation",
                    description = "Establishing present-moment awareness",
                    guidance = "Sit with dignity and gentle alertness. Bring your full attention to this present moment. Notice the quality of awareness itself - spacious, clear, and naturally peaceful.",
                    durationSeconds = 90
                ),
                MeditationStep(
                    title = "Breath Awareness",
                    description = "Anchoring in the breath",
                    guidance = "Focus gently on your natural breathing. Feel each breath as it flows in and out. When your mind wanders, simply return to the breath with kindness.",
                    durationSeconds = 180
                ),
                MeditationStep(
                    title = "Thought Observation",
                    description = "Watching thoughts mindfully",
                    guidance = "Notice thoughts as they arise and pass away, like clouds moving across the sky of your awareness. Don't engage with the thoughts, just observe them with friendly curiosity.",
                    durationSeconds = 240
                ),
                MeditationStep(
                    title = "Emotion Recognition",
                    description = "Mindful awareness of feelings",
                    guidance = "Notice any emotions or feelings that arise. Welcome them with compassion, neither pushing them away nor holding onto them. Simply observe with gentle awareness.",
                    durationSeconds = 180
                ),
                MeditationStep(
                    title = "Open Awareness",
                    description = "Expanding consciousness",
                    guidance = "Let your awareness expand to include all sensations, sounds, and experiences. Rest in this open, spacious awareness without focusing on any particular thing.",
                    durationSeconds = 210
                )
            )

            "extended_focus" -> listOf(
                MeditationStep(
                    title = "Preparation for Deep Focus",
                    description = "Setting the foundation",
                    guidance = "Settle into a stable, comfortable position. Set your intention to cultivate deep, sustained concentration. Let go of all distractions and concerns.",
                    durationSeconds = 180
                ),
                MeditationStep(
                    title = "Single-Pointed Concentration",
                    description = "Developing laser focus",
                    guidance = "Choose a single point of focus - your breath, a visualization, or a mantra. Maintain unwavering attention on this one object, gently returning whenever you notice wandering.",
                    durationSeconds = 420
                ),
                MeditationStep(
                    title = "Deepening Concentration",
                    description = "Strengthening your focus",
                    guidance = "Your focus is becoming stronger and more stable. Notice how your mind can rest more easily on your chosen object. Deepen this concentrated state.",
                    durationSeconds = 480
                ),
                MeditationStep(
                    title = "Sustained Attention",
                    description = "Maintaining deep focus",
                    guidance = "Rest in this state of concentrated attention. Your mind is calm, clear, and focused. Maintain this awareness with effortless ease.",
                    durationSeconds = 480
                ),
                MeditationStep(
                    title = "Focus Integration",
                    description = "Bringing focus to daily life",
                    guidance = "As you prepare to end this session, set an intention to carry this focused awareness into your daily activities. This concentration is a skill you can access anytime.",
                    durationSeconds = 180
                ),
                MeditationStep(
                    title = "Gentle Completion",
                    description = "Closing with awareness",
                    guidance = "Slowly expand your awareness back to your surroundings. Take a few deep breaths and gently return to normal consciousness, feeling focused and centered.",
                    durationSeconds = 60
                )
            )

            "complete_zen" -> listOf(
                MeditationStep(
                    title = "Zen Preparation",
                    description = "Entering the way of zen",
                    guidance = "Sit in the traditional meditation posture. Your spine is erect like a mountain, your mind clear like the sky. Begin to settle into the timeless practice of zen.",
                    durationSeconds = 180
                ),
                MeditationStep(
                    title = "Just Sitting",
                    description = "The practice of shikantaza",
                    guidance = "Simply sit. Don't try to achieve anything or get anywhere. Just be completely present with whatever arises. This is the essence of zen - pure being.",
                    durationSeconds = 480
                ),
                MeditationStep(
                    title = "Breath of Life",
                    description = "Natural breathing awareness",
                    guidance = "Notice your breath without controlling it. Each breath is a complete universe unto itself. Breathe with the rhythm of existence itself.",
                    durationSeconds = 420
                ),
                MeditationStep(
                    title = "Thoughtless Mind",
                    description = "Resting in no-mind",
                    guidance = "Let thoughts come and go without attachment. Rest in the space between thoughts, in the silence that is your true nature. This is the mind of zen.",
                    durationSeconds = 600
                ),
                MeditationStep(
                    title = "Universal Compassion",
                    description = "Opening the heart",
                    guidance = "Extend boundless compassion to all beings everywhere. Feel your connection to the web of existence. Your peace contributes to the peace of the world.",
                    durationSeconds = 360
                ),
                MeditationStep(
                    title = "Buddha Nature",
                    description = "Recognizing your true self",
                    guidance = "Rest in the recognition of your inherent buddha nature - the awakened awareness that has always been present. This is your original face, before you were born.",
                    durationSeconds = 420
                ),
                MeditationStep(
                    title = "Return to the World",
                    description = "Bringing zen into life",
                    guidance = "As you prepare to return to daily life, remember that zen is not separate from ordinary activities. Carry this awakened awareness into every moment.",
                    durationSeconds = 240
                )
            )

            else -> {
                // Handle custom AI sessions with stored ID format: "custom_ai_timestamp"
                if (type.startsWith("custom_ai_")) {
                    Log.d(TAG, "Loading custom AI session: $type")
                    loadCustomMeditationSteps(type)
                } else if (type == "custom_ai") {
                    // This is the placeholder case - should not happen during actual sessions
                    Log.w(TAG, "Using placeholder for custom_ai - this should not happen in actual sessions")
                    listOf(
                        MeditationStep(
                            title = "AI-Generated Session",
                            description = "Custom meditation experience",
                            guidance = "Please wait while we generate your personalized meditation session...",
                            durationSeconds = 300
                        )
                    )
                } else {
                    // Default fallback
                    Log.w(TAG, "Using default fallback for unknown type: $type")
                    listOf(
                        MeditationStep(
                            title = "Basic Meditation",
                            description = "Simple mindfulness practice",
                            guidance = "Focus gently on your breath. Each time your mind wanders, kindly return your attention to the breath. This is the practice of meditation - returning again and again with patience.",
                            durationSeconds = 300
                        )
                    )
                }
            }
        }
    }

    private fun loadCustomMeditationConfig(): CustomMeditationConfig? {
        return try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            
            Log.d(TAG, "DEBUG: Loading config for meditation type: $meditationType")
            
            val duration = prefs.getInt("${meditationType}_duration", 15)
            val totalSteps = prefs.getInt("${meditationType}_total_steps", 4)
            val stepDuration = prefs.getInt("${meditationType}_step_duration", 225)
            val focus = prefs.getString("${meditationType}_focus", "") ?: ""
            val mood = prefs.getString("${meditationType}_mood", "") ?: ""
            val experience = prefs.getString("${meditationType}_experience", "Beginner") ?: "Beginner"
            
            Log.d(TAG, "DEBUG: Loaded config values:")
            Log.d(TAG, "DEBUG: - Duration: $duration minutes")
            Log.d(TAG, "DEBUG: - Total steps: $totalSteps")
            Log.d(TAG, "DEBUG: - Step duration: $stepDuration seconds")
            Log.d(TAG, "DEBUG: - Focus: '$focus'")
            Log.d(TAG, "DEBUG: - Mood: '$mood'")
            Log.d(TAG, "DEBUG: - Experience: '$experience'")
            
            val config = CustomMeditationConfig(
                sessionId = meditationType,
                totalDuration = duration,
                totalSteps = totalSteps,
                stepDuration = stepDuration,
                focus = focus,
                mood = mood,
                experience = experience
            )
            
            Log.d(TAG, "DEBUG: Successfully created config for session: ${config.sessionId}")
            config
        } catch (e: Exception) {
            Log.e(TAG, "DEBUG: Error loading custom meditation config", e)
            null
        }
    }
    
    private fun loadCustomMeditationStep(stepIndex: Int): MeditationStep? {
        return try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            val title = prefs.getString("${meditationType}_step_${stepIndex}_title", null)
            val description = prefs.getString("${meditationType}_step_${stepIndex}_description", null)
            val guidance = prefs.getString("${meditationType}_step_${stepIndex}_guidance", null)
            val duration = prefs.getInt("${meditationType}_step_${stepIndex}_duration", 0)
            
            Log.d(TAG, "DEBUG: Loading step $stepIndex for session $meditationType")
            Log.d(TAG, "DEBUG: Found - title: ${title != null}, description: ${description != null}, guidance: ${guidance != null}, duration: $duration")
            Log.d(TAG, "DEBUG: Raw stored values:")
            Log.d(TAG, "DEBUG: - Title: '$title'")
            Log.d(TAG, "DEBUG: - Description: '$description'")
            Log.d(TAG, "DEBUG: - Guidance: '$guidance'")
            Log.d(TAG, "DEBUG: - Duration: $duration")
            
            if (title != null && description != null && guidance != null && duration > 0) {
                // Check if this is generic fallback content - if so, treat as not found
                val isGenericContent = guidance.contains("Focus on your breath and be present in this moment") ||
                                     guidance.contains("Focus gently on your breath") ||
                                     title.startsWith("Meditation Step") ||
                                     title == "Basic Meditation"
                
                if (isGenericContent) {
                    Log.d(TAG, "DEBUG: Found generic/fallback content for step $stepIndex, treating as not found to regenerate")
                    Log.d(TAG, "DEBUG: Generic content detected: '$guidance'")
                    return null
                }
                
                val step = MeditationStep(title, description, guidance, duration)
                Log.d(TAG, "DEBUG: Successfully loaded custom step $stepIndex: '$title' - '${guidance.take(100)}...'")
                step
            } else {
                Log.d(TAG, "DEBUG: Step $stepIndex not found or incomplete")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "DEBUG: Error loading custom meditation step $stepIndex", e)
            null
        }
    }
    
    private fun createPlaceholderStep(stepIndex: Int, config: CustomMeditationConfig): MeditationStep {
        val focusText = if (config.focus.isNotEmpty()) " focusing on ${config.focus}" else ""
        val moodText = if (config.mood.isNotEmpty()) " You're feeling ${config.mood} and that's perfectly okay." else ""
        
        return when (stepIndex) {
            0 -> MeditationStep(
                title = "Welcome to Your Practice",
                description = "Beginning your personalized meditation",
                guidance = "Welcome to your custom meditation$focusText.$moodText Take three deep breaths and allow yourself to settle into this moment. Your personalized guidance is being prepared.",
                durationSeconds = config.stepDuration
            )
            else -> MeditationStep(
                title = "Continuing Practice",
                description = "Deepening your meditation",
                guidance = "Continue with your meditation practice$focusText, allowing each breath to bring you deeper into relaxation.",
                durationSeconds = config.stepDuration
            )
        }
    }

    private fun loadCustomMeditationSteps(sessionId: String): List<MeditationStep> {
        try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            val totalSteps = prefs.getInt("${sessionId}_total_steps", 0)

            if (totalSteps > 0) {
                val steps = mutableListOf<MeditationStep>()

                for (i in 0 until totalSteps) {
                    val title = prefs.getString("${sessionId}_step_${i}_title", "Meditation Step ${i + 1}") ?: "Meditation Step ${i + 1}"
                    val description = prefs.getString("${sessionId}_step_${i}_description", "Mindful practice") ?: "Mindful practice"
                    val guidance = prefs.getString("${sessionId}_step_${i}_guidance", "Focus on your breath and be present.") ?: "Focus on your breath and be present."
                    val duration = prefs.getInt("${sessionId}_step_${i}_duration", 300)

                    steps.add(MeditationStep(
                        title = title,
                        description = description,
                        guidance = guidance,
                        durationSeconds = duration
                    ))
                }
                return steps
            } else {
                // Return placeholder steps
                return listOf(
                    MeditationStep(
                        title = "Custom Meditation",
                        description = "AI-generated meditation step",
                        guidance = "Focus on your breath and allow yourself to relax deeply.",
                        durationSeconds = 300
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom meditation steps for $sessionId", e)
            return listOf(
                MeditationStep(
                    title = "Basic Meditation",
                    description = "Simple mindfulness practice",
                    guidance = "Focus gently on your breath and be present in this moment.",
                    durationSeconds = 300
                )
            )
        }
    }

    fun handleCustomMeditationError() {
        Log.w(TAG, "Custom meditation failed, resetting to allow other meditations")
        // Reset all custom meditation state
        customMeditationConfig = null
        generatedStepsCount = 0
        _isGeneratingNextStep.value = false
        _sessionState.value = MeditationSessionState.PREPARING
        
        // Stop any ongoing processes
        timerJob?.cancel()
        audioManager.stopBackgroundSound()
        audioManager.stopBinauralTone()
        textToSpeech?.stop()
        _isPlaying.value = false
    }

    // DEBUG FUNCTIONS - Remove in production
    fun clearCustomMeditationData() {
        try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            // Clear all data for this session
            val allKeys = prefs.all.keys.filter { it.startsWith(meditationType) }
            allKeys.forEach { key ->
                editor.remove(key)
            }
            
            val success = editor.commit()
            Log.d(TAG, "DEBUG: Cleared custom meditation data for $meditationType - Success: $success")
            Log.d(TAG, "DEBUG: Cleared keys: $allKeys")
        } catch (e: Exception) {
            Log.e(TAG, "DEBUG: Error clearing custom meditation data", e)
        }
    }
    
    fun debugPrintStoredData() {
        try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            val allData = prefs.all
            
            Log.d(TAG, "DEBUG: All stored meditation data:")
            allData.forEach { (key, value) ->
                if (key.startsWith(meditationType)) {
                    Log.d(TAG, "DEBUG: $key = $value")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "DEBUG: Error printing stored data", e)
        }
    }

    private fun generateFirstCustomStep(config: CustomMeditationConfig) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "DEBUG: Starting first custom step generation")
                Log.d(TAG, "DEBUG: Config details - Focus: '${config.focus}', Mood: '${config.mood}', Experience: '${config.experience}'")
                
                // Get the inference model instance
                val inferenceModel = InferenceModel.getInstance(context)
                
                // Build a detailed prompt for the first step
                val prompt = """Create an opening meditation step for a ${config.totalDuration}-minute custom meditation focused on ${config.focus}.

USER'S CONTEXT:
- Focus area: ${config.focus.ifEmpty { "general relaxation and mindfulness" }}
- Current mood: ${config.mood.ifEmpty { "ready for meditation" }}
- Experience level: ${config.experience}

Please respond in this exact format:

Title: [A brief, welcoming step name that relates to their focus]
Guidance: [2-3 clear, simple sentences that welcome them and introduce their specific focus area. Keep it conversational and supportive.]

Make it personal and specific to their goal of ${config.focus}."""
                
                Log.d(TAG, "DEBUG: Generated prompt: $prompt")
                
                var fullResponse = ""
                
                Log.d(TAG, "DEBUG: Starting async generation...")
                
                inferenceModel.generateResponseAsync(prompt) { partialResult, done ->
                    if (partialResult != null) {
                        fullResponse += partialResult
                        Log.d(TAG, "DEBUG: Partial response received: ${partialResult.take(50)}...")
                    }
                    
                    if (done) {
                        viewModelScope.launch {
                            Log.d(TAG, "DEBUG: Full response received: $fullResponse")
                            try {
                                // Parse the response - handle multiple formats
                                val lines = fullResponse.trim().split("\n")
                                var title = "Welcome to Your Custom Practice"
                                var guidance = "Welcome to your personalized meditation. Find a comfortable position and let's begin this journey together."
                                
                                // Try to parse different formats
                                for (line in lines) {
                                    when {
                                        // Format: "Title: Something"
                                        line.startsWith("Title:", ignoreCase = true) -> {
                                            title = line.substringAfter(":").trim()
                                            Log.d(TAG, "DEBUG: Parsed title (format 1): '$title'")
                                        }
                                        // Format: "Guidance: Something" 
                                        line.startsWith("Guidance:", ignoreCase = true) -> {
                                            guidance = line.substringAfter(":").trim()
                                            Log.d(TAG, "DEBUG: Parsed guidance (format 1): '$guidance'")
                                        }
                                        // Format: "## Step 1: Something" or "# Something"
                                        line.startsWith("##") || line.startsWith("# ") -> {
                                            val titleText = line.replace("##", "").replace("#", "").trim()
                                            if (titleText.contains(":")) {
                                                title = titleText.substringAfter(":").trim()
                                            } else {
                                                title = titleText
                                            }
                                            Log.d(TAG, "DEBUG: Parsed title (format 2): '$title'")
                                        }
                                        // Format: "**Guidance:** Something"
                                        line.startsWith("**Guidance:**", ignoreCase = true) -> {
                                            guidance = line.substringAfter("**Guidance:**").trim()
                                            Log.d(TAG, "DEBUG: Parsed guidance (format 2): '$guidance'")
                                        }
                                    }
                                }
                                
                                // If we didn't find guidance in the expected format, try to extract the main content
                                if (guidance == "Welcome to your personalized meditation. Find a comfortable position and let's begin this journey together.") {
                                    // Look for the main guidance content after **Guidance:**
                                    val guidanceStart = fullResponse.indexOf("**Guidance:**")
                                    if (guidanceStart != -1) {
                                        val afterGuidance = fullResponse.substring(guidanceStart + "**Guidance:**".length)
                                        // Extract until next major section or end
                                        val nextSection = afterGuidance.indexOf("**")
                                        val mainGuidance = if (nextSection != -1) {
                                            afterGuidance.substring(0, nextSection)
                                        } else {
                                            afterGuidance
                                        }
                                        guidance = mainGuidance.trim().split("\n").firstOrNull()?.trim() ?: guidance
                                        Log.d(TAG, "DEBUG: Extracted main guidance: '$guidance'")
                                    }
                                }
                                
                                val firstStep = MeditationStep(
                                    title = title,
                                    description = "Personalized opening based on your preferences",
                                    guidance = guidance,
                                    durationSeconds = config.stepDuration
                                )
                                
                                Log.d(TAG, "DEBUG: Created first step - Title: '$title', Guidance: '${guidance.take(100)}...'")
                                
                                // Store the generated first step
                                storeGeneratedStep(config.sessionId, 0, firstStep)
                                generatedStepsCount = 1
                                
                                // Update the current step immediately
                                _currentStep.value = firstStep
                                _isGeneratingNextStep.value = false
                                
                                Log.d(TAG, "DEBUG: Successfully updated current step with generated content")
                                
                                // If TTS is enabled and ready, speak the new guidance
                                if (_ttsEnabled.value && isTtsReady && _isPlaying.value) {
                                    speakGuidance(guidance)
                                }
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "DEBUG: Error parsing first step response", e)
                                Log.e(TAG, "DEBUG: Raw response was: $fullResponse")
                                // Create fallback step with user preferences
                                val fallbackStep = createPersonalizedFallbackStep(0, config)
                                storeGeneratedStep(config.sessionId, 0, fallbackStep)
                                _currentStep.value = fallbackStep
                                generatedStepsCount = 1
                                _isGeneratingNextStep.value = false
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "DEBUG: Error generating first custom step", e)
                // Create fallback step with user preferences
                val fallbackStep = createPersonalizedFallbackStep(0, config)
                storeGeneratedStep(config.sessionId, 0, fallbackStep)
                _currentStep.value = fallbackStep
                generatedStepsCount = 1
                _isGeneratingNextStep.value = false
            }
        }
    }
    
    private fun createPersonalizedFallbackStep(stepIndex: Int, config: CustomMeditationConfig): MeditationStep {
        val personalizedGuidance = when (stepIndex) {
            0 -> {
                val focusText = if (config.focus.isNotEmpty()) " focusing on ${config.focus}" else ""
                val moodText = if (config.mood.isNotEmpty()) " You're feeling ${config.mood} and that's perfectly okay." else ""
                "Welcome to your personalized meditation$focusText.$moodText Take three deep breaths and allow yourself to settle into this moment."
            }
            else -> {
                val focusText = if (config.focus.isNotEmpty()) " with ${config.focus}" else ""
                "Continue your meditation practice$focusText. Allow each breath to bring you deeper into relaxation and awareness."
            }
        }
        
        return MeditationStep(
            title = if (stepIndex == 0) "Welcome to Your Practice" else "Continuing Your Journey",
            description = "Personalized meditation step",
            guidance = personalizedGuidance,
            durationSeconds = config.stepDuration
        )
    }

    // Function to clear generic/fallback content to force regeneration
    fun clearGenericContent() {
        try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            Log.d(TAG, "DEBUG: Checking for generic content to clear...")
            
            // Check all steps for this session
            val totalSteps = prefs.getInt("${meditationType}_total_steps", 4)
            var clearedCount = 0
            
            for (i in 0 until totalSteps) {
                val guidance = prefs.getString("${meditationType}_step_${i}_guidance", null)
                val title = prefs.getString("${meditationType}_step_${i}_title", null)
                
                val isGenericContent = guidance?.let { g ->
                    g.contains("Focus on your breath and be present in this moment") ||
                    g.contains("Focus gently on your breath") ||
                    g.contains("gently settle into a comfortable posture")
                } ?: false
                
                val isGenericTitle = title?.let { t ->
                    t.startsWith("Meditation Step") || t == "Basic Meditation"
                } ?: false
                
                if (isGenericContent || isGenericTitle) {
                    Log.d(TAG, "DEBUG: Clearing generic step $i - Title: '$title', Guidance: '${guidance?.take(50)}...'")
                    editor.remove("${meditationType}_step_${i}_title")
                    editor.remove("${meditationType}_step_${i}_description")
                    editor.remove("${meditationType}_step_${i}_guidance")
                    editor.remove("${meditationType}_step_${i}_duration")
                    clearedCount++
                }
            }
            
            if (clearedCount > 0) {
                // Reset generated steps count
                editor.putInt("${meditationType}_generated_steps", 0)
                generatedStepsCount = 0
                
                val success = editor.commit()
                Log.d(TAG, "DEBUG: Cleared $clearedCount generic steps - Success: $success")
            } else {
                Log.d(TAG, "DEBUG: No generic content found to clear")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "DEBUG: Error clearing generic content", e)
        }
    }

    // Test AI model functionality
    fun testAIGeneration() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "DEBUG: Testing AI model generation...")
                val inferenceModel = InferenceModel.getInstance(context)
                
                val testPrompt = "Say hello and confirm you are working properly. Just respond with 'Hello, I am working correctly.'"
                var fullResponse = ""
                
                val responseFuture = inferenceModel.generateResponseAsync(testPrompt) { partialResult, done ->
                    if (partialResult != null) {
                        fullResponse += partialResult
                        Log.d(TAG, "DEBUG: AI Test - Partial: $partialResult")
                    }
                    
                    if (done) {
                        Log.d(TAG, "DEBUG: AI Test - Full response: '$fullResponse'")
                    }
                }
                
                responseFuture.get()
                
            } catch (e: Exception) {
                Log.e(TAG, "DEBUG: AI Test failed", e)
            }
        }
    }

    // Add timeout for AI generation to prevent app hanging
    private fun generateFirstCustomStepWithTimeout(config: CustomMeditationConfig) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "DEBUG: Starting first custom step generation with timeout")
                
                // Set a timeout of 30 seconds
                val timeoutJob = launch {
                    delay(30000) // 30 seconds
                    Log.w(TAG, "DEBUG: AI generation timeout, using fallback")
                    if (_isGeneratingNextStep.value) {
                        val fallbackStep = createPersonalizedFallbackStep(0, config)
                        storeGeneratedStep(config.sessionId, 0, fallbackStep)
                        _currentStep.value = fallbackStep
                        generatedStepsCount = 1
                        _isGeneratingNextStep.value = false
                    }
                }
                
                generateFirstCustomStep(config)
                
                // Cancel timeout if generation completes
                delay(100) // Small delay to let generation start
                while (_isGeneratingNextStep.value && timeoutJob.isActive) {
                    delay(500)
                }
                timeoutJob.cancel()
                
            } catch (e: Exception) {
                Log.e(TAG, "DEBUG: Error in timeout wrapper", e)
            }
        }
    }

    companion object {
        fun getFactory(context: Context, meditationType: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(MeditationSessionViewModel::class.java)) {
                        return MeditationSessionViewModel(context, meditationType) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}