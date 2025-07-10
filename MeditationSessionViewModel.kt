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
            }

            if (_timeRemaining.value <= 0) {
                moveToNextStep()
            }
        }
    }

    private fun moveToNextStep() {
        currentStepIndexValue++
        _currentStepIndex.value = currentStepIndexValue

        if (currentStepIndexValue < meditationSteps.size) {
            val nextStep = meditationSteps[currentStepIndexValue]
            _currentStep.value = nextStep
            _timeRemaining.value = nextStep.durationSeconds

            // Speak the new step guidance
            if (_ttsEnabled.value && isTtsReady) {
                speakGuidance(nextStep.guidance)
            }

            if (_isPlaying.value) {
                startTimer()
            }

            Log.d(TAG, "Moved to step ${currentStepIndexValue + 1}/${meditationSteps.size}: ${nextStep.title}")
        } else {
            completeSession()
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

    private fun loadCustomMeditationSteps(sessionId: String): List<MeditationStep> {
        try {
            val prefs = context.getSharedPreferences("custom_meditations", Context.MODE_PRIVATE)
            val totalSteps = prefs.getInt("${sessionId}_total_steps", 0)

            Log.d(TAG, "Loading custom meditation $sessionId with $totalSteps steps")

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

                    Log.d(TAG, "Loaded step ${i + 1}: $title (${duration}s)")
                    Log.d(TAG, "Step guidance: ${guidance.take(50)}...")
                }

                return steps
            } else {
                Log.w(TAG, "No steps found for custom session $sessionId")
                Log.d(TAG, "Available keys in SharedPreferences:")
                prefs.all.keys.filter { it.startsWith(sessionId) }.forEach { key ->
                    Log.d(TAG, "Key: $key = ${prefs.all[key]}")
                }
                return createFallbackCustomSession()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom meditation steps for $sessionId", e)
            return createFallbackCustomSession()
        }
    }

    private fun createFallbackCustomSession(): List<MeditationStep> {
        return listOf(
            MeditationStep(
                title = "Custom Meditation Beginning",
                description = "Starting your personalized session",
                guidance = "Welcome to your custom meditation. Find a comfortable position and take a few deep breaths.",
                durationSeconds = 120
            ),
            MeditationStep(
                title = "Mindful Breathing",
                description = "Focusing on your breath",
                guidance = "Turn your attention to your natural breathing. Notice each breath as it flows in and out.",
                durationSeconds = 240
            ),
            MeditationStep(
                title = "Body Awareness",
                description = "Connecting with your body",
                guidance = "Scan through your body, noticing any sensations with gentle curiosity.",
                durationSeconds = 300
            ),
            MeditationStep(
                title = "Peaceful Presence",
                description = "Resting in awareness",
                guidance = "Simply be present. Allow thoughts and feelings to come and go naturally.",
                durationSeconds = 240
            ),
            MeditationStep(
                title = "Closing Integration",
                description = "Completing your practice",
                guidance = "Appreciate this time you've given yourself. Slowly return your awareness to your surroundings.",
                durationSeconds = 120
            )
        )
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