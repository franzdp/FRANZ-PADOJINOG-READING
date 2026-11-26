package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ReadingLanguage {
    ENGLISH,
    FILIPINO,
}

/**
 * Pure-Kotlin speech normalizer so phonics slashes (/m/), syllable hyphens (won-der-ful),
 * reading acronyms (CVCe, KS1), and punctuation sound natural and human in both
 * Philippine English and Filipino/Tagalog.
 */
object ReadingSpeechFormatter {

    private val doubledLetterRegex = Regex(
        """\b(Letter|letter|titik|Titik|letrang|Patinig|patinig|Katinig|katinig|ang)\s+([A-Z])([a-z])\b""",
    )
    private val multiHyphenSyllableRegex = Regex("""\b([A-Za-zñÑ]+)(?:-([A-Za-zñÑ]+)){2,}\b""")
    private val phonemeSlashRegex = Regex("""/(?:([a-zA-ZăĕĭŏŭāēīōūñÑ]+)|-([a-zA-ZñÑ]+))/""")

    private val englishPhonemeSpoken = mapOf(
        "ă" to "short a",
        "ĕ" to "short e",
        "ĭ" to "short i",
        "ŏ" to "short o",
        "ŭ" to "short u",
        "ā" to "long a",
        "ē" to "long e",
        "ī" to "long i",
        "ō" to "long o",
        "ū" to "long u",
        "a" to "ah",
        "e" to "eh",
        "i" to "ih",
        "o" to "aw",
        "u" to "uh",
        "b" to "buh",
        "c" to "kuh",
        "d" to "duh",
        "f" to "fff",
        "g" to "guh",
        "h" to "huh",
        "j" to "juh",
        "k" to "kuh",
        "l" to "lll",
        "m" to "mmm",
        "n" to "nnn",
        "p" to "puh",
        "q" to "kwuh",
        "r" to "rrr",
        "s" to "sss",
        "t" to "tuh",
        "v" to "vvv",
        "w" to "wuh",
        "x" to "ks",
        "y" to "yuh",
        "z" to "zzz",
        "zzz" to "zzz",
        "ks" to "ks",
        "kw" to "kw",
        "sh" to "shh",
        "ch" to "chuh",
        "th" to "th",
        "ng" to "ng",
    )

    private val filipinoPhonemeSpoken = mapOf(
        "a" to "ah",
        "e" to "eh",
        "i" to "ih",
        "o" to "oh",
        "u" to "uh",
        "b" to "ba",
        "d" to "da",
        "g" to "ga",
        "h" to "ha",
        "k" to "ka",
        "l" to "lll",
        "m" to "mmm",
        "n" to "nnn",
        "p" to "pa",
        "r" to "rrr",
        "s" to "sss",
        "t" to "ta",
        "w" to "wa",
        "y" to "ya",
        "ng" to "ng",
    )

