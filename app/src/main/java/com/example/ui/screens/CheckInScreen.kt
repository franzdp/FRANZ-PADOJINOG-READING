package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.LearnerEntity
import com.example.data.MasteryBadgeTier
import com.example.data.SessionProgressEntity
import com.example.ui.components.BagacayCreditFooter
import com.example.ui.components.BagacaySchoolHeader

private enum class CheckInTab {
    LEARNERS,
    NEW_CHECK_IN,
}

@Composable
fun CheckInScreen(
    learners: List<LearnerEntity>,
    progressByLearner: Map<String, List<SessionProgressEntity>>,
    wrongCountByLearner: Map<String, Int>,
    onCheckInSubmit: (fullName: String, gradeLevel: Int, section: String) -> Unit,
    onResumeLearner: (LearnerEntity) -> Unit,
    onUpdateLearner: (learnerId: String, fullName: String, gradeLevel: Int, section: String) -> Unit,
    onDeleteLearner: (String) -> Unit,
    onOpenBackupRestore: () -> Unit,
) {
    var activeTab by rememberSaveable {
        mutableStateOf(if (learners.isEmpty()) CheckInTab.NEW_CHECK_IN else CheckInTab.LEARNERS)
    }
    var initializedTab by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(learners.isEmpty()) {
        if (!initializedTab && learners.isNotEmpty()) {
            activeTab = CheckInTab.LEARNERS
            initializedTab = true
        } else if (learners.isEmpty()) {
            activeTab = CheckInTab.NEW_CHECK_IN
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var gradeFilter by rememberSaveable { mutableIntStateOf(0) } // 0 = All, 1..6 = Grade

    var fullName by rememberSaveable { mutableStateOf("") }
    var gradeLevel by rememberSaveable { mutableIntStateOf(1) }
    var section by rememberSaveable { mutableStateOf("") }

    var editingLearner by remember { mutableStateOf<LearnerEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<LearnerEntity?>(null) }

    val filteredLearners = remember(learners, searchQuery, gradeFilter) {
        val q = searchQuery.trim()
        learners.filter { learner ->
            val matchesGrade = gradeFilter == 0 || learner.gradeLevel == gradeFilter
            val matchesQuery = q.isBlank() ||
                learner.fullName.contains(q, ignoreCase = true) ||
                learner.section.contains(q, ignoreCase = true)
            matchesGrade && matchesQuery
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
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
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    BagacaySchoolHeader(
                        subtitle = "Offline Reading Remediation • Grades 1–6",
                        trailingContent = {
                            IconButton(
                                onClick = onOpenBackupRestore,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("checkin_backup_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = stringResource(R.string.cd_backup_restore),
                                    tint = androidx.compose.ui.graphics.Color.White,
                                )
                            }
                        },
                    )
                }

                // Clean Switcher between Returning Learners & New Check-In
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = activeTab == CheckInTab.LEARNERS,
                            onClick = { activeTab = CheckInTab.LEARNERS },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = { Icon(Icons.Default.Group, contentDescription = null) },
                            modifier = Modifier.testTag("tab_learners_button"),
                        ) {
                            Text("Learners (${learners.size})", maxLines = 1)
                        }
                        SegmentedButton(
                            selected = activeTab == CheckInTab.NEW_CHECK_IN,
                            onClick = { activeTab = CheckInTab.NEW_CHECK_IN },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                            modifier = Modifier.testTag("tab_new_checkin_button"),
                        ) {
                            Text("New Check-In", maxLines = 1)
                        }
                    }
                }

                if (activeTab == CheckInTab.LEARNERS) {
                    if (learners.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Text(
                                        text = "No Learners Checked In Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Add your first Grade 1–6 learner to start tracking their individual reading progress and mastery badges.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Button(
                                        onClick = { activeTab = CheckInTab.NEW_CHECK_IN },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("empty_add_learner_button"),
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Check In a Learner")
                                    }
                                }
                            }
                        }
                    } else {
                        // Classroom search & Grade 1-6 filter bar
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search learner name or section...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("learner_search_input"),
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    item {
                                        FilterChip(
                                            selected = gradeFilter == 0,
                                            onClick = { gradeFilter = 0 },
                                            label = { Text("All (${learners.size})") },
                                            modifier = Modifier.height(40.dp),
                                        )
                                    }
                                    items((1..6).toList()) { grade ->
                                        FilterChip(
                                            selected = gradeFilter == grade,
                                            onClick = {
                                                gradeFilter = if (gradeFilter == grade) 0 else grade
                                            },
                                            label = { Text("Grade $grade") },
                                            modifier = Modifier
                                                .height(40.dp)
                                                .testTag("filter_grade_chip_$grade"),
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredLearners.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                ) {
                                    Text(
                                        text = "No learners match that search or grade filter.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(18.dp),
                                    )
                                }
                            }
                        } else {
                            items(filteredLearners, key = { it.id }) { learner ->
                                val learnerProgress = progressByLearner[learner.id].orEmpty()
                                val completedCount = learnerProgress.count { it.attemptsCount > 0 }
                                val masteredCount = learnerProgress.count {
                                    it.badgeTier == MasteryBadgeTier.MASTERED_GREEN.tierIndex
                                }
                                val wrongCount = wrongCountByLearner[learner.id] ?: 0

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("returning_learner_card_${learner.id}"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "G${learner.gradeLevel}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                Text(
                                                    text = learner.fullName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = "Grade ${learner.gradeLevel} • ${learner.section}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            IconButton(
                                                onClick = { editingLearner = learner },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("edit_learner_button_${learner.id}"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit ${learner.fullName}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            IconButton(
                                                onClick = { pendingDelete = learner },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_learner_button_${learner.id}"),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Remove learner ${learner.fullName}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            val statsText = buildString {
                                                if (completedCount == 0) {
                                                    append("No completed sessions yet")
                                                } else {
                                                    append("$completedCount done • $masteredCount mastered")
                                                }
                                                if (wrongCount > 0) {
                                                    append(" • $wrongCount to review")
                                                }
                                            }
                                            Text(
                                                text = statsText,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (wrongCount > 0) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.primary
                                                },
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Button(
                                                onClick = { onResumeLearner(learner) },
                                                modifier = Modifier
                                                    .height(48.dp)
                                                    .testTag("resume_learner_button_${learner.id}"),
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                                Spacer(Modifier.width(4.dp))
                                                Text("Open")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // New Learner Check-In Tab (Grade 1 to Grade 6 only)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "Learner Check-In",
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                }

                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = { fullName = it },
                                    label = { Text("Learner Full Name") },
                                    placeholder = { Text("e.g., Juan Dela Cruz") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("learner_name_input"),
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Grade Level (Grade 1 – Grade 6)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    GradeLevelSelectorGrid(
                                        selectedGrade = gradeLevel,
                                        onSelectGrade = { gradeLevel = it },
                                        tagPrefix = "grade_chip",
                                    )
                                }

                                OutlinedTextField(
                                    value = section,
                                    onValueChange = { section = it },
                                    label = { Text("Section (Optional)") },
                                    placeholder = { Text("e.g., Sampaguita or Section A") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("learner_section_input"),
                                )

                                Button(
                                    onClick = {
                                        val submittedName = fullName
                                        val submittedGrade = gradeLevel
                                        val submittedSection = section.ifBlank { "Section A" }
                                        fullName = ""
                                        section = ""
                                        onCheckInSubmit(submittedName, submittedGrade, submittedSection)
                                    },
                                    enabled = fullName.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("checkin_submit_button"),
                                ) {
                                    Text("Check In & Start Reading")
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
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

    editingLearner?.let { target ->
        var editName by remember(target.id) { mutableStateOf(target.fullName) }
        var editGrade by remember(target.id) { mutableIntStateOf(target.gradeLevel.coerceIn(1, 6)) }
        var editSection by remember(target.id) { mutableStateOf(target.section) }

        AlertDialog(
            onDismissRequest = { editingLearner = null },
            title = { Text("Edit Learner Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Grade Level",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    GradeLevelSelectorGrid(
                        selectedGrade = editGrade,
                        onSelectGrade = { editGrade = it },
                        tagPrefix = "edit_grade_chip",
                    )
                    OutlinedTextField(
                        value = editSection,
                        onValueChange = { editSection = it },
                        label = { Text("Section") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateLearner(target.id, editName, editGrade, editSection)
                        editingLearner = null
                    },
                    enabled = editName.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingLearner = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    pendingDelete?.let { victim ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove Learner Record?") },
            text = {
                Text("This will permanently remove ${victim.fullName}'s local progress on this device. Export a JSON Backup first if you want a copy.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteLearner(victim.id)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_learner_button"),
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun GradeLevelSelectorGrid(
    selectedGrade: Int,
    onSelectGrade: (Int) -> Unit,
    tagPrefix: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6)).forEach { rowGrades ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowGrades.forEach { grade ->
                    val isSelected = selectedGrade == grade
                    Surface(
                        onClick = { onSelectGrade(grade) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("${tagPrefix}_$grade"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            },
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Grade $grade",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
