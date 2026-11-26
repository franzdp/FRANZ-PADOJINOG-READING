package com.example

import com.example.audio.ReadingLanguage
import com.example.audio.ReadingSpeechFormatter
import com.example.curriculum.CurriculumRepository
import com.example.curriculum.ReadingTrack
import com.example.data.MasteryBadgeTier
import com.example.data.SessionProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun curriculum_tracks_and_resume_are_isolated_per_learner_and_track() {
        assertEquals(
            listOf("KS1-English", "KS1-Filipino", "KS2-Basic", "KS2-Plus"),
            ReadingTrack.entries.map { it.title },
        )

        val learnerAProgress = listOf(
            SessionProgressEntity(
                learnerId = "learner-a",
                moduleId = "ks1_en_w1",
                trackId = ReadingTrack.KS1_ENGLISH.id,
                weekNumber = 1,
                sessionOrder = 1,
                totalQuestions = 4,
                bestScore = 4,
                lastScore = 4,
                firstTryCorrectCount = 4,
                attemptsCount = 1,
                badgeTier = MasteryBadgeTier.MASTERED_GREEN.tierIndex,
                completedAt = 1L,
                updatedAt = 1L,
            ),
        )
        val learnerBProgress = emptyList<SessionProgressEntity>()

        assertEquals(
            "ks1_en_w2",
            CurriculumRepository.resolveResumeModule(ReadingTrack.KS1_ENGLISH, "ks1_en_w1", learnerAProgress).id,
        )
        assertEquals(
            "ks1_en_w1",
            CurriculumRepository.resolveResumeModule(ReadingTrack.KS1_ENGLISH, null, learnerBProgress).id,
        )
        assertEquals(
            "ks1_fil_w1",
            CurriculumRepository.resolveResumeModule(ReadingTrack.KS1_FILIPINO, "ks1_en_w1", learnerAProgress).id,
        )
        assertEquals(
            "ks2_plus_w1",
            CurriculumRepository.resolveResumeModule(
                track = ReadingTrack.KS2_PLUS,
                lastModuleId = null,
                progressList = emptyList(),
                inProgressModuleIds = setOf("ks2_plus_w1", "ks1_en_w2"),
            ).id,
        )
        assertEquals(
            "ks2_plus_w3",
            CurriculumRepository.resolveResumeModule(
                track = ReadingTrack.KS2_PLUS,
                lastModuleId = "ks2_plus_w1",
                progressList = emptyList(),
                inProgressModuleIds = setOf("ks2_plus_w3"),
            ).id,
        )
    }

    @Test
    fun speech_formatter_cleans_phonemes_syllables_and_filipino_pronunciation() {
        val englishFormatted = ReadingSpeechFormatter.format(
            "Mini-Lesson (LAS #1): Letter Mm makes /m/ + /ă/ + /t/ in won-der-ful CVCe.",
            ReadingLanguage.ENGLISH,
        )
        assertFalse(englishFormatted.contains("/"))
        assertTrue(englishFormatted.contains("Activity 1"))
        assertTrue(englishFormatted.contains("Letter M"))
        assertTrue(englishFormatted.contains("mmm, short a, tuh"))
        assertTrue(englishFormatted.contains("won, der, ful"))
        assertTrue(englishFormatted.contains("C V C E"))

        val filipinoNative = ReadingSpeechFormatter.format(
            "Mini-Lesson (Sesyon 8): titik Ss (/s/) at patinig Aa (/a/). ma + sa = masa!",
            ReadingLanguage.FILIPINO,
            usingEnglishFallbackForFilipino = false,
        )
        assertFalse(filipinoNative.contains("/"))
        assertTrue(filipinoNative.contains("titik S"))
        assertTrue(filipinoNative.contains("patinig A"))

        val filipinoFallback = ReadingSpeechFormatter.format(
            "Mga bata ng Bagacay at si Niño",
            ReadingLanguage.FILIPINO,
            usingEnglishFallbackForFilipino = true,
        )
        assertTrue(filipinoFallback.contains("manga"))
        assertTrue(filipinoFallback.contains("nang"))
        assertTrue(filipinoFallback.contains("Ninyo"))
    }
}
