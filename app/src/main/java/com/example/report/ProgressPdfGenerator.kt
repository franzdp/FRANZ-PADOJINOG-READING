package com.example.report

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.R
import com.example.curriculum.CurriculumRepository
import com.example.curriculum.ReadingModule
import com.example.curriculum.ReadingTrack
import com.example.data.InProgressSessionEntity
import com.example.data.LearnerEntity
import com.example.data.MasteryBadgeTier
import com.example.data.SessionProgressEntity
import com.example.data.WrongItemEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PdfGenerationResult(
    val file: File,
    val contentUri: Uri,
    val summaryText: String,
)

object ProgressPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    private data class KeyStageSection(
        val keyStageCode: String,
        val headerLabel: String,
        val tracks: List<ReadingTrack>,
    )

    private val keyStageSections = listOf(
        KeyStageSection(
            keyStageCode = "KS1",
            headerLabel = "KEY STAGE 1 (KS1) • Grades 1–3",
            tracks = listOf(ReadingTrack.KS1_ENGLISH, ReadingTrack.KS1_FILIPINO),
        ),
        KeyStageSection(
            keyStageCode = "KS2",
            headerLabel = "KEY STAGE 2 (KS2) • Grades 4–6",
            tracks = listOf(ReadingTrack.KS2_BASIC, ReadingTrack.KS2_PLUS),
        ),
    )

    fun generateProgressReport(
        context: Context,
        learner: LearnerEntity,
        progressList: List<SessionProgressEntity>,
        wrongItems: List<WrongItemEntity>,
        inProgressList: List<InProgressSessionEntity> = emptyList(),
    ): PdfGenerationResult {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = learner.fullName.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "learner" }
        val pdfFile = File(reportsDir, "Reading_${safeName}_Report.pdf")

        val renderedNative = runCatching {
            renderWithAndroidPdfDocument(context, pdfFile, learner, progressList, wrongItems, inProgressList)
        }.isSuccess

        if (!renderedNative) {
            renderFallbackPdf(pdfFile, learner, progressList, wrongItems, inProgressList)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )

        val summary = buildSummaryText(learner, progressList, wrongItems)
        return PdfGenerationResult(file = pdfFile, contentUri = uri, summaryText = summary)
    }

    private fun renderWithAndroidPdfDocument(
        context: Context,
        pdfFile: File,
        learner: LearnerEntity,
        progressList: List<SessionProgressEntity>,
        wrongItems: List<WrongItemEntity>,
        inProgressList: List<InProgressSessionEntity>,
    ) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            var canvas = page.canvas
            var y = 0f

            val headerBgPaint = Paint().apply { color = Color.parseColor("#0D47A1") }
            val goldAccentPaint = Paint().apply { color = Color.parseColor("#FFB300") }
            val ksBannerPaint = Paint().apply { color = Color.parseColor("#1E3A8A") }
            val trackSubheaderPaint = Paint().apply { color = Color.parseColor("#E0E7FF") }
            val cardBgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
            val altRowBgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 1f
            }

            val titlePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val subtitlePaint = TextPaint().apply {
                color = Color.parseColor("#DBEAFE")
                textSize = 11f
                isAntiAlias = true
            }
            val sectionHeaderPaint = TextPaint().apply {
                color = Color.parseColor("#0D47A1")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val ksBannerTextPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val ksBannerRightPaint = TextPaint().apply {
                color = Color.parseColor("#FDE68A")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            val trackSubheaderTextPaint = TextPaint().apply {
                color = Color.parseColor("#1E3A8A")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val trackSubheaderRightPaint = TextPaint().apply {
                color = Color.parseColor("#1E40AF")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            val bodyBoldPaint = TextPaint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = TextPaint().apply {
                color = Color.parseColor("#334155")
                textSize = 9.5f
                isAntiAlias = true
            }
            val rowRightPaint = TextPaint().apply {
                color = Color.parseColor("#334155")
                textSize = 9f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }
            val smallMutedPaint = TextPaint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                isAntiAlias = true
            }
            val footerPagePaint = TextPaint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                textAlign = Paint.Align.RIGHT
                isAntiAlias = true
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                c.drawLine(MARGIN, PAGE_HEIGHT - 36f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 36f, dividerPaint)
                c.drawText(
                    "Developed for Bagacay ES Learners • This is an innovation of FRANZ D. PADOJINOG",
                    MARGIN,
                    PAGE_HEIGHT - 20f,
                    smallMutedPaint,
                )
                c.drawText(
                    "Page $pNum",
                    PAGE_WIDTH - MARGIN,
                    PAGE_HEIGHT - 20f,
                    footerPagePaint,
                )
            }

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > PAGE_HEIGHT - 46f) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN
                }
            }

            val progressByModuleId = progressList.associateBy { it.moduleId }
            val inProgressByModuleId = inProgressList.associateBy { it.moduleId }

            // Top School Header with Bagacay ES Logo
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 84f, headerBgPaint)
            canvas.drawRect(0f, 84f, PAGE_WIDTH.toFloat(), 90f, goldAccentPaint)
            runCatching {
                val logoBmp = BitmapFactory.decodeResource(context.resources, R.drawable.bagacay_logo)
                if (logoBmp != null) {
                    val scaled = Bitmap.createScaledBitmap(logoBmp, 58, 58, true)
                    val whiteCircle = Paint().apply {
                        color = Color.WHITE
                        isAntiAlias = true
                    }
                    val cx = PAGE_WIDTH - MARGIN - 29f
                    val cy = 42f
                    canvas.drawCircle(cx, cy, 31f, whiteCircle)
                    canvas.drawBitmap(scaled, cx - 29f, cy - 29f, null)
                }
            }
            canvas.drawText("BAGACAY ELEMENTARY SCHOOL", MARGIN, 34f, titlePaint)
            canvas.drawText(
                "Reading Progress & Mastery Report (Key Stages 1 & 2)",
                MARGIN,
                54f,
                subtitlePaint,
            )
            val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date())
            canvas.drawText("Generated: $dateStr", MARGIN, 70f, subtitlePaint)

            y = 104f

            // Learner Info Card
            val infoRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 68f)
            canvas.drawRoundRect(infoRect, 10f, 10f, cardBgPaint)

            val track = ReadingTrack.fromId(learner.preferredTrackId)
            val completedCount = progressList.count { it.attemptsCount > 0 }
            val ks1Modules = CurriculumRepository.getModulesForTrack(ReadingTrack.KS1_ENGLISH) +
                CurriculumRepository.getModulesForTrack(ReadingTrack.KS1_FILIPINO)
            val ks2Modules = CurriculumRepository.getModulesForTrack(ReadingTrack.KS2_BASIC) +
                CurriculumRepository.getModulesForTrack(ReadingTrack.KS2_PLUS)
            val ks1Done = ks1Modules.count { (progressByModuleId[it.id]?.attemptsCount ?: 0) > 0 }
            val ks2Done = ks2Modules.count { (progressByModuleId[it.id]?.attemptsCount ?: 0) > 0 }

            val avgPct = if (progressList.isEmpty()) {
                0
            } else {
                progressList.map {
                    if (it.totalQuestions > 0) (it.bestScore * 100) / it.totalQuestions else 0
                }.average().toInt()
            }

            canvas.drawText("Learner: ${learner.fullName}", MARGIN + 12f, y + 20f, bodyBoldPaint)
            canvas.drawText(
                "Grade ${learner.gradeLevel} • Section: ${learner.section} • Selected Activity: ${track.title}",
                MARGIN + 12f,
                y + 37f,
                bodyPaint,
            )
            canvas.drawText(
                "KS1 Done: $ks1Done/${ks1Modules.size}  |  KS2 Done: $ks2Done/${ks2Modules.size}  |  Total Completed: $completedCount  |  Avg Best: $avgPct%",
                MARGIN + 12f,
                y + 54f,
                bodyPaint,
            )
            y += 84f

            // 5-Color Mastery Badge Summary
            canvas.drawText("5-Color Mastery Badge Summary", MARGIN, y, sectionHeaderPaint)
            y += 8f

            val badgeCounts = MasteryBadgeTier.entries.associateWith { tier ->
                progressList.count { it.attemptsCount > 0 && it.badgeTier == tier.tierIndex }
            }
            val pillWidth = (CONTENT_WIDTH - 24f) / 5f
            MasteryBadgeTier.entries.reversed().forEachIndexed { idx, tier ->
                val left = MARGIN + idx * (pillWidth + 6f)
                val rect = RectF(left, y, left + pillWidth, y + 42f)
                val bgPaint = Paint().apply { color = tier.pdfHex }
                canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

                val whiteBold = TextPaint().apply {
                    color = Color.WHITE
                    textSize = 13f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val whiteSmall = TextPaint().apply {
                    color = Color.WHITE
                    textSize = 8.5f
                    isAntiAlias = true
                }
                val count = badgeCounts[tier] ?: 0
                canvas.drawText("$count", left + 10f, y + 19f, whiteBold)
                canvas.drawText(tier.shortLabel, left + 10f, y + 33f, whiteSmall)
            }
            y += 58f

            // Activities Listed Based on Key Stages (KS1 & KS2 - ARAL Math Format)
            ensureSpace(44f)
            canvas.drawText("Activities Progress by Key Stage (KS1 & KS2)", MARGIN, y, sectionHeaderPaint)
            y += 10f

            for (ksSection in keyStageSections) {
                val ksAllModules = ksSection.tracks.flatMap { CurriculumRepository.getModulesForTrack(it) }
                val ksCompleted = ksAllModules.count { (progressByModuleId[it.id]?.attemptsCount ?: 0) > 0 }
                val ksInProgress = ksAllModules.count {
                    (progressByModuleId[it.id]?.attemptsCount ?: 0) == 0 && inProgressByModuleId.containsKey(it.id)
                }

                ensureSpace(64f)
                val bannerRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f)
                canvas.drawRoundRect(bannerRect, 6f, 6f, ksBannerPaint)
                canvas.drawText(ksSection.headerLabel, MARGIN + 10f, y + 16f, ksBannerTextPaint)
                val ksSummaryStr = if (ksInProgress > 0) {
                    "$ksCompleted/${ksAllModules.size} Completed • $ksInProgress In Progress"
                } else {
                    "$ksCompleted/${ksAllModules.size} Completed"
                }
                canvas.drawText(ksSummaryStr, PAGE_WIDTH - MARGIN - 10f, y + 16f, ksBannerRightPaint)
                y += 28f

                for (ksTrack in ksSection.tracks) {
                    val trackModules = CurriculumRepository.getModulesForTrack(ksTrack)
                    val trackDone = trackModules.count { (progressByModuleId[it.id]?.attemptsCount ?: 0) > 0 }

                    ensureSpace(46f)
                    val subRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f)
                    canvas.drawRoundRect(subRect, 5f, 5f, trackSubheaderPaint)
                    canvas.drawText("${ksTrack.title} (${ksTrack.gradeRangeLabel})", MARGIN + 10f, y + 13.5f, trackSubheaderTextPaint)
                    canvas.drawText("$trackDone / ${trackModules.size} done", PAGE_WIDTH - MARGIN - 10f, y + 13.5f, trackSubheaderRightPaint)
                    y += 23f

                    trackModules.forEachIndexed { idx, module ->
                        ensureSpace(24f)
                        val progress = progressByModuleId[module.id]
                        val checkpoint = inProgressByModuleId[module.id]
                        val nextItemNum = computeNextItemNumber(module, checkpoint)

                        val rowBg = if (idx % 2 == 0) cardBgPaint else altRowBgPaint
                        val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f)
                        canvas.drawRoundRect(rowRect, 4f, 4f, rowBg)

                        val dotColor: Int
                        val statusText: String
                        val statusColor: Int
                        when {
                            progress != null && progress.attemptsCount > 0 -> {
                                val tier = MasteryBadgeTier.fromTierIndex(progress.badgeTier)
                                val pct = if (progress.totalQuestions > 0) {
                                    (progress.bestScore * 100) / progress.totalQuestions
                                } else {
                                    0
                                }
                                dotColor = tier.pdfHex
                                statusColor = tier.pdfHex
                                statusText = if (nextItemNum != null) {
                                    "Resume Item $nextItemNum/${module.questions.size} • Best: ${progress.bestScore}/${progress.totalQuestions} ($pct%) • ${tier.shortLabel}"
                                } else {
                                    "${progress.bestScore}/${progress.totalQuestions} ($pct%) • ${tier.shortLabel} • Tries: ${progress.attemptsCount}"
                                }
                            }
                            nextItemNum != null -> {
                                dotColor = Color.parseColor("#0284C7")
                                statusColor = Color.parseColor("#0369A1")
                                statusText = "In Progress • Item $nextItemNum/${module.questions.size}"
                            }
                            else -> {
                                dotColor = MasteryBadgeTier.SUPPORT_GRAY.pdfHex
                                statusColor = Color.parseColor("#64748B")
                                statusText = "Not Started (${module.questions.size} items)"
                            }
                        }

                        val dotPaint = Paint().apply {
                            color = dotColor
                            isAntiAlias = true
                        }
                        canvas.drawCircle(MARGIN + 10f, y + 10f, 4f, dotPaint)

                        val clippedTitle = clipText(module.title, 36)
                        canvas.drawText(clippedTitle, MARGIN + 20f, y + 13.5f, bodyBoldPaint)

                        rowRightPaint.color = statusColor
                        canvas.drawText(statusText, PAGE_WIDTH - MARGIN - 8f, y + 13.5f, rowRightPaint)
                        y += 22f
                    }
                    y += 4f
                }
                y += 4f
            }

            y += 6f
            ensureSpace(64f)
            canvas.drawText(
                "Repeated-Wrong Items & Remediation Targets (${wrongItems.size})",
                MARGIN,
                y,
                sectionHeaderPaint,
            )
            y += 14f

            if (wrongItems.isEmpty()) {
                canvas.drawText(
                    "No repeated-wrong items recorded! Learner answered items within 2 tries.",
                    MARGIN,
                    y + 6f,
                    bodyPaint,
                )
                y += 24f
            } else {
                for (item in wrongItems) {
                    ensureSpace(58f)
                    val itemRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 50f)
                    val warnPaint = Paint().apply { color = Color.parseColor("#FEF2F2") }
                    canvas.drawRoundRect(itemRect, 8f, 8f, warnPaint)

                    val repeatPaint = TextPaint().apply {
                        color = Color.parseColor("#B91C1C")
                        textSize = 9.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val ksTag = ReadingTrack.fromId(item.trackId).keyStage
                    canvas.drawText(
                        "[$ksTag] ${clipText(item.moduleTitle, 34)}  •  Repeated Wrong: ${item.repeatedWrongCount}x (Misses: ${item.wrongCount})",
                        MARGIN + 10f,
                        y + 16f,
                        repeatPaint,
                    )
                    canvas.drawText(
                        "Item: ${clipText(item.promptSummary, 68)}",
                        MARGIN + 10f,
                        y + 30f,
                        bodyPaint,
                    )
                    canvas.drawText(
                        "Expected: ${clipText(item.expectedAnswer, 26)}   |   Last Answer: ${clipText(item.lastWrongAnswer, 26)}",
                        MARGIN + 10f,
                        y + 43f,
                        smallMutedPaint,
                    )
                    y += 56f
                }
            }

            drawFooter(canvas, pageNumber)
            document.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
        } finally {
            runCatching { document.close() }
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

    private fun renderFallbackPdf(
        pdfFile: File,
        learner: LearnerEntity,
        progressList: List<SessionProgressEntity>,
        wrongItems: List<WrongItemEntity>,
        inProgressList: List<InProgressSessionEntity>,
    ) {
        val progressByModuleId = progressList.associateBy { it.moduleId }
        val inProgressByModuleId = inProgressList.associateBy { it.moduleId }
        val lines = buildList {
            add("BAGACAY ELEMENTARY SCHOOL - READING REPORT (KEY STAGES)")
            add("Learner: ${learner.fullName} | Grade ${learner.gradeLevel} - ${learner.section}")
            keyStageSections.forEach { ks ->
                add("--- ${ks.keyStageCode} ---")
                ks.tracks.forEach { tr ->
                    val mods = CurriculumRepository.getModulesForTrack(tr)
                    val done = mods.count { (progressByModuleId[it.id]?.attemptsCount ?: 0) > 0 }
                    val inProg = mods.count { inProgressByModuleId.containsKey(it.id) }
                    add("${tr.title}: $done/${mods.size} completed, $inProg in progress")
                }
            }
            add("Repeated-Wrong Items: ${wrongItems.size}")
            wrongItems.take(8).forEach {
                add("Missed (${it.repeatedWrongCount}x): ${it.expectedAnswer} [Last: ${it.lastWrongAnswer}]")
            }
            add("Developed for Bagacay ES Learners | This is an innovation of FRANZ D. PADOJINOG")
        }
        val escapedStream = buildString {
            append("BT\n/F1 10 Tf\n40 780 Td\n15 TL\n")
            lines.forEach { line ->
                val clean = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                append("($clean) Tj T*\n")
            }
            append("ET")
        }
        val objs = listOf(
            "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
            "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n",
            "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n",
            "4 0 obj << /Length ${escapedStream.length} >> stream\n$escapedStream\nendstream\nendobj\n",
            "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n",
        )
        val sb = StringBuilder("%PDF-1.4\n")
        val offsets = mutableListOf(0)
        objs.forEach { obj ->
            offsets.add(sb.length)
            sb.append(obj)
        }
        val xrefStart = sb.length
        sb.append("xref\n0 ${objs.size + 1}\n0000000000 65535 f \n")
        offsets.drop(1).forEach { off ->
            sb.append(String.format(Locale.US, "%010d 00000 n \n", off))
        }
        sb.append("trailer << /Size ${objs.size + 1} /Root 1 0 R >>\nstartxref\n$xrefStart\n%%EOF\n")
        pdfFile.writeText(sb.toString(), Charsets.ISO_8859_1)
    }

    fun buildSummaryText(
        learner: LearnerEntity,
        progressList: List<SessionProgressEntity>,
        wrongItems: List<WrongItemEntity>,
    ): String {
        val completed = progressList.count { it.attemptsCount > 0 }
        val ks1Done = progressList.count { ReadingTrack.fromId(it.trackId).keyStage == "KS1" && it.attemptsCount > 0 }
        val ks2Done = progressList.count { ReadingTrack.fromId(it.trackId).keyStage == "KS2" && it.attemptsCount > 0 }
        val mastered = progressList.count { it.badgeTier == MasteryBadgeTier.MASTERED_GREEN.tierIndex }
        val topWeak = wrongItems.take(3).joinToString("; ") {
            "${it.expectedAnswer} (${it.repeatedWrongCount}x)"
        }
        return buildString {
            append("Bagacay ES Reading Report\n")
            append("Learner: ${learner.fullName} (Grade ${learner.gradeLevel} - ${learner.section})\n")
            append("KS1 Done: $ks1Done/16 | KS2 Done: $ks2Done/16 | Total: $completed | Mastered: $mastered\n")
            if (wrongItems.isNotEmpty()) {
                append("Repeated-Wrong Items (${wrongItems.size}): $topWeak")
            } else {
                append("Repeated-Wrong Items: None")
            }
        }
    }

    private fun clipText(text: String, maxChars: Int): String {
        return if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"
    }
}

