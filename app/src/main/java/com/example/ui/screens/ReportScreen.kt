package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.curriculum.CurriculumRepository
import com.example.curriculum.ReadingTrack
import com.example.data.InProgressSessionEntity
import com.example.data.LearnerEntity
import com.example.data.MasteryBadgeTier
import com.example.data.SessionProgressEntity
import com.example.data.WrongItemEntity
import com.example.ui.components.BagacayCreditFooter
import com.example.ui.components.FiveColorMasteryLegend
import com.example.ui.components.MasteryBadgePill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    learner: LearnerEntity,
    progressList: List<SessionProgressEntity>,
    wrongItems: List<WrongItemEntity>,
    inProgressList: List<InProgressSessionEntity> = emptyList(),
    onSendToMessenger: () -> Unit,
    onPreviewPdf: () -> Unit,
    onSavePdf: () -> Unit,
    onPracticeWrongItems: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val learnerProgress = remember(learner.id, progressList) {
        progressList.filter { it.learnerId == learner.id }
    }
    val learnerWrongItems = remember(learner.id, wrongItems) {
        wrongItems.filter { it.learnerId == learner.id }
    }
    val learnerInProgress = remember(learner.id, inProgressList) {
        inProgressList.filter { it.learnerId == learner.id }
    }
    val progressByModuleId = remember(learnerProgress) {
        learnerProgress.associateBy { it.moduleId }
    }
    val inProgressByModuleId = remember(learnerInProgress) {
        learnerInProgress.associateBy { it.moduleId }
    }

    val countsByTier = remember(learnerProgress) {
        MasteryBadgeTier.entries.associateWith { tier ->
            learnerProgress.count { it.attemptsCount > 0 && it.badgeTier == tier.tierIndex }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("report_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                title = {
                    Column {
                        Text("PDF Progress Report", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${learner.fullName} • Grade ${learner.gradeLevel} • ${learner.section}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Messenger & PDF Actions Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = "Send Report Straight to Messenger",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Creates a Bagacay ES PDF with 5-color mastery badges and repeated-wrong tracking, then attaches it directly to Facebook Messenger (or Share Sheet fallback).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Button(
                                onClick = onSendToMessenger,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("send_pdf_messenger_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Send PDF to Messenger", fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = onPreviewPdf,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("preview_pdf_button"),
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Preview PDF")
                                }
                                FilledTonalButton(
                                    onClick = onSavePdf,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("save_pdf_button"),
                                ) {
                                    Icon(Icons.Default.SaveAlt, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Save PDF")
                                }
                            }
                        }
                    }
                }

                item {
                    FiveColorMasteryLegend(countsByTier = countsByTier)
                }

                // Repeated-Wrong Items Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Repeated-Wrong Tracking (${learnerWrongItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (learnerWrongItems.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = onPracticeWrongItems,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("practice_wrong_items_button"),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Practice Missed")
                            }
                        }
                    }
                }

                if (learnerWrongItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Text(
                                text = "Great news! No repeated-wrong items have been logged for ${learner.fullName} yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(learnerWrongItems, key = { "${learner.id}_${it.itemId}" }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wrong_item_card_${item.itemId}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
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
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text(
                                            text = item.moduleTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.error,
                                    ) {
                                        Text(
                                            text = "Repeated ${item.repeatedWrongCount}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = item.promptSummary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Expected: ${item.expectedAnswer}  •  Last Answer: ${item.lastWrongAnswer}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // Activities Breakdown by Key Stage (KS1 & KS2)
                item {
                    Text(
                        text = "Activities by Key Stage (KS1 & KS2)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                val keyStages = listOf(
                    "KS1" to "Key Stage 1 (KS1) • Grades 1–3",
                    "KS2" to "Key Stage 2 (KS2) • Grades 4–6",
                )
                keyStages.forEach { (ksCode, ksTitle) ->
                    val ksTracks = ReadingTrack.forKeyStage(ksCode)
                    item(key = "ks_section_$ksCode") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = ksTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                ksTracks.forEach { track ->
                                    val trackModules = CurriculumRepository.getModulesForTrack(track)
                                    val trackDone = trackModules.count { (progressByModuleId[it.id]?.attemptsCount ?: 0) > 0 }
                                    val activeTrackItems = trackModules.filter {
                                        (progressByModuleId[it.id]?.attemptsCount ?: 0) > 0 || inProgressByModuleId.containsKey(it.id)
                                    }
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    text = "$trackDone / ${trackModules.size} completed",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                )
                                            }
                                            if (activeTrackItems.isEmpty()) {
                                                Text(
                                                    text = "No activities started yet in ${track.title} (all 8 weeks included in PDF).",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            } else {
                                                activeTrackItems.forEach { module ->
                                                    val progress = progressByModuleId[module.id]
                                                    val checkpoint = inProgressByModuleId[module.id]
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = module.title,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                            )
                                                            val detail = when {
                                                                progress != null && progress.attemptsCount > 0 ->
                                                                    "Best: ${progress.bestScore}/${progress.totalQuestions} • First-Try: ${progress.firstTryCorrectCount} • Tries: ${progress.attemptsCount}"
                                                                checkpoint != null -> {
                                                                    val itemNum = (checkpoint.currentIndex + 1).coerceIn(1, module.questions.size)
                                                                    "In Progress • Item $itemNum/${module.questions.size}"
                                                                }
                                                                else -> ""
                                                            }
                                                            Text(
                                                                text = detail,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            )
                                                        }
                                                        if (progress != null && progress.attemptsCount > 0) {
                                                            MasteryBadgePill(
                                                                tier = MasteryBadgeTier.fromTierIndex(progress.badgeTier),
                                                                compact = true,
                                                            )
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

                item {
                    BagacayCreditFooter()
                }
            }
        }
    }
}