    fun format(
        rawText: String,
        language: ReadingLanguage,
        usingEnglishFallbackForFilipino: Boolean = false,
    ): String {
        var text = rawText.trim()
        if (text.isEmpty()) return ""

        // Normalize visual UI symbols into natural spoken pauses/connectors
        text = text
            .replace("\n", ". ")
            .replace("→", ", ")
            .replace("•", ", ")
            .replace("—", ", ")
            .replace("–", ", ")
            .replace(";", ", ")
            .replace(" + ", ", ")
            .replace(" = ", ", ")

        // Expand curriculum tags & abbreviations cleanly
        text = text
            .replace(Regex("""\bLAS\s*#"""), "Activity ")
            .replace(Regex("""\bSesyon\s*#"""), "Sesyon ")
            .replace(Regex("""\bCVCe\b"""), "C V C E")
            .replace(Regex("""\bCVC\b"""), "C V C")
            .replace(Regex("""\bKS1\b"""), "Key Stage 1")
            .replace(Regex("""\bKS2\b"""), "Key Stage 2")
            .replace(Regex("""\b5Ws\b"""), "five W's")
            .replace(Regex("""\bARAL\b"""), "Aral")
            .replace(Regex("""\bDepEd\b"""), "Dep Ed")

        // Replace doubled letter names (e.g. "Letter Mm" -> "Letter M", "titik Ss" -> "titik S")
        text = doubledLetterRegex.replace(text) { match ->
            val prefix = match.groupValues[1]
            val upper = match.groupValues[2]
            val lower = match.groupValues[3]
            if (upper.lowercase() == lower) "$prefix $upper" else match.value
        }

        // Convert 3+ hyphenated syllable breakdowns (won-der-ful -> won, der, ful)
        text = multiHyphenSyllableRegex.replace(text) { match ->
            match.value.split("-").joinToString(", ")
        }

        // Convert phoneme slash notation (/m/, /ă/, /-et/) into natural spoken sounds
        val phonemeMap = if (language == ReadingLanguage.ENGLISH) englishPhonemeSpoken else filipinoPhonemeSpoken
        text = phonemeSlashRegex.replace(text) { match ->
            val core = match.groupValues[1]
            val rime = match.groupValues[2]
            if (rime.isNotEmpty()) {
                rime
            } else {
                phonemeMap[core.lowercase()] ?: core
            }
        }

        // If device has no Filipino/Tagalog TTS voice pack and must fall back to English,
        // guide common Tagalog particles so they are pronounced close to Tagalog rather than spelled out.
        if (language == ReadingLanguage.FILIPINO && usingEnglishFallbackForFilipino) {
            text = text
                .replace("ñ", "ny")
                .replace("Ñ", "Ny")
                .replace(Regex("""\bmga\b""", RegexOption.IGNORE_CASE), "manga")
                .replace(Regex("""\bng\b""", RegexOption.IGNORE_CASE), "nang")
                .replace(Regex("""\bsi\b""", RegexOption.IGNORE_CASE), "see")
                .replace(Regex("""\bni\b""", RegexOption.IGNORE_CASE), "nee")
                .replace(Regex("""\bsa\b""", RegexOption.IGNORE_CASE), "sah")
                .replace(Regex("""\bna\b""", RegexOption.IGNORE_CASE), "nah")
        }

        // Clean up duplicate spacing/punctuation
        text = text
            .replace(Regex(""",\s*,"""), ",")
            .replace(Regex("""\s+"""), " ")
            .trim()

        return text
    }
}

class ReadingTtsManager(private val context: Context) {

    companion object {
        private const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
        private const val NORMAL_SPEECH_RATE = 0.94f
        private const val SLOW_SPEECH_RATE = 0.78f
        private const val NATURAL_PITCH = 1.0f
    }

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
    private var resolvedEnglishVoice: Voice? = null
    private var offlineEnglishVoice: Voice? = null
    private var resolvedFilipinoVoice: Voice? = null
    private var offlineFilipinoVoice: Voice? = null
    private var usingEnglishFallbackForFilipino: Boolean = false

    private var pendingFallbackRetry: (() -> Unit)? = null
    private var tts: TextToSpeech? = null

    init {
        initBestTtsEngine()
    }