object MessengerReportSharer {

    private const val MESSENGER_PACKAGE = "com.facebook.orca"
    private const val MESSENGER_LITE_PACKAGE = "com.facebook.mlite"

    fun sendPdfToMessenger(
        context: Context,
        result: PdfGenerationResult,
    ): String {
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, result.contentUri)
            putExtra(Intent.EXTRA_TEXT, result.summaryText)
            putExtra(Intent.EXTRA_SUBJECT, "Reading Progress Report - Bagacay ES")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Try Facebook Messenger directly first
        val messengerIntent = Intent(baseIntent).apply { setPackage(MESSENGER_PACKAGE) }
        try {
            context.startActivity(messengerIntent)
            return "Opened Messenger with PDF Progress Report."
        } catch (_: ActivityNotFoundException) {
            // Try Messenger Lite before general chooser
        }

        val messengerLiteIntent = Intent(baseIntent).apply { setPackage(MESSENGER_LITE_PACKAGE) }
        try {
            context.startActivity(messengerLiteIntent)
            return "Opened Messenger Lite with PDF Progress Report."
        } catch (_: ActivityNotFoundException) {
            // Fallback to Android share chooser so report can always be sent
        }

        val chooser = Intent.createChooser(baseIntent, "Send Reading PDF Report via Messenger or Share")
        context.startActivity(chooser)
        return "Messenger not detected — opened Share Sheet with PDF Report."
    }

    fun openPdfPreview(context: Context, result: PdfGenerationResult): Boolean {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(result.contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(viewIntent, "Open PDF Progress Report"))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
