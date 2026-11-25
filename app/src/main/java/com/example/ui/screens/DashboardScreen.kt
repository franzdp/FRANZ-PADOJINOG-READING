package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.ReadingLanguage
import com.example.curriculum.CurriculumRepository
import com.example.curriculum.ReadingModule
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
fun DashboardScreen(
    learner: LearnerEntity,
    selectedTrack: ReadingTrack,
    progressList: List<SessionProgressEntity>,
    wrongItems: List<WrongItemEntity>,
    onSelectTrack: (ReadingTrack) -> Unit,
    onStartModule: (String) -> Unit,
    onSwitchLearner: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenBackupRestore: () -> Unit,
) {
    val modules = remember(selectedTrack) { CurriculumRepository.getModulesForTrack(selectedTrack) }
    val progressByModuleId = remember(progressList) { progressList.associateBy { it.moduleId } }
    val resumeModule = remember(selectedTrack, learner.lastModuleId, progressList) {
        CurriculumRepository.resolveResumeModule(selectedTrack, learner.lastModuleId, progressList)
    }

    val countsByTier = remember(progressList) {
        val completedCounts = MasteryBadgeTier.entries.associateWith { tier ->
            progressList.count { it.badgeTier == tier.tierIndex }
        }.toMutableMap()
        val unstarted = (CurriculumRepository.getAllModules().size - progressList.size).coerceAtLeast(0)
        completedCounts[MasteryBadgeTier.SUPPORT_GRAY] =
            (completedCounts[MasteryBadgeTier.SUPPORT_GRAY] ?: 0) + unstarted
        completedCounts
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = learner.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Grade ${learner.gradeLevel} (${learner.keyStage}) • ${learner.section}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenBackupRestore,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_backup_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = stringResource(R.string.cd_backup_restore),
                        )
                    }
                    IconButton(
                        onClick = onOpenReport,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("dashboard_report_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = stringResource(R.string.cd_progress_report),
                        )
                    }
                    IconButton(
                        onClick = onSwitchLearner,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("switch_learner_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch learner",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenReport,
                modifier = Modifier.testTag("fab_pdf_messenger"),
                icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                text = { Text("PDF Report • Messenger") },
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Resume Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Ready to Resume",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Text(
                                    text = selectedTrack.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                )
                            }
                            Text(
                                text = resumeModule.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Text(
                                text = resumeModule.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            )
                            Button(
                                onClick = { onStartModule(resumeModule.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("resume_next_session_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary,
                                ),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Continue Lesson", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Repeated-Wrong Alert Strip (if any)
                if (wrongItems.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenReport)
                                .testTag("repeated_wrong_alert_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${wrongItems.size} Repeated-Wrong Item(s) Tracked",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = "Tap to review, practice, or send the PDF report to Messenger.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                // 5-Color Mastery Legend
                item {
                    FiveColorMasteryLegend(countsByTier = countsByTier)
                }

                // Activity Track Selector (KS1-English, KS1-Filipino, KS2-Basic, KS2-Plus)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Select Activities (${learner.keyStage} Recommended)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        val rows = ReadingTrack.entries.chunked(2)
                        rows.forEach { rowTracks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                rowTracks.forEach { track ->
                                    val isSelected = selectedTrack == track
                                    val isRecommended = track.keyStage == learner.keyStage
                                    Surface(
                                        onClick = { onSelectTrack(track) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("track_chip_${track.id}"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                            },
                                        ),
                                        tonalElevation = if (isSelected) 3.dp else 0.dp,
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = track.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                )
                                                if (isRecommended) {
                                                    Text(
                                                        text = "★",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.tertiary,
                                                    )
                                                }
                                            }
                                            Text(
                                                text = track.gradeRangeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${selectedTrack.title} (${selectedTrack.gradeRangeLabel}) • ${selectedTrack.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Module Cards
                items(modules, key = { it.id }) { module ->
                    ModuleCard(
                        module = module,
                        progress = progressByModuleId[module.id],
                        onClick = { onStartModule(module.id) },
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    BagacayCreditFooter()
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(
    module: ReadingModule,
    progress: SessionProgressEntity?,
    onClick: () -> Unit,
) {
    val tier = if (progress != null && progress.attemptsCount > 0) {
        MasteryBadgeTier.fromTierIndex(progress.badgeTier)
    } else {
        MasteryBadgeTier.SUPPORT_GRAY
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("module_card_${module.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, tier.color.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (module.isCheckIn) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ) {
                        Text(
                            text = if (module.isCheckIn) "Week ${module.weekNumber} • Check-In" else "Week ${module.weekNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (module.isCheckIn) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = if (module.language == ReadingLanguage.ENGLISH) "PH English" else "Filipino",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                MasteryBadgePill(tier = tier, compact = true)
            }

            Text(
                text = module.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = module.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = module.workbookRef,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (progress != null && progress.attemptsCount > 0) {
                    Text(
                        text = "Best: ${progress.bestScore}/${progress.totalQuestions} (${progress.attemptsCount}x)",
                        style = MaterialTheme.typography.labelMedium,
                        color = tier.color,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        text = "${module.questions.size} items • Tap to start",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
