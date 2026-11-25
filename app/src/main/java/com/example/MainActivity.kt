package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AralViewModel
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.screens.CheckInScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LessonScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.theme.AralReadingTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object Routes {
    const val CHECK_IN = "check_in"
    const val DASHBOARD = "dashboard"
    const val LESSON = "lesson"
    const val REPORT = "report"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AralReadingTheme {
                AralReadingApp()
            }
        }
    }
}

@Composable
fun AralReadingApp(
    viewModel: AralViewModel = viewModel(),
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val learners by viewModel.learners.collectAsStateWithLifecycle()
    val activeLearner by viewModel.activeLearner.collectAsStateWithLifecycle()
    val activeProgress by viewModel.activeProgress.collectAsStateWithLifecycle()
    val activeWrongItems by viewModel.activeWrongItems.collectAsStateWithLifecycle()
    val selectedTrack by viewModel.selectedTrack.collectAsStateWithLifecycle()
    val lessonState by viewModel.lessonState.collectAsStateWithLifecycle()

    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsStateWithLifecycle()
    val isSlowMode by viewModel.ttsManager.slowMode.collectAsStateWithLifecycle()
    val englishVoiceLabel by viewModel.ttsManager.englishVoiceLabel.collectAsStateWithLifecycle()
    val filipinoVoiceLabel by viewModel.ttsManager.filipinoVoiceLabel.collectAsStateWithLifecycle()

    var showBackupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.statusMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(context, uri)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(context, uri)
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            viewModel.savePdfToUri(context, uri)
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { hostPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(hostPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.CHECK_IN,
            ) {
                composable(Routes.CHECK_IN) {
                    CheckInScreen(
                        learners = learners,
                        onCheckInSubmit = { name, grade, section ->
                            viewModel.checkInAndActivate(name, grade, section) {
                                navController.navigate(Routes.DASHBOARD)
                            }
                        },
                        onResumeLearner = { learner ->
                            viewModel.resumeReturningLearner(learner) {
                                navController.navigate(Routes.DASHBOARD)
                            }
                        },
                        onDeleteLearner = { id -> viewModel.deleteLearner(id) },
                        onOpenBackupRestore = { showBackupDialog = true },
                    )
                }

                composable(Routes.DASHBOARD) {
                    val learner = activeLearner
                    if (learner == null) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack(Routes.CHECK_IN, inclusive = false)
                        }
                    } else {
                        DashboardScreen(
                            learner = learner,
                            selectedTrack = selectedTrack,
                            progressList = activeProgress,
                            wrongItems = activeWrongItems,
                            onSelectTrack = viewModel::selectTrack,
                            onStartModule = { moduleId ->
                                viewModel.startModule(moduleId)
                                navController.navigate(Routes.LESSON)
                            },
                            onSwitchLearner = {
                                navController.popBackStack(Routes.CHECK_IN, inclusive = false)
                            },
                            onOpenReport = {
                                navController.navigate(Routes.REPORT)
                            },
                            onOpenBackupRestore = { showBackupDialog = true },
                        )
                    }
                }

                composable(Routes.LESSON) {
                    LessonScreen(
                        state = lessonState,
                        isSpeaking = isSpeaking,
                        isSlowMode = isSlowMode,
                        englishVoiceLabel = englishVoiceLabel,
                        filipinoVoiceLabel = filipinoVoiceLabel,
                        onSpeak = { text, lang -> viewModel.speakText(text, lang) },
                        onStopSpeaking = viewModel::stopSpeaking,
                        onToggleSlowMode = viewModel.ttsManager::toggleSlowMode,
                        onSelectOption = viewModel::submitAnswer,
                        onNextQuestion = viewModel::nextQuestionOrFinish,
                        onReplayModule = {
                            lessonState.module?.let { mod ->
                                if (lessonState.isRemediationDrill) {
                                    viewModel.startRepeatedWrongPractice()
                                } else {
                                    viewModel.startModule(mod.id)
                                }
                            }
                        },
                        onOpenReport = {
                            navController.navigate(Routes.REPORT)
                        },
                        onBack = {
                            navController.popBackStack()
                        },
                    )
                }

                composable(Routes.REPORT) {
                    val learner = activeLearner
                    if (learner == null) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack(Routes.CHECK_IN, inclusive = false)
                        }
                    } else {
                        ReportScreen(
                            learner = learner,
                            progressList = activeProgress,
                            wrongItems = activeWrongItems,
                            onSendToMessenger = { viewModel.generateAndSharePdfToMessenger(context) },
                            onPreviewPdf = { viewModel.generateAndPreviewPdf(context) },
                            onSavePdf = {
                                val safeName = learner.fullName.lowercase(Locale.US)
                                    .replace(Regex("[^a-z0-9]+"), "_")
                                    .trim('_')
                                    .ifBlank { "learner" }
                                savePdfLauncher.launch("ARAL_Reading_${safeName}_Report.pdf")
                            },
                            onPracticeWrongItems = {
                                if (viewModel.startRepeatedWrongPractice()) {
                                    navController.navigate(Routes.LESSON)
                                }
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    if (showBackupDialog) {
        BackupRestoreDialog(
            learnerCount = learners.size,
            onExportBackup = {
                val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                exportBackupLauncher.launch("ARAL_Reading_BagacayES_Backup_$stamp.json")
            },
            onRestoreBackup = {
                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onDismiss = { showBackupDialog = false },
        )
    }
}
