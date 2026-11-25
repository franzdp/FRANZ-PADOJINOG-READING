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
import com.example.data.InProgressSessionEntity
import com.example.data.LearnerEntity
import com.example.data.MasteryBadgeTier
import com.example.data.SessionProgressEntity
import com.example.data.WrongItemEntity
import com.example.report.MessengerReportSharer
import com.example.report.PdfGenerationResult
import com.example.report.ProgressPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

class AralViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AralDatabase.getInstance(application)
    val repository = AralRepository(database.aralDao())
    val ttsManager = ReadingTtsManager(application)

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val statusMessages: SharedFlow<String> = _statusMessages.asSharedFlow()

    val learners: StateFlow<List<LearnerEntity>> = repository.observeLearners()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allProgress: StateFlow<List<SessionProgressEntity>> = repository.observeAllProgress()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allWrongItems: StateFlow<List<WrongItemEntity>> = repository.observeAllWrongItems()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val allInProgress: StateFlow<List<InProgressSessionEntity>> = repository.observeAllInProgress()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val progressByLearner: StateFlow<Map<String, List<SessionProgressEntity>>> = allProgress
        .map { list -> list.groupBy { it.learnerId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val wrongCountByLearner: StateFlow<Map<String, Int>> = allWrongItems
        .map { list -> list.groupingBy { it.learnerId }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _activeLearnerId = MutableStateFlow<String?>(null)
    val activeLearnerId: StateFlow<String?> = _activeLearnerId.asStateFlow()

    val activeLearner: StateFlow<LearnerEntity?> = combine(_activeLearnerId, learners) { id, list ->
        if (id == null) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeProgress: StateFlow<List<SessionProgressEntity>> = combine(_activeLearnerId, allProgress) { id, list ->
        if (id == null) emptyList() else list.filter { it.learnerId == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeWrongItems: StateFlow<List<WrongItemEntity>> = combine(_activeLearnerId, allWrongItems) { id, list ->
        if (id == null) emptyList() else list.filter { it.learnerId == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val activeInProgress: StateFlow<List<InProgressSessionEntity>> = combine(_activeLearnerId, allInProgress) { id, list ->
        if (id == null) emptyList() else list.filter { it.learnerId == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
            ttsManager.stop()
            _lessonState.value = LessonRunnerUiState()
            _lastPdfResult.value = null
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
            ttsManager.stop()
            _lessonState.value = LessonRunnerUiState()
            _lastPdfResult.value = null
            val track = ReadingTrack.fromId(learner.preferredTrackId)
            val scopedModuleId = learner.lastModuleId?.takeIf {
                CurriculumRepository.getModuleById(it)?.track == track
            }
            repository.touchLearnerResume(
                learnerId = learner.id,
                trackId = track.id,
                moduleId = scopedModuleId,
            )
            _activeLearnerId.value = learner.id
            _selectedTrack.value = track
            onReady()
        }
    }

    fun switchLearner() {
        ttsManager.stop()
        _activeLearnerId.value = null
        _lessonState.value = LessonRunnerUiState()
        _lastPdfResult.value = null
    }

    fun selectTrack(track: ReadingTrack) {
        _selectedTrack.value = track
        val learner = activeLearner.value ?: return
        val scopedModuleId = learner.lastModuleId?.takeIf {
            CurriculumRepository.getModuleById(it)?.track == track
        }
        viewModelScope.launch {
            repository.touchLearnerResume(
                learnerId = learner.id,
                trackId = track.id,
                moduleId = scopedModuleId,
            )
        }
    }

    fun updateLearnerProfile(
        learnerId: String,
        fullName: String,
        gradeLevel: Int,
        section: String,
    ) {
        if (fullName.isBlank()) {
            _statusMessages.tryEmit("Learner name cannot be empty.")
            return
        }
        viewModelScope.launch {
            repository.updateLearnerProfile(
                learnerId = learnerId,
                fullName = fullName,
                gradeLevel = gradeLevel,
                section = section,
            )
            _statusMessages.tryEmit("Updated learner profile.")
        }
    }

    fun deleteLearner(learnerId: String) {
        viewModelScope.launch {
            repository.deleteLearner(learnerId)
            if (_activeLearnerId.value == learnerId) {
                _activeLearnerId.value = null
                _lessonState.value = LessonRunnerUiState()
                _lastPdfResult.value = null
            }
            _statusMessages.tryEmit("Learner profile removed.")
        }
    }

    fun startModule(moduleId: String, forceRestart: Boolean = false) {
        val module = CurriculumRepository.getModuleById(moduleId) ?: return
        _selectedTrack.value = module.track
        val learnerId = _activeLearnerId.value

        val cachedCheckpoint = if (!forceRestart && learnerId != null) {
            activeInProgress.value.firstOrNull { it.learnerId == learnerId && it.moduleId == moduleId }
        } else {
            null
        }

        _lessonState.value = buildLessonStateFromCheckpoint(module, cachedCheckpoint)

        if (learnerId != null) {
            viewModelScope.launch {
                if (forceRestart) {
                    repository.clearInProgressSession(learnerId, module.id)
                } else if (cachedCheckpoint == null) {
                    val dbCheckpoint = repository.getInProgressSession(learnerId, module.id)
                    if (dbCheckpoint != null && _lessonState.value.module?.id == module.id && _activeLearnerId.value == learnerId) {
                        _lessonState.value = buildLessonStateFromCheckpoint(module, dbCheckpoint)
                    }
                }
                repository.touchLearnerResume(learnerId, module.track.id, module.id)
            }
        }
    }

    private fun buildLessonStateFromCheckpoint(
        module: ReadingModule,
        saved: InProgressSessionEntity?,
    ): LessonRunnerUiState {
        if (saved == null || module.questions.isEmpty()) {
            return LessonRunnerUiState(
                module = module,
                questions = module.questions,
                currentIndex = 0,
                attemptNumber = 1,
                isRemediationDrill = false,
            )
        }
        val safeIndex = saved.currentIndex.coerceIn(0, module.questions.lastIndex)
        val itemFinished = saved.isCurrentSolved || saved.isCurrentFailedBothTries
        return if (itemFinished && safeIndex + 1 < module.questions.size) {
            LessonRunnerUiState(
                module = module,
                questions = module.questions,
                currentIndex = safeIndex + 1,
                attemptNumber = 1,
                selectedOption = null,
                isCurrentSolved = false,
                isCurrentFailedBothTries = false,
                showHintBanner = false,
                showMiniLessonBanner = false,
                correctCount = saved.correctCount.coerceIn(0, module.questions.size),
                firstTryCorrectCount = saved.firstTryCorrectCount.coerceIn(0, module.questions.size),
                isRemediationDrill = false,
            )
        } else {
            LessonRunnerUiState(
                module = module,
                questions = module.questions,
                currentIndex = safeIndex,
                attemptNumber = saved.attemptNumber.coerceIn(1, 2),
                selectedOption = saved.selectedOption,
                isCurrentSolved = saved.isCurrentSolved,
                isCurrentFailedBothTries = saved.isCurrentFailedBothTries,
                showHintBanner = saved.showHintBanner,
                showMiniLessonBanner = saved.showMiniLessonBanner,
                correctCount = saved.correctCount.coerceIn(0, module.questions.size),
                firstTryCorrectCount = saved.firstTryCorrectCount.coerceIn(0, module.questions.size),
                isRemediationDrill = false,
            )
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
            val updated = state.copy(
                selectedOption = option,
                isCurrentSolved = true,
                showHintBanner = false,
                showMiniLessonBanner = !gotFirstTry, // Show reinforcing mini-lesson after 2nd try success
                correctCount = state.correctCount + 1,
                firstTryCorrectCount = state.firstTryCorrectCount + if (gotFirstTry) 1 else 0,
            )
            _lessonState.value = updated
            persistInProgressIfApplicable(updated)
            return
        }

        // Wrong answer handling (2-Try Engine)
        if (state.attemptNumber == 1) {
            // Try 1 Wrong -> Show Hint + let learner try a 2nd time
            val updated = state.copy(
                selectedOption = option,
                attemptNumber = 2,
                showHintBanner = true,
                showMiniLessonBanner = false,
            )
            _lessonState.value = updated
            persistInProgressIfApplicable(updated)
        } else {
            // Try 2 Wrong -> Record repeated-wrong item + reveal full Mini-Lesson & answer
            val updated = state.copy(
                selectedOption = option,
                isCurrentFailedBothTries = true,
                showHintBanner = false,
                showMiniLessonBanner = true,
            )
            _lessonState.value = updated
            persistInProgressIfApplicable(updated)

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
            val updated = state.copy(
                currentIndex = nextIndex,
                attemptNumber = 1,
                selectedOption = null,
                isCurrentSolved = false,
                isCurrentFailedBothTries = false,
                showHintBanner = false,
                showMiniLessonBanner = false,
            )
            _lessonState.value = updated
            persistInProgressIfApplicable(updated)
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
                    repository.clearInProgressSession(learnerId, module.id)
                    _lessonState.value = _lessonState.value.copy(
                        earnedTier = MasteryBadgeTier.fromTierIndex(saved.badgeTier),
                    )
                }
            }
        }
    }

    private fun persistInProgressIfApplicable(state: LessonRunnerUiState) {
        val learnerId = _activeLearnerId.value ?: return
        val module = state.module ?: return
        if (state.isRemediationDrill || state.isCompleted) return
        val checkpoint = InProgressSessionEntity(
            learnerId = learnerId,
            moduleId = module.id,
            trackId = module.track.id,
            currentIndex = state.currentIndex,
            attemptNumber = state.attemptNumber,
            selectedOption = state.selectedOption,
            isCurrentSolved = state.isCurrentSolved,
            isCurrentFailedBothTries = state.isCurrentFailedBothTries,
            showHintBanner = state.showHintBanner,
            showMiniLessonBanner = state.showMiniLessonBanner,
            correctCount = state.correctCount,
            firstTryCorrectCount = state.firstTryCorrectCount,
            updatedAt = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            repository.saveInProgressSession(checkpoint)
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
                _statusMessages.tryEmit(
                    "Restored ${summary.learnersRestored} learner(s), ${summary.sessionsRestored} session(s), ${summary.wrongItemsRestored} wrong item(s).",
                )
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