    private fun initBestTtsEngine() {
        val googleTtsInstalled = isTtsEngineInstalled(GOOGLE_TTS_PACKAGE)
        if (googleTtsInstalled) {
            tts = TextToSpeech(context.applicationContext, { status ->
                if (status == TextToSpeech.SUCCESS) {
                    onTtsInitialized()
                } else {
                    // Fallback gracefully to system default TTS engine
                    runCatching { tts?.shutdown() }
                    tts = TextToSpeech(context.applicationContext) { fallbackStatus ->
                        if (fallbackStatus == TextToSpeech.SUCCESS) {
                            onTtsInitialized()
                        } else {
                            _isReady.value = false
                        }
                    }
                }
            }, GOOGLE_TTS_PACKAGE)
        } else {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    onTtsInitialized()
                } else {
                    _isReady.value = false
                }
            }
        }
    }

    private fun isTtsEngineInstalled(packageName: String): Boolean {
        return runCatching {
            val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE).apply {
                setPackage(packageName)
            }
            context.packageManager.queryIntentServices(intent, 0).isNotEmpty()
        }.getOrDefault(false)
    }

    private fun onTtsInitialized() {
        val engine = tts ?: return
        runCatching {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
        }
        configureListeners()
        detectBestLocalesAndVoices()
        _isReady.value = true
    }

    private fun configureListeners() {
        tts?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    pendingFallbackRetry = null
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    val retry = pendingFallbackRetry
                    pendingFallbackRetry = null
                    if (retry != null) {
                        retry()
                    } else {
                        _isSpeaking.value = false
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    val retry = pendingFallbackRetry
                    pendingFallbackRetry = null
                    if (retry != null) {
                        retry()
                    } else {
                        _isSpeaking.value = false
                    }
                }
            },
        )
    }

    private fun detectBestLocalesAndVoices() {
        val engine = tts ?: return
        val available = runCatching { engine.availableLanguages }.getOrNull().orEmpty()
        val voices = runCatching { engine.voices }.getOrNull().orEmpty()

        val enPh = Locale("en", "PH")
        val enUs = Locale.US
        val enFallback = Locale.ENGLISH

        val installedVoices = voices.filter { voice ->
            TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features
        }

        // Select highest-quality natural English voice (preferring en-PH, then en-US)
        resolvedEnglishVoice = selectBestVoice(installedVoices, ReadingLanguage.ENGLISH, allowNetwork = true)
        offlineEnglishVoice = selectBestVoice(installedVoices, ReadingLanguage.ENGLISH, allowNetwork = false)

        resolvedEnglishLocale = resolvedEnglishVoice?.locale ?: when {
            isLocaleSupported(engine, enPh) -> enPh
            available.any { it.language.equals("en", true) && it.country.equals("PH", true) } -> enPh
            isLocaleSupported(engine, enUs) -> enUs
            else -> enFallback
        }
        _englishVoiceLabel.value = buildVoiceLabel(
            locale = resolvedEnglishLocale,
            voice = resolvedEnglishVoice,
            defaultPhLabel = "Philippine English (en-PH • Natural)",
            fallbackPrefix = "English Natural",
        )

        // Select highest-quality natural Filipino/Tagalog voice (fil-PH / tl-PH)
        val filPh = Locale("fil", "PH")
        val tlPh = Locale("tl", "PH")
        val fil = Locale("fil")
        val tl = Locale("tl")

        resolvedFilipinoVoice = selectBestVoice(installedVoices, ReadingLanguage.FILIPINO, allowNetwork = true)
        offlineFilipinoVoice = selectBestVoice(installedVoices, ReadingLanguage.FILIPINO, allowNetwork = false)

        val supportedFilLocale = resolvedFilipinoVoice?.locale ?: when {
            isLocaleSupported(engine, filPh) -> filPh
            isLocaleSupported(engine, tlPh) -> tlPh
            isLocaleSupported(engine, fil) -> fil
            isLocaleSupported(engine, tl) -> tl
            else -> null
        }

        if (supportedFilLocale != null) {
            usingEnglishFallbackForFilipino = false
            resolvedFilipinoLocale = supportedFilLocale
            _filipinoVoiceLabel.value = buildVoiceLabel(
                locale = supportedFilLocale,
                voice = resolvedFilipinoVoice,
                defaultPhLabel = "Filipino / Tagalog (fil-PH • Natural)",
                fallbackPrefix = "Filipino Natural",
            )
        } else {
            usingEnglishFallbackForFilipino = true
            resolvedFilipinoLocale = resolvedEnglishLocale
            resolvedFilipinoVoice = resolvedEnglishVoice
            offlineFilipinoVoice = offlineEnglishVoice
            _filipinoVoiceLabel.value = "Filipino (PH phonetic fallback)"
        }
    }

    private fun selectBestVoice(
        voices: List<Voice>,
        language: ReadingLanguage,
        allowNetwork: Boolean,
    ): Voice? {
        val candidates = voices.filter { voice ->
            if (!allowNetwork && voice.isNetworkConnectionRequired) return@filter false
            localeScore(voice.locale, language) > 0
        }
        if (candidates.isEmpty()) return null

        return candidates.maxWithOrNull(
            compareBy<Voice> { localeScore(it.locale, language) }
                .thenBy { if (it.quality >= Voice.QUALITY_HIGH) 1 else 0 }
                .thenBy { if (!it.isNetworkConnectionRequired) 1 else 0 }
                .thenBy { it.quality }
                .thenBy { if ("-x-" in it.name || "neural" in it.name.lowercase()) 1 else 0 }
                .thenBy { -it.latency },
        )
    }

    private fun localeScore(locale: Locale, language: ReadingLanguage): Int {
        val lang = locale.language.lowercase()
        val country = locale.country.uppercase()
        return when (language) {
            ReadingLanguage.ENGLISH -> when {
                lang == "en" && country == "PH" -> 400
                lang == "en" && country == "US" -> 220
                lang == "en" && country == "GB" -> 180
                lang == "en" -> 140
                else -> 0
            }
            ReadingLanguage.FILIPINO -> when {
                lang == "fil" && country == "PH" -> 400
                lang == "tl" && country == "PH" -> 380
                lang == "fil" -> 320
                lang == "tl" -> 300
                else -> 0
            }
        }
    }

    private fun buildVoiceLabel(
        locale: Locale,
        voice: Voice?,
        defaultPhLabel: String,
        fallbackPrefix: String,
    ): String {
        val isNatural = voice != null && voice.quality >= Voice.QUALITY_HIGH
        val qualitySuffix = if (isNatural) " • Natural" else ""
        return if (locale.country.equals("PH", ignoreCase = true)) {
            if (locale.language.equals("en", ignoreCase = true)) {
                "Philippine English (en-PH$qualitySuffix)"
            } else {
                defaultPhLabel
            }
        } else {
            "$fallbackPrefix (${locale.toLanguageTag()}$qualitySuffix)"
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

        val spokenText = ReadingSpeechFormatter.format(
            rawText = text,
            language = language,
            usingEnglishFallbackForFilipino = usingEnglishFallbackForFilipino,
        )
        if (spokenText.isBlank()) return

        val primaryVoice = when (language) {
            ReadingLanguage.ENGLISH -> resolvedEnglishVoice ?: offlineEnglishVoice
            ReadingLanguage.FILIPINO -> resolvedFilipinoVoice ?: offlineFilipinoVoice
        }
        val targetLocale = when (language) {
            ReadingLanguage.ENGLISH -> resolvedEnglishLocale
            ReadingLanguage.FILIPINO -> resolvedFilipinoLocale
        }

        val fallbackVoice = when (language) {
            ReadingLanguage.ENGLISH -> offlineEnglishVoice
            ReadingLanguage.FILIPINO -> offlineFilipinoVoice
        }
        pendingFallbackRetry = if (primaryVoice != null && primaryVoice.isNetworkConnectionRequired && fallbackVoice != null) {
            {
                speakWithVoice(engine, spokenText, targetLocale, fallbackVoice, forceSlow)
            }
        } else {
            null
        }

        speakWithVoice(engine, spokenText, targetLocale, primaryVoice, forceSlow)
    }

    private fun speakWithVoice(
        engine: TextToSpeech,
        spokenText: String,
        locale: Locale,
        voice: Voice?,
        forceSlow: Boolean,
    ) {
        if (voice != null) {
            runCatching { engine.voice = voice }
                .onFailure { engine.language = locale }
        } else {
            engine.language = locale
        }

        val rate = if (forceSlow || _slowMode.value) SLOW_SPEECH_RATE else NORMAL_SPEECH_RATE
        engine.setSpeechRate(rate)
        engine.setPitch(NATURAL_PITCH)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val utteranceId = "aral_${System.currentTimeMillis()}"
        engine.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        pendingFallbackRetry = null
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        pendingFallbackRetry = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isReady.value = false
        _isSpeaking.value = false
    }
}
