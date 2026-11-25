package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReadingLanguage {
    ENGLISH,
    FILIPINO,
}

class ReadingTtsManager(context: Context) {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _slowMode = MutableStateFlow(false)
    val slowMode: StateFlow<Boolean> = _slowMode.asStateFlow()

    private val _englishVoiceLabel = MutableStateFlow("Philippine English (en-PH)")
    val englishVoiceLabel: StateFlow<String> = _englishVoiceLabel.asStateFlow()

    private val _filipinoVoiceLabel = MutableStateFlow("Filipino (fil-PH)")
    val filipinoVoiceLabel: StateFlow<String> = _filipinoVoiceLabel.asStateFlow()

    private var resolvedEnglishLocale: Locale = Locale("en", "PH")
    private var resolvedFilipinoLocale: Locale = Locale("fil", "PH")

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureListeners()
                detectBestLocales()
                _isReady.value = true
            } else {
                _isReady.value = false
            }
        }
    }

    private fun configureListeners() {
        tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                }
            },
        )
    }

    private fun detectBestLocales() {
        val engine = tts ?: return
        val available = runCatching { engine.availableLanguages }.getOrNull().orEmpty()

        val enPh = Locale("en", "PH")
        val enUs = Locale.US
        val enFallback = Locale.ENGLISH

        resolvedEnglishLocale = when {
            isLocaleSupported(engine, enPh) -> {
                _englishVoiceLabel.value = "Philippine English (en-PH)"
                enPh
            }
            available.any { it.language.equals("en", true) && it.country.equals("PH", true) } -> {
                _englishVoiceLabel.value = "Philippine English (en-PH)"
                enPh
            }
            isLocaleSupported(engine, enUs) -> {
                _englishVoiceLabel.value = "English (PH fallback: en-US)"
                enUs
            }
            else -> {
                _englishVoiceLabel.value = "English (Default)"
                enFallback
            }
        }

        val filPh = Locale("fil", "PH")
        val tlPh = Locale("tl", "PH")
        val fil = Locale("fil")
        val tl = Locale("tl")

        resolvedFilipinoLocale = when {
            isLocaleSupported(engine, filPh) -> {
                _filipinoVoiceLabel.value = "Filipino (fil-PH)"
                filPh
            }
            isLocaleSupported(engine, tlPh) -> {
                _filipinoVoiceLabel.value = "Filipino / Tagalog (tl-PH)"
                tlPh
            }
            isLocaleSupported(engine, fil) -> {
                _filipinoVoiceLabel.value = "Filipino (fil)"
                fil
            }
            isLocaleSupported(engine, tl) -> {
                _filipinoVoiceLabel.value = "Tagalog (tl)"
                tl
            }
            else -> {
                _filipinoVoiceLabel.value = "Filipino (PH phonetic fallback)"
                resolvedEnglishLocale
            }
        }
    }

    private fun isLocaleSupported(engine: TextToSpeech, locale: Locale): Boolean {
        val result = engine.isLanguageAvailable(locale)
        return result == TextToSpeech.LANG_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    fun toggleSlowMode() {
        _slowMode.value = !_slowMode.value
    }

    fun setSlowMode(slow: Boolean) {
        _slowMode.value = slow
    }

    fun speak(
        text: String,
        language: ReadingLanguage,
        forceSlow: Boolean = false,
    ) {
        val engine = tts ?: return
        if (!_isReady.value || text.isBlank()) return

        val targetLocale = when (language) {
            ReadingLanguage.ENGLISH -> resolvedEnglishLocale
            ReadingLanguage.FILIPINO -> resolvedFilipinoLocale
        }
        engine.language = targetLocale
        val rate = if (forceSlow || _slowMode.value) 0.72f else 0.92f
        engine.setSpeechRate(rate)
        engine.setPitch(1.02f)

        val utteranceId = "aral_${System.currentTimeMillis()}"
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }
}
