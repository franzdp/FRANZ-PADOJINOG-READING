package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.ReadingLanguage
import com.example.ui.LessonRunnerUiState
import com.example.ui.components.MasteryBadgePill
import com.example.ui.components.TtsListenControlBar
import com.example.ui.theme.BadgeAmber
import com.example.ui.theme.BadgeAmberBg
import com.example.ui.theme.BadgeGreen
import com.example.ui.theme.BadgeGreenBg

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    state: LessonRunnerUiState,
    isSpeaking: Boolean,
    isSlowMode: Boolean,
    englishVoiceLabel: String,
    filipinoVoiceLabel: String,
    onSpeak: (text: String, language: ReadingLanguage) -> Unit,
    onStopSpeaking: () -> Unit,
    onToggleSlowMode: () -> Unit,
    onSelectOption: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onReplayModule: () -> Unit,
    onOpenReport: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler {
        onStopSpeaking()
        onBack()
    }

    val module = state.module
    if (module == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onBack) { Text("Back to Dashboard") }
        }
        return
    }

    var showPassage by remember(module.id) { mutableStateOf(true) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onStopSpeaking()
                            onBack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lesson_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = module.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = module.workbookRef,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (state.isCompleted) {
                LessonCompleteContent(
                    state = state,
                    onReplay = onReplayModule,
                    onOpenReport = onOpenReport,
                    onBackToDashboard = onBack,
                    modifier = Modifier.widthIn(max = 640.dp),
                )
            } else {
                val question = state.currentQuestion ?: return@Box
                val voiceLabel = if (question.language == ReadingLanguage.ENGLISH) {
                    englishVoiceLabel
                } else {
                    filipinoVoiceLabel
                }
                val totalCount = state.questions.size.coerceAtLeast(1)
                val progressFraction = (state.currentIndex + 1).toFloat() / totalCount.toFloat()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // Progress Header
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Item ${state.currentIndex + 1} of $totalCount",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (state.attemptNumber == 1) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        BadgeAmberBg
                                    },
                                ) {
                                    Text(
                                        text = if (state.attemptNumber == 1) {
                                            "Try 1 of 2"
                                        } else {
                                            "Try 2 of 2 (Hint Active)"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.attemptNumber == 1) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            BadgeAmber
                                        },
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                            )
                        }
                    }

                    // Passage / Story Card
                    module.passage?.let { passage ->
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                            )
                                            Text(
                                                text = passage.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        TextButton(
                                            onClick = { showPassage = !showPassage },
                                            modifier = Modifier.testTag("toggle_passage_button"),
                                        ) {
                                            Text(if (showPassage) "Hide" else "Show Story")
                                        }
                                    }

                                    AnimatedVisibility(visible = showPassage) {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(
                                                text = passage.body,
                                                style = MaterialTheme.typography.bodyLarge,
                                            )
                                            OutlinedButton(
                                                onClick = { onSpeak(passage.body, passage.language) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("read_passage_tts_button"),
                                            ) {
                                                Text(
                                                    text = if (passage.language == ReadingLanguage.ENGLISH) {
                                                        "Read Story Aloud (PH English)"
                                                    } else {
                                                        "Basahin ang Kuwento (Filipino)"
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Question & TTS Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        text = question.kind.badgeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }

                                Text(
                                    text = question.prompt,
                                    style = MaterialTheme.typography.headlineSmall,
                                )

                                question.visualCue?.let { cue ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                    ) {
                                        Text(
                                            text = cue,
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(14.dp),
                                        )
                                    }
                                }

                                val textToSpeak = question.speakText ?: question.prompt
                                TtsListenControlBar(
                                    language = question.language,
                                    voiceLabel = voiceLabel,
                                    isSpeaking = isSpeaking,
                                    isSlowMode = isSlowMode,
                                    buttonLabel = question.speakLabel,
                                    onSpeakClick = { onSpeak(textToSpeak, question.language) },
                                    onStopClick = onStopSpeaking,
                                    onToggleSlow = onToggleSlowMode,
                                )
                            }
                        }
                    }

                    // Answer Options
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            question.options.forEachIndexed { idx, option ->
                                val isSelected = state.selectedOption == option
                                val isCorrectOption = option == question.correctAnswer
                                val showOutcomeColors = state.isCurrentSolved || state.isCurrentFailedBothTries

                                val containerColor = when {
                                    showOutcomeColors && isCorrectOption -> BadgeGreenBg
                                    showOutcomeColors && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.errorContainer
                                    !showOutcomeColors && isSelected && state.attemptNumber == 2 -> BadgeAmberBg
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val borderColor = when {
                                    showOutcomeColors && isCorrectOption -> BadgeGreen
                                    showOutcomeColors && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                                    !showOutcomeColors && isSelected && state.attemptNumber == 2 -> BadgeAmber
                                    else -> MaterialTheme.colorScheme.outline
                                }
                                val textColor = when {
                                    showOutcomeColors && isCorrectOption -> BadgeGreen
                                    showOutcomeColors && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Surface(
                                    onClick = { onSelectOption(option) },
                                    enabled = !state.isCurrentSolved && !state.isCurrentFailedBothTries && !(state.attemptNumber == 2 && isSelected),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                        .testTag("answer_option_$idx"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = containerColor,
                                    border = BorderStroke(1.5.dp, borderColor),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = option,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = textColor,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (showOutcomeColors && isCorrectOption) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Correct answer",
                                                tint = BadgeGreen,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Try 1 Hint Banner
                    if (state.showHintBanner) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hint_banner_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = BadgeAmberBg),
                                border = BorderStroke(1.dp, BadgeAmber),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = BadgeAmber,
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Try 1 Hint — You have 1 more try!",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = BadgeAmber,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = question.hint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Try 2 Mini-Lesson / Feedback Banner
                    if (state.isCurrentSolved || state.isCurrentFailedBothTries) {
                        item {
                            val solved = state.isCurrentSolved
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("mini_lesson_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (solved) BadgeGreenBg else MaterialTheme.colorScheme.errorContainer,
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (solved) BadgeGreen else MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (solved) Icons.Default.CheckCircle else Icons.Default.School,
                                            contentDescription = null,
                                            tint = if (solved) BadgeGreen else MaterialTheme.colorScheme.error,
                                        )
                                        Text(
                                            text = if (solved) {
                                                if (state.attemptNumber == 1) "Awesome! Correct on Try 1!" else "Great recovery on Try 2!"
                                            } else {
                                                "Correct Answer: ${question.correctAnswer} (Logged for Review)"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (solved) BadgeGreen else MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }

                                    Text(
                                        text = question.miniLesson,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedButton(
                                            onClick = { onSpeak(question.miniLesson, question.language) },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("speak_minilesson_button"),
                                        ) {
                                            Text("Hear Mini-Lesson")
                                        }
                                        Button(
                                            onClick = onNextQuestion,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("next_question_button"),
                                        ) {
                                            Text(
                                                if (state.currentIndex + 1 < totalCount) "Next Item" else "Finish Lesson",
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonCompleteContent(
    state: LessonRunnerUiState,
    onReplay: () -> Unit,
    onOpenReport: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = state.questions.size.coerceAtLeast(1)
    val pct = (state.correctCount * 100) / total

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = state.earnedTier.color,
                        modifier = Modifier.size(64.dp),
                    )
                    Text(
                        text = if (state.isRemediationDrill) "Practice Complete!" else "Session Complete!",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    MasteryBadgePill(tier = state.earnedTier)
                    Text(
                        text = "Score: ${state.correctCount} / $total ($pct%)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "First-try mastery: ${state.firstTryCorrectCount} / $total items",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = onBackToDashboard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("complete_back_dashboard_button"),
                    ) {
                        Text("Back to Workbook Hub")
                    }
                    FilledTonalButton(
                        onClick = onOpenReport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("complete_open_report_button"),
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View PDF Report & Send to Messenger")
                    }
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("complete_replay_button"),
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Try Again")
                    }
                }
            }
        }
    }
}
