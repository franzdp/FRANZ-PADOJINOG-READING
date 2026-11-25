package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.ui.components.BagacayCreditFooter
import com.example.ui.components.BagacaySchoolHeader

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    learners: List<LearnerEntity>,
    onCheckInSubmit: (fullName: String, gradeLevel: Int, section: String) -> Unit,
    onResumeLearner: (LearnerEntity) -> Unit,
    onDeleteLearner: (String) -> Unit,
    onOpenBackupRestore: () -> Unit,
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var gradeLevel by rememberSaveable { mutableIntStateOf(1) }
    var section by rememberSaveable { mutableStateOf("Section A") }
    var gradeMenuExpanded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LearnerEntity?>(null) }

    val keyStage = if (gradeLevel <= 3) "KS1 (Grades 1–3)" else "KS2 (Grades 4–6)"

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
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    BagacaySchoolHeader(
                        subtitle = "Offline Reading Remediation • KS1 & KS2",
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

                if (learners.isNotEmpty()) {
                    item {
                        Text(
                            text = "Returning Learners — Tap to Resume",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    items(learners, key = { it.id }) { learner ->
                        val track = ReadingTrack.fromId(learner.preferredTrackId)
                        val lastMod = learner.lastModuleId?.let { CurriculumRepository.getModuleById(it) }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("returning_learner_card_${learner.id}"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = learner.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "Grade ${learner.gradeLevel} (${learner.keyStage}) • ${learner.section}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = if (lastMod != null) {
                                            "Resume: ${track.shortTitle} — ${lastMod.title}"
                                        } else {
                                            "Track: ${track.title}"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
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
                                Button(
                                    onClick = { onResumeLearner(learner) },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("resume_learner_button_${learner.id}"),
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            }
                        }
                    }
                }

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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Grade Level",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Text(
                                            text = keyStage,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        )
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = gradeMenuExpanded,
                                    onExpandedChange = { gradeMenuExpanded = !gradeMenuExpanded },
                                ) {
                                    OutlinedTextField(
                                        value = "Grade $gradeLevel (${if (gradeLevel <= 3) "KS1" else "KS2"})",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Grade Level") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = gradeMenuExpanded)
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .testTag("grade_level_dropdown"),
                                    )
                                    ExposedDropdownMenu(
                                        expanded = gradeMenuExpanded,
                                        onDismissRequest = { gradeMenuExpanded = false },
                                    ) {
                                        (1..6).forEach { grade ->
                                            val stage = if (grade <= 3) "KS1" else "KS2"
                                            DropdownMenuItem(
                                                text = { Text("Grade $grade ($stage)") },
                                                onClick = {
                                                    gradeLevel = grade
                                                    gradeMenuExpanded = false
                                                },
                                                modifier = Modifier.testTag("grade_dropdown_item_$grade"),
                                            )
                                        }
                                    }
                                }

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    (1..6).forEach { grade ->
                                        FilterChip(
                                            selected = gradeLevel == grade,
                                            onClick = { gradeLevel = grade },
                                            label = { Text("Grade $grade") },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("grade_chip_$grade"),
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = section,
                                onValueChange = { section = it },
                                label = { Text("Section") },
                                placeholder = { Text("e.g., Sampaguita / Rizal") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("learner_section_input"),
                            )

                            Button(
                                onClick = { onCheckInSubmit(fullName, gradeLevel, section) },
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

                item {
                    BagacayCreditFooter()
                }
            }
        }
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
