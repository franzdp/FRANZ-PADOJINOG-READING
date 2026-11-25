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
import com.example.curriculum.ReadingTrack
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

    fun generateProgressReport(
        context: Context,
        learner: LearnerEntity,
        progressList: List<SessionProgressEntity>,
        wrongItems: List<WrongItemEntity>,
    ): PdfGenerationResult {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = learner.fullName.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "learner" }
        val pdfFile = File(reportsDir, "ARAL_Reading_${safeName}_Report.pdf")

        val renderedNative = runCatching {
            renderWithAndroidPdfDocument(context, pdfFile, learner, progressList, wrongItems)
        }.isSuccess

        if (!renderedNative) {
            renderFallbackPdf(pdfFile, learner, progressList, wrongItems)
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
    ) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            var canvas = page.canvas
            var y = 0f

            val headerBgPaint = Paint().apply { color = Color.parseColor("#0D47A1") }
            val goldAccentPaint = Paint().apply { color = Color.parseColor("#FFB300") }
            val cardBgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
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
            val bodyBoldPaint = TextPaint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = TextPaint().apply {
                color = Color.parseColor("#334155")
                textSize = 10f
                isAntiAlias = true
            }
            val smallMutedPaint = TextPaint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                isAntiAlias = true
            }

            fun drawFooter(c: Canvas, pNum: Int) {
                c.drawLine(MARGIN, PAGE_HEIGHT - 36f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 36f, dividerPaint)
                c.drawText(
                    "Bagacay Elementary School • ARAL-Reading Remediation Program",
                    MARGIN,
                    PAGE_HEIGHT - 20f,
                    smallMutedPaint,
                )
                c.drawText(
                    "Page $pNum",
                    PAGE_WIDTH - MARGIN - 36f,
                    PAGE_HEIGHT - 20f,
                    smallMutedPaint,
                )
            }

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > PAGE_HEIGHT - 48f) {
                    drawFooter(canvas, pageNumber)
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN
                }
            }

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
                "ARAL-Reading Progress & Mastery Report (KS1–KS2)",
                MARGIN,
                54f,
                subtitlePaint,
            )
            val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date())
            canvas.drawText("Generated: $dateStr", MARGIN, 70f, subtitlePaint)

            y = 108f

            // Learner Info Card
            val infoRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 72f)
            canvas.drawRoundRect(infoRect, 10f, 10f, cardBgPaint)

            val track = ReadingTrack.fromId(learner.preferredTrackId)
            val completedCount = progressList.count { it.attemptsCount > 0 }
            val avgPct = if (progressList.isEmpty()) {
                0
            } else {
                progressList.map {
                    if (it.totalQuestions > 0) (it.bestScore * 100) / it.totalQuestions else 0
                }.average().toInt()
            }

            canvas.drawText("Learner: ${learner.fullName}", MARGIN + 12f, y + 22f, bodyBoldPaint)
            canvas.drawText(
                "Grade ${learner.gradeLevel} (${learner.keyStage}) • Section: ${learner.section}",
                MARGIN + 12f,
                y + 40f,
                bodyPaint,
            )
            canvas.drawText(
                "Active Track: ${track.title}  |  Completed Sessions: $completedCount  |  Avg Best Score: $avgPct%",
                MARGIN + 12f,
                y + 57f,
                bodyPaint,
            )
            y += 92f

            // 5-Color Mastery Badge Summary
            canvas.drawText("5-Color Mastery Badge Summary", MARGIN, y, sectionHeaderPaint)
            y += 10f

            val badgeCounts = MasteryBadgeTier.entries.associateWith { tier ->
                progressList.count { it.badgeTier == tier.tierIndex }
            }
            val pillWidth = (CONTENT_WIDTH - 24f) / 5f
            MasteryBadgeTier.entries.reversed().forEachIndexed { idx, tier ->
                val left = MARGIN + idx * (pillWidth + 6f)
                val rect = RectF(left, y, left + pillWidth, y + 46f)
                val bgPaint = Paint().apply { color = tier.pdfHex }
                canvas.drawRoundRect(rect, 8f, 8f, bgPaint)

                val whiteBold = TextPaint().apply {
                    color = Color.WHITE
                    textSize = 14f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                val whiteSmall = TextPaint().apply {
                    color = Color.WHITE
                    textSize = 8.5f
                    isAntiAlias = true
                }
                val count = badgeCounts[tier] ?: 0
                canvas.drawText("$count", left + 10f, y + 21f, whiteBold)
                canvas.drawText(tier.shortLabel, left + 10f, y + 36f, whiteSmall)
            }
            y += 66f

            // Session Breakdown
            ensureSpace(50f)
            canvas.drawText("Completed Workbook Sessions & Check-Ins", MARGIN, y, sectionHeaderPaint)
            y += 14f

            if (progressList.isEmpty()) {
                canvas.drawText(
                    "No completed sessions recorded yet for this learner.",
                    MARGIN,
                    y + 6f,
                    bodyPaint,
                )
                y += 28f
            } else {
                val sortedProgress = progressList.sortedWith(
                    compareBy<SessionProgressEntity> { it.trackId }
                        .thenBy { it.weekNumber }
                        .thenBy { it.sessionOrder },
                )
                for (entry in sortedProgress) {
                    ensureSpace(34f)
                    val module = CurriculumRepository.getModuleById(entry.moduleId)
                    val title = module?.title ?: entry.moduleId
                    val trackLabel = ReadingTrack.fromId(entry.trackId).shortTitle
                    val tier = MasteryBadgeTier.fromTierIndex(entry.badgeTier)
                    val pct = if (entry.totalQuestions > 0) {
                        (entry.bestScore * 100) / entry.totalQuestions
                    } else {
                        0
                    }

                    val rowRect = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 26f)
                    canvas.drawRoundRect(rowRect, 6f, 6f, cardBgPaint)

                    val dotPaint = Paint().apply { color = tier.pdfHex }
                    canvas.drawCircle(MARGIN + 12f, y + 13f, 5f, dotPaint)

                    val clippedTitle = clipText(title, 34)
                    canvas.drawText("[$trackLabel] $clippedTitle", MARGIN + 24f, y + 17f, bodyBoldPaint)
                    canvas.drawText(
                        "${entry.bestScore}/${entry.totalQuestions} ($pct%) • ${tier.shortLabel} • Tries: ${entry.attemptsCount}",
                        PAGE_WIDTH - MARGIN - 195f,
                        y + 17f,
                        bodyPaint,
                    )
                    y += 32f
                }
            }

            y += 10f
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
                    canvas.drawText(
                        "${clipText(item.moduleTitle, 38)}  •  Repeated Wrong: ${item.repeatedWrongCount}x (Total Misses: ${item.wrongCount})",
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

    private fun renderFallbackPdf(
        pdfFile: File,
        learner: LearnerEntity,
        progressList: List<SessionProgressEntity>,
        wrongItems: List<WrongItemEntity>,
    ) {
        val lines = buildList {
            add("BAGACAY ELEMENTARY SCHOOL - ARAL-READING REPORT")
            add("Learner: ${learner.fullName} | Grade ${learner.gradeLevel} (${learner.keyStage}) - ${learner.section}")
            add("Completed Sessions: ${progressList.size} | Repeated-Wrong Items: ${wrongItems.size}")
            wrongItems.take(12).forEach {
                add("Missed (${it.repeatedWrongCount}x): ${it.expectedAnswer} [Last: ${it.lastWrongAnswer}]")
            }
        }
        val escapedStream = buildString {
            append("BT\n/F1 11 Tf\n40 780 Td\n16 TL\n")
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
        val mastered = progressList.count { it.badgeTier == MasteryBadgeTier.MASTERED_GREEN.tierIndex }
        val topWeak = wrongItems.take(3).joinToString("; ") {
            "${it.expectedAnswer} (${it.repeatedWrongCount}x)"
        }
        return buildString {
            append("Bagacay ES ARAL-Reading Report\n")
            append("Learner: ${learner.fullName} (Grade ${learner.gradeLevel} - ${learner.section}, ${learner.keyStage})\n")
            append("Sessions Completed: $completed | Mastered Badges: $mastered\n")
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
            putExtra(Intent.EXTRA_SUBJECT, "ARAL Reading Progress Report - Bagacay ES")
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

        val chooser = Intent.createChooser(baseIntent, "Send ARAL-Reading PDF Report via Messenger or Share")
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
