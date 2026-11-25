package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.curriculum.CurriculumRepository
import com.example.curriculum.ReadingTrack
import com.example.data.AralDatabase
import com.example.data.AralRepository
import com.example.data.MasteryBadgeTier
import com.example.report.ProgressPdfGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AralReadingTests {

    private lateinit var context: Context
    private lateinit var db: AralDatabase
    private lateinit var repository: AralRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AralDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AralRepository(db.aralDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `all 4 workbook tracks have 8 weeks and valid questions with hints and mini-lessons`() {
        ReadingTrack.entries.forEach { track ->
            val modules = CurriculumRepository.getModulesForTrack(track)
            assertEquals("Expected 8 weeks for ${track.title}", 8, modules.size)
            modules.forEach { mod ->
                assertTrue("Module ${mod.id} has no questions", mod.questions.isNotEmpty())
                mod.questions.forEach { q ->
                    assertTrue("Question ${q.id} options missing answer", q.correctAnswer in q.options)
                    assertTrue("Question ${q.id} hint blank", q.hint.isNotBlank())
                    assertTrue("Question ${q.id} miniLesson blank", q.miniLesson.isNotBlank())
                }
            }
        }
        assertEquals(4, CurriculumRepository.getModulesForTrack(ReadingTrack.KS1_ENGLISH).count { it.isCheckIn })
        assertEquals(4, CurriculumRepository.getModulesForTrack(ReadingTrack.KS1_FILIPINO).count { it.isCheckIn })
        assertEquals(
            listOf("KS1-English", "KS1-Filipino", "KS2-Basic", "KS2-Plus"),
            ReadingTrack.entries.map { it.title },
        )
    }

    @Test
    fun `5-color mastery badge tiers map percentages accurately`() {
        assertEquals(MasteryBadgeTier.SUPPORT_GRAY, MasteryBadgeTier.fromScore(0, 4, attemptsCount = 0))
        assertEquals(MasteryBadgeTier.SUPPORT_GRAY, MasteryBadgeTier.fromScore(1, 4, attemptsCount = 1))
        assertEquals(MasteryBadgeTier.EMERGING_RED, MasteryBadgeTier.fromScore(2, 4, attemptsCount = 1))
        assertEquals(MasteryBadgeTier.DEVELOPING_AMBER, MasteryBadgeTier.fromScore(3, 4, attemptsCount = 1))
        assertEquals(MasteryBadgeTier.PROFICIENT_BLUE, MasteryBadgeTier.fromScore(4, 5, attemptsCount = 1))
        assertEquals(MasteryBadgeTier.MASTERED_GREEN, MasteryBadgeTier.fromScore(4, 4, attemptsCount = 1))
    }

    @Test
    fun `check-in, repeated-wrong tracking, backup restore, and PDF generation work end-to-end`() = runTest {
        val ks1Learner = repository.checkInLearner("Maria Santos", gradeLevel = 2, section = "Sampaguita")
        assertEquals("KS1", ks1Learner.keyStage)
        assertEquals("ks1_english", ks1Learner.preferredTrackId)

        val ks2Learner = repository.checkInLearner("Jose Reyes", gradeLevel = 5, section = "Rizal")
        assertEquals("KS2", ks2Learner.keyStage)
        assertEquals("ks2_basic", ks2Learner.preferredTrackId)

        repository.recordItemMissedAfterTwoTries(
            learnerId = ks1Learner.id,
            trackId = ReadingTrack.KS1_ENGLISH.id,
            moduleId = "ks1_en_w2",
            moduleTitle = "Week 2: Target Letters m, a, t",
            itemId = "ks1_en_w2_q2",
            promptSummary = "Tap the word formed by /m/ + /a/ + /t/",
            expectedAnswer = "mat",
            learnerAnswer = "amt",
        )
        repository.recordItemMissedAfterTwoTries(
            learnerId = ks1Learner.id,
            trackId = ReadingTrack.KS1_ENGLISH.id,
            moduleId = "ks1_en_w2",
            moduleTitle = "Week 2: Target Letters m, a, t",
            itemId = "ks1_en_w2_q2",
            promptSummary = "Tap the word formed by /m/ + /a/ + /t/",
            expectedAnswer = "mat",
            learnerAnswer = "tam",
        )

        val progress = repository.recordSessionCompletion(
            learnerId = ks1Learner.id,
            trackId = ReadingTrack.KS1_ENGLISH.id,
            moduleId = "ks1_en_w2",
            weekNumber = 2,
            sessionOrder = 2,
            totalQuestions = 4,
            correctCount = 3,
            firstTryCorrectCount = 2,
        )
        assertEquals(MasteryBadgeTier.DEVELOPING_AMBER.tierIndex, progress.badgeTier)

        val wrongList = repository.observeWrongItems(ks1Learner.id).first()
        assertEquals(1, wrongList.size)
        assertEquals(2, wrongList.first().repeatedWrongCount)

        // Generate real PDF report
        val pdfResult = ProgressPdfGenerator.generateProgressReport(
            context = context,
            learner = ks1Learner,
            progressList = listOf(progress),
            wrongItems = wrongList,
        )
        assertTrue(pdfResult.file.exists())
        assertTrue(pdfResult.file.length() > 100)
        val header = pdfResult.file.inputStream().use { stream ->
            ByteArray(4).also { stream.read(it) }.decodeToString()
        }
        assertEquals("%PDF", header)

        // Export & Restore JSON Backup
        val backupJson = repository.exportBackupJson()
        repository.deleteLearner(ks1Learner.id)
        assertEquals(1, repository.observeLearners().first().size)

        val restoreSummary = repository.restoreFromBackupJson(backupJson).getOrThrow()
        assertEquals(2, restoreSummary.learnersRestored)
        assertEquals(2, repository.observeLearners().first().size)
        assertEquals(2, repository.observeWrongItems(ks1Learner.id).first().first().repeatedWrongCount)
    }
}
