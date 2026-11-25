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
    onSendToMessenger: () -> Unit,
    onPreviewPdf: () -> Unit,
    onSavePdf: () -> Unit,
    onPracticeWrongItems: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }

    val countsByTier = remember(progressList) {
        MasteryBadgeTier.entries.associateWith { tier ->
            progressList.count { it.badgeTier == tier.tierIndex }
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
                            text = "${learner.fullName} • Grade ${learner.gradeLevel} (${learner.keyStage})",
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
                            text = "Repeated-Wrong Tracking (${wrongItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (wrongItems.isNotEmpty()) {
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

                if (wrongItems.isEmpty()) {
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
                    items(wrongItems, key = { it.itemId }) { item ->
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

                // Completed Sessions Breakdown
                item {
                    Text(
                        text = "Session Scores & Badges (${progressList.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (progressList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Text(
                                text = "Complete any workbook week or Check-In to see session badges here.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(progressList, key = { it.moduleId }) { progress ->
                        val module = CurriculumRepository.getModuleById(progress.moduleId)
                        val tier = MasteryBadgeTier.fromTierIndex(progress.badgeTier)
                        val track = ReadingTrack.fromId(progress.trackId)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "[${track.shortTitle}] ${module?.title ?: progress.moduleId}",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = "Best: ${progress.bestScore}/${progress.totalQuestions} • First-Try: ${progress.firstTryCorrectCount} • Attempts: ${progress.attemptsCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                MasteryBadgePill(tier = tier, compact = true)
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
