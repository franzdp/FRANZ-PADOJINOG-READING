package com.example.curriculum

import com.example.audio.ReadingLanguage

enum class ReadingTrack(
    val id: String,
    val title: String,
    val shortTitle: String,
    val keyStage: String,
    val gradeRangeLabel: String,
    val description: String,
    val primaryLanguage: ReadingLanguage,
) {
    KS1_ENGLISH(
        id = "ks1_english",
        title = "KS1-English",
        shortTitle = "KS1-English",
        keyStage = "KS1",
        gradeRangeLabel = "Grades 1–3",
        description = "Alphabet, phonics, CVC words, sight words & Magic E",
        primaryLanguage = ReadingLanguage.ENGLISH,
    ),
    KS1_FILIPINO(
        id = "ks1_filipino",
        title = "KS1-Filipino",
        shortTitle = "KS1-Filipino",
        keyStage = "KS1",
        gradeRangeLabel = "Grades 1–3",
        description = "Alpabeto, pagpapantig, klaster, salitandaan at pagbasa",
        primaryLanguage = ReadingLanguage.FILIPINO,
    ),
    KS2_BASIC(
        id = "ks2_basic",
        title = "KS2-Basic",
        shortTitle = "KS2-Basic",
        keyStage = "KS2",
        gradeRangeLabel = "Grades 4–6",
        description = "Weeks 1–4 Filipino decoding & stories; Weeks 5–8 English blends & CVCe",
        primaryLanguage = ReadingLanguage.FILIPINO,
    ),
    KS2_PLUS(
        id = "ks2_plus",
        title = "KS2-Plus",
        shortTitle = "KS2-Plus",
        keyStage = "KS2",
        gradeRangeLabel = "Grades 4–6",
        description = "Weeks 1–8 English fluency, root words, story elements, cause-effect & 5Ws",
        primaryLanguage = ReadingLanguage.ENGLISH,
    );

    companion object {
        fun fromId(id: String): ReadingTrack {
            return entries.firstOrNull { it.id == id } ?: KS1_ENGLISH
        }

        fun forKeyStage(keyStage: String): List<ReadingTrack> {
            return entries.filter { it.keyStage == keyStage }
        }
    }
}

enum class QuestionKind(val badgeLabel: String) {
    LISTEN_AND_PICK("Listen & Pick"),
    PHONICS_SOUND("Phonics & Sound"),
    WORD_BUILDER("Word & Syllable Builder"),
    READING_COMPREHENSION("Story & Comprehension"),
    CHECK_IN("Review & Mastery"),
}

data class ReadingPassage(
    val title: String,
    val body: String,
    val language: ReadingLanguage,
)

data class ReadingQuestion(
    val id: String,
    val kind: QuestionKind,
    val prompt: String,
    val speakText: String?,
    val speakLabel: String = "Listen (PH English)",
    val language: ReadingLanguage,
    val visualCue: String? = null,
    val options: List<String>,
    val correctAnswer: String,
    val hint: String,       // Shown on Try 1 wrong
    val miniLesson: String, // Shown on Try 2 wrong or Tutor's Guide scaffolding
)

data class ReadingModule(
    val id: String,
    val track: ReadingTrack,
    val weekNumber: Int,
    val sessionOrder: Int,
    val isCheckIn: Boolean = false,
    val title: String,
    val subtitle: String,
    val workbookRef: String,
    val targetFocus: String,
    val language: ReadingLanguage,
    val passage: ReadingPassage? = null,
    val questions: List<ReadingQuestion>,
)
