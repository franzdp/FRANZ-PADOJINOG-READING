package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.ReadingLanguage
import com.example.audio.ReadingTtsManager
import com.example.curriculum.CurriculumRepository
import com.example.curriculum.ReadingModule
import com.example.curriculum.ReadingQuestion
import com.example.curriculum.ReadingTrack
import com.example.data.AralDatabase
import com.example.data.AralRepository
import com.example.data.LearnerEntity
import com.example.data.MasteryBadgeTier
import com.example.data.SessionProgressEntity
import com.example.data.WrongItemEntity
import com.example.report.MessengerReportSharer
import com.example.report.PdfGenerationResult
import com.example.report.ProgressPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LessonRunnerUiState(
    val module: ReadingModule? = null,
    val questions: List<ReadingQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val attemptNumber: Int = 1, // 1 or 2 (2-try engine)
    val selectedOption: String? = null,
    val isCurrentSolved: Boolean = false,
    val isCurrentFailedBothTries: Boolean = false,
    val showHintBanner: Boolean = false,
    val showMiniLessonBanner: Boolean = false,
    val correctCount: Int = 0,
    val firstTryCorrectCount: Int = 0,
    val isCompleted: Boolean = false,
    val earnedTier: MasteryBadgeTier = MasteryBadgeTier.SUPPORT_GRAY,
    val isRemediationDrill: Boolean = false,
) {
    val currentQuestion: ReadingQuestion?
        get() = questions.getOrNull(currentIndex)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AralViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AralDatabase.getInstance(application)
    val repository = AralRepository(database.aralDao())
    val ttsManager = ReadingTtsManager(application)

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusMessages: SharedFlow<String> = _statusMessages.asSharedFlow()

    val learners: StateFlow<List<LearnerEntity>> = repository.observeLearners()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeLearnerId = MutableStateFlow<String?>(null)
    val activeLearnerId: StateFlow<String?> = _activeLearnerId.asStateFlow()

    val activeLearner: StateFlow<LearnerEntity?> = _activeLearnerId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.observeLearner(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeProgress: StateFlow<List<SessionProgressEntity>> = _activeLearnerId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeProgress(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWrongItems: StateFlow<List<WrongItemEntity>> = _activeLearnerId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.observeWrongItems(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTrack = MutableStateFlow(ReadingTrack.KS1_ENGLISH)
    val selectedTrack: StateFlow<ReadingTrack> = _selectedTrack.asStateFlow()

    private val _lessonState = MutableStateFlow(LessonRunnerUiState())
    val lessonState: StateFlow<LessonRunnerUiState> = _lessonState.asStateFlow()

    private val _lastPdfResult = MutableStateFlow<PdfGenerationResult?>(null)
    val lastPdfResult: StateFlow<PdfGenerationResult?> = _lastPdfResult.asStateFlow()

    fun checkInAndActivate(
        fullName: String,
        gradeLevel: Int,
        section: String,
        onReady: (LearnerEntity) -> Unit,
    ) {
        if (fullName.isBlank()) {
            _statusMessages.tryEmit("Please enter the learner's name to check in.")
            return
        }
        viewModelScope.launch {
            val learner = repository.checkInLearner(
                fullName = fullName,
                gradeLevel = gradeLevel,
                section = section,
            )
            _activeLearnerId.value = learner.id
            _selectedTrack.value = ReadingTrack.fromId(learner.preferredTrackId)
            onReady(learner)
        }
    }

    fun resumeReturningLearner(learner: LearnerEntity, onReady: () -> Unit) {
        viewModelScope.launch {
            repository.touchLearnerResume(
                learnerId = learner.id,
                trackId = learner.preferredTrackId,
                moduleId = learner.lastModuleId,
            )
            _activeLearnerId.value = learner.id
            _selectedTrack.value = ReadingTrack.fromId(learner.preferredTrackId)
            onReady()
        }
    }

    fun selectTrack(track: ReadingTrack) {
        _selectedTrack.value = track
        val learner = activeLearner.value ?: return
        viewModelScope.launch {
            repository.touchLearnerResume(
                learnerId = learner.id,
                trackId = track.id,
                moduleId = learner.lastModuleId,
            )
        }
    }

    fun deleteLearner(learnerId: String) {
        viewModelScope.launch {
            repository.deleteLearner(learnerId)
            if (_activeLearnerId.value == learnerId) {
                _activeLearnerId.value = null
            }
            _statusMessages.tryEmit("Learner profile removed.")
        }
    }

    fun startModule(moduleId: String) {
        val module = CurriculumRepository.getModuleById(moduleId) ?: return
        _selectedTrack.value = module.track
        _lessonState.value = LessonRunnerUiState(
            module = module,
            questions = module.questions,
            currentIndex = 0,
            attemptNumber = 1,
            isRemediationDrill = false,
        )
        val learnerId = _activeLearnerId.value
        if (learnerId != null) {
            viewModelScope.launch {
                repository.touchLearnerResume(learnerId, module.track.id, module.id)
            }
        }
    }

    fun startRepeatedWrongPractice(): Boolean {
        val wrongList = activeWrongItems.value
        if (wrongList.isEmpty()) {
            _statusMessages.tryEmit("No repeated-wrong items to practice right now!")
            return false
        }
        val allQuestions = CurriculumRepository.getAllModules()
            .flatMap { mod -> mod.questions.map { q -> mod to q } }
            .associateBy { (_, q) -> q.id }

        val drillQuestions = wrongList.mapNotNull { wrong -> allQuestions[wrong.itemId]?.second }
        if (drillQuestions.isEmpty()) return false

        val syntheticModule = ReadingModule(
            id = "remediation_drill",
            track = _selectedTrack.value,
            weekNumber = 0,
            sessionOrder = 0,
            isCheckIn = false,
            title = "Repeated-Wrong Remediation Practice",
            subtitle = "Targeted 2-try review of missed workbook items",
            workbookRef = "Bagacay ES Remediation Drill",
            targetFocus = "Reviewing ${drillQuestions.size} missed reading items",
            language = _selectedTrack.value.primaryLanguage,
            passage = null,
            questions = drillQuestions,
        )
        _lessonState.value = LessonRunnerUiState(
            module = syntheticModule,
            questions = drillQuestions,
            currentIndex = 0,
            attemptNumber = 1,
            isRemediationDrill = true,
        )
        return true
    }

    fun submitAnswer(option: String) {
        val state = _lessonState.value
        val question = state.currentQuestion ?: return
        val module = state.module ?: return
        if (state.isCurrentSolved || state.isCurrentFailedBothTries) return

        val isCorrect = option == question.correctAnswer
        if (isCorrect) {
            val gotFirstTry = state.attemptNumber == 1
            _lessonState.value = state.copy(
                selectedOption = option,
                isCurrentSolved = true,
                showHintBanner = false,
                showMiniLessonBanner = !gotFirstTry, // Show reinforcing mini-lesson after 2nd try success
                correctCount = state.correctCount + 1,
                firstTryCorrectCount = state.firstTryCorrectCount + if (gotFirstTry) 1 else 0,
            )
            return
        }

        // Wrong answer handling (2-Try Engine)
        if (state.attemptNumber == 1) {
            // Try 1 Wrong -> Show Hint + let learner try a 2nd time
            _lessonState.value = state.copy(
                selectedOption = option,
                attemptNumber = 2,
                showHintBanner = true,
                showMiniLessonBanner = false,
            )
        } else {
            // Try 2 Wrong -> Record repeated-wrong item + reveal full Mini-Lesson & answer
            _lessonState.value = state.copy(
                selectedOption = option,
                isCurrentFailedBothTries = true,
                showHintBanner = false,
                showMiniLessonBanner = true,
            )
            val learnerId = _activeLearnerId.value
            if (learnerId != null) {
                val itemModule = CurriculumRepository.getAllModules()
                    .firstOrNull { mod -> mod.questions.any { it.id == question.id } } ?: module
                viewModelScope.launch {
                    repository.recordItemMissedAfterTwoTries(
                        learnerId = learnerId,
                        trackId = itemModule.track.id,
                        moduleId = itemModule.id,
                        moduleTitle = itemModule.title,
                        itemId = question.id,
                        promptSummary = question.prompt,
                        expectedAnswer = question.correctAnswer,
                        learnerAnswer = option,
                    )
                }
            }
        }
    }

    fun nextQuestionOrFinish() {
        ttsManager.stop()
        val state = _lessonState.value
        val module = state.module ?: return
        val nextIndex = state.currentIndex + 1

        if (nextIndex < state.questions.size) {
            _lessonState.value = state.copy(
                currentIndex = nextIndex,
                attemptNumber = 1,
                selectedOption = null,
                isCurrentSolved = false,
                isCurrentFailedBothTries = false,
                showHintBanner = false,
                showMiniLessonBanner = false,
            )
        } else {
            val total = state.questions.size.coerceAtLeast(1)
            val earned = MasteryBadgeTier.fromScore(state.correctCount, total, attemptsCount = 1)
            _lessonState.value = state.copy(
                isCompleted = true,
                earnedTier = earned,
            )

            val learnerId = _activeLearnerId.value
            if (learnerId != null && !state.isRemediationDrill) {
                viewModelScope.launch {
                    val saved = repository.recordSessionCompletion(
                        learnerId = learnerId,
                        trackId = module.track.id,
                        moduleId = module.id,
                        weekNumber = module.weekNumber,
                        sessionOrder = module.sessionOrder,
                        totalQuestions = total,
                        correctCount = state.correctCount,
                        firstTryCorrectCount = state.firstTryCorrectCount,
                    )
                    _lessonState.value = _lessonState.value.copy(
                        earnedTier = MasteryBadgeTier.fromTierIndex(saved.badgeTier),
                    )
                }
            }
        }
    }

    fun speakText(text: String, language: ReadingLanguage, forceSlow: Boolean = false) {
        ttsManager.speak(text, language, forceSlow)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = repository.exportBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open destination file.")
                }
            }.onSuccess {
                _statusMessages.tryEmit("Backup saved safely! Records are ready to restore anytime.")
            }.onFailure { err ->
                _statusMessages.tryEmit("Backup failed: ${err.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    fun restoreBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Unable to read selected backup file.")
                }
                repository.restoreFromBackupJson(json).getOrThrow()
            }.onSuccess { summary ->
                val refreshed = repository.observeLearners()
                _statusMessages.tryEmit(
                    "Restored ${summary.learnersRestored} learner(s), ${summary.sessionsRestored} session(s), ${summary.wrongItemsRestored} wrong item(s).",
                )
                refreshed
            }.onFailure { err ->
                _statusMessages.tryEmit("Restore failed: ${err.localizedMessage ?: "Invalid JSON backup"}")
            }
        }
    }

    fun generateAndSharePdfToMessenger(context: Context) {
        val learner = activeLearner.value
        if (learner == null) {
            _statusMessages.tryEmit("Please select or check in a learner first.")
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ProgressPdfGenerator.generateProgressReport(
                    context = context,
                    learner = learner,
                    progressList = activeProgress.value,
                    wrongItems = activeWrongItems.value,
                )
            }
            _lastPdfResult.value = result
            val msg = MessengerReportSharer.sendPdfToMessenger(context, result)
            _statusMessages.tryEmit(msg)
        }
    }

    fun generateAndPreviewPdf(context: Context) {
        val learner = activeLearner.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ProgressPdfGenerator.generateProgressReport(
                    context = context,
                    learner = learner,
                    progressList = activeProgress.value,
                    wrongItems = activeWrongItems.value,
                )
            }
            _lastPdfResult.value = result
            val opened = MessengerReportSharer.openPdfPreview(context, result)
            if (!opened) {
                _statusMessages.tryEmit("PDF saved at ${result.file.name}. No PDF viewer installed — use Send to Messenger or Save PDF.")
            }
        }
    }

    fun savePdfToUri(context: Context, uri: Uri) {
        val learner = activeLearner.value ?: return
        viewModelScope.launch {
            runCatching {
                val result = withContext(Dispatchers.IO) {
                    ProgressPdfGenerator.generateProgressReport(
                        context = context,
                        learner = learner,
                        progressList = activeProgress.value,
                        wrongItems = activeWrongItems.value,
                    )
                }
                _lastPdfResult.value = result
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        result.file.inputStream().use { input -> input.copyTo(out) }
                    } ?: error("Cannot write PDF destination.")
                }
            }.onSuccess {
                _statusMessages.tryEmit("PDF Progress Report saved!")
            }.onFailure { err ->
                _statusMessages.tryEmit("Could not save PDF: ${err.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
