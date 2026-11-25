package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.ReadingLanguage
import com.example.data.MasteryBadgeTier
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.TealBright
import com.example.ui.theme.WarmGold

@Composable
fun BagacaySchoolHeader(
    modifier: Modifier = Modifier,
    subtitle: String = stringResource(R.string.program_subtitle),
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(RoyalBlue, CobaltBlue, TealBright),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bagacay_logo),
                    contentDescription = stringResource(R.string.cd_bagacay_logo),
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.school_full_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = WarmGold,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "ARAL-Reading App",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}

@Composable
fun BagacayCreditFooter(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.bagacay_logo),
                contentDescription = stringResource(R.string.cd_bagacay_logo),
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.school_credit),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "KS1 (Grades 1–3) & KS2 (Grades 4–6) • District of Culasi • Offline-Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

fun MasteryBadgeTier.icon(): ImageVector = when (this) {
    MasteryBadgeTier.SUPPORT_GRAY -> Icons.Outlined.Circle
    MasteryBadgeTier.EMERGING_RED -> Icons.Outlined.Info
    MasteryBadgeTier.DEVELOPING_AMBER -> Icons.Default.TrendingUp
    MasteryBadgeTier.PROFICIENT_BLUE -> Icons.Default.Star
    MasteryBadgeTier.MASTERED_GREEN -> Icons.Default.EmojiEvents
}

@Composable
fun MasteryBadgePill(
    tier: MasteryBadgeTier,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = tier.containerColor,
        border = BorderStroke(1.dp, tier.color.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 5.dp else 6.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(tier.color),
            )
            Icon(
                imageVector = tier.icon(),
                contentDescription = tier.title,
                tint = tier.color,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = if (compact) tier.shortLabel else "${tier.shortLabel} (${tier.rangeHint})",
                style = MaterialTheme.typography.labelMedium,
                color = tier.color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiveColorMasteryLegend(
    countsByTier: Map<MasteryBadgeTier, Int>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "5-Color Mastery Badges",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MasteryBadgeTier.entries.reversed().forEach { tier ->
                    val count = countsByTier[tier] ?: 0
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tier.containerColor,
                        border = BorderStroke(1.dp, tier.color.copy(alpha = 0.4f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(tier.color),
                            )
                            Text(
                                text = "$count ${tier.shortLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                color = tier.color,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = tier.rangeHint,
                                style = MaterialTheme.typography.labelSmall,
                                color = tier.color.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TtsListenControlBar(
    language: ReadingLanguage,
    voiceLabel: String,
    isSpeaking: Boolean,
    isSlowMode: Boolean,
    buttonLabel: String,
    onSpeakClick: () -> Unit,
    onStopClick: () -> Unit,
    onToggleSlow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSpeaking) {
                FilledTonalButton(
                    onClick = onStopClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("stop_tts_button"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.cd_speak_word))
                    Spacer(Modifier.width(6.dp))
                    Text("Stop Audio", maxLines = 1)
                }
            } else {
                Button(
                    onClick = onSpeakClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("speak_tts_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(R.string.cd_speak_word),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(buttonLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            AssistChip(
                onClick = onToggleSlow,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("toggle_tts_speed_chip"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Text(if (isSlowMode) "Slow" else "Normal")
                },
            )
        }
        Text(
            text = if (language == ReadingLanguage.ENGLISH) {
                "Voice: $voiceLabel"
            } else {
                "Boses: $voiceLabel"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun BackupRestoreDialog(
    learnerCount: Int,
    onExportBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text("Records Preservation & Backup")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Your Bagacay ES learner profiles ($learnerCount saved), 5-color mastery badges, and repeated-wrong histories are stored safely in an upgrade-safe local database so app updates keep all progress.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Use JSON Backup & Restore anytime to save a copy to Google Drive / Downloads or transfer records to another tablet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onExportBackup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_backup_button"),
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export JSON Backup")
                }
                OutlinedButton(
                    onClick = onRestoreBackup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restore_backup_button"),
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restore from JSON Backup")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_backup_dialog_button"),
            ) {
                Text("Close")
            }
        },
    )
}
