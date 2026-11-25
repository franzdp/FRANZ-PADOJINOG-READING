package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
fun DashboardScreen(
    learner: LearnerEntity,
    selectedTrack: ReadingTrack,
    progressList: List<SessionProgressEntity>,
    wrongItems: List<WrongItemEntity>,
    inProgressList: List<InProgressSessionEntity> = emptyList(),
    onSelectTrack: (ReadingTrack) -> Unit,
    onStartModule: (String) -> Unit,
    onSwitchLearner: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenBackupRestore: () -> Unit,
) {
    BackHandler { onSwitchLearner() }

    val learnerProgress = remember(learner.id, progressList) {
        progressList.filter { it.learnerId == learner.id }
    }
    val learnerWrongItems = remember(learner.id, wrongItems) {
        wrongItems.filter { it.learnerId == learner.id }
    }
    val learnerInProgress = remember(learner.id, inProgressList) {
        inProgressList.filter { it.learnerId == learner.id }
    }

    val modules = remember(selectedTrack) { CurriculumRepository.getModulesForTrack(selectedTrack) }
    val trackProgress = remember(selectedTrack, learnerProgress) {
        learnerProgress.filter { it.trackId == selectedTrack.id }
    }
    val progressByModuleId = remember(trackProgress) {
        trackProgress.associateBy { it.moduleId }
    }
    val inProgressByModuleId = remember(selectedTrack, learnerInProgress) {
        learnerInProgress.filter { it.trackId == selectedTrack.id }.associateBy { it.moduleId }
    }
    val completedInTrack = remember(trackProgress) {
        trackProgress.count { it.attemptsCount > 0 }
    }
    val resumeModule = remember(selectedTrack, learner.lastModuleId, trackProgress, inProgressByModuleId) {
        CurriculumRepository.resolveResumeModule(
            track = selectedTrack,
            lastModuleId = learner.lastModuleId,
            progressList = trackProgress,
            inProgressModuleIds = inProgressByModuleId.keys,
        )
    }
    val resumeCheckpoint = inProgressByModuleId[resumeModule.id]
    val resumeNextItem = remember(resumeModule, resumeCheckpoint) {
        computeNextItemNumber(resumeModule, resumeCheckpoint)
    }

    val countsByTier = remember(modules, trackProgress) {
        val completedCounts = MasteryBadgeTier.entries.associateWith { tier ->
            trackProgress.count { it.attemptsCount > 0 && it.badgeTier == tier.tierIndex }
        }.toMutableMap()
        val startedCount = trackProgress.count { it.attemptsCount > 0 }
        val unstarted = (modules.size - startedCount).coerceAtLeast(0)
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
                            text = "Grade ${learner.gradeLevel} • ${learner.section}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onSwitchLearner,
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("switch_learner_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Switch", style = MaterialTheme.typography.labelLarge)
                    }
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
                },
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                    label = { Text("Activities") },
                    modifier = Modifier.testTag("nav_activities_tab"),
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenReport,
                    icon = {
                        BadgedBox(
                            badge = {
                                if (learnerWrongItems.isNotEmpty()) {
                                    Badge { Text("${learnerWrongItems.size}") }
                                }
                            },
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null)
                        }
                    },
                    label = { Text("PDF Report") },
                    modifier = Modifier.testTag("fab_pdf_messenger"),
                )
            }
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
                // Activity Selector (KS1 and KS2 appear ONLY in the activity)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Select Activity",
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
                                    val trackDoneCount = learnerProgress.count {
                                        it.trackId == track.id && it.attemptsCount > 0
                                    }
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
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                                            Text(
                                                text = "$trackDoneCount / 8 done",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Text(
                            text = "${selectedTrack.title} • ${selectedTrack.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Resume / Next Lesson Banner for this Learner & Selected Activity
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
                                    text = when {
                                        resumeNextItem != null -> "In Progress • Item $resumeNextItem of ${resumeModule.questions.size}"
                                        completedInTrack > 0 -> "Ready to Resume"
                                        else -> "Start Activity"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Text(
                                    text = "$completedInTrack / ${modules.size} weeks",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
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
                            LinearProgressIndicator(
                                progress = {
                                    if (modules.isEmpty()) 0f else completedInTrack.toFloat() / modules.size.toFloat()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
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
                                Text(
                                    text = when {
                                        resumeNextItem != null -> "Resume Item $resumeNextItem/${resumeModule.questions.size}"
                                        completedInTrack > 0 -> "Continue Lesson"
                                        else -> "Start Week 1"
                                    },
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Repeated-Wrong Alert Strip for this Learner
                if (learnerWrongItems.isNotEmpty()) {
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
                                        text = "${learnerWrongItems.size} Repeated-Wrong Item(s) for ${learner.fullName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = "Tap to practice missed items or send PDF report to Messenger.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                // 5-Color Mastery Legend scoped to the selected activity
                item {
                    FiveColorMasteryLegend(countsByTier = countsByTier)
                }

                item {
                    Text(
                        text = "${selectedTrack.title} Weeks (${modules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Module Cards
                items(modules, key = { "${learner.id}_${it.id}" }) { module ->
                    ModuleCard(
                        module = module,
                        progress = progressByModuleId[module.id],
                        inProgress = inProgressByModuleId[module.id],
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

private fun computeNextItemNumber(
    module: ReadingModule,
    checkpoint: InProgressSessionEntity?,
): Int? {
    if (checkpoint == null || module.questions.isEmpty()) return null
    val safeIndex = checkpoint.currentIndex.coerceIn(0, module.questions.lastIndex)
    val itemFinished = checkpoint.isCurrentSolved || checkpoint.isCurrentFailedBothTries
    val targetIndex = if (itemFinished && safeIndex + 1 < module.questions.size) safeIndex + 1 else safeIndex
    return targetIndex + 1
}

@Composable
private fun ModuleCard(
    module: ReadingModule,
    progress: SessionProgressEntity?,
    inProgress: InProgressSessionEntity?,
    onClick: () -> Unit,
) {
    val tier = if (progress != null && progress.attemptsCount > 0) {
        MasteryBadgeTier.fromTierIndex(progress.badgeTier)
    } else {
        MasteryBadgeTier.SUPPORT_GRAY
    }
    val nextItemNum = computeNextItemNumber(module, inProgress)

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
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
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
                when {
                    nextItemNum != null && progress != null && progress.attemptsCount > 0 -> {
                        Text(
                            text = "Resume Item $nextItemNum/${module.questions.size} • Best: ${progress.bestScore}/${progress.totalQuestions}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    nextItemNum != null -> {
                        Text(
                            text = "In Progress • Item $nextItemNum/${module.questions.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    progress != null && progress.attemptsCount > 0 -> {
                        Text(
                            text = "Best: ${progress.bestScore}/${progress.totalQuestions} (${progress.attemptsCount}x)",
                            style = MaterialTheme.typography.labelMedium,
                            color = tier.color,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    else -> {
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
}
