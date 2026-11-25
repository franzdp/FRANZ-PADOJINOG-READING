package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.BadgeAmber
import com.example.ui.theme.BadgeAmberBg
import com.example.ui.theme.BadgeBlue
import com.example.ui.theme.BadgeBlueBg
import com.example.ui.theme.BadgeGray
import com.example.ui.theme.BadgeGrayBg
import com.example.ui.theme.BadgeGreen
import com.example.ui.theme.BadgeGreenBg
import com.example.ui.theme.BadgeRed
import com.example.ui.theme.BadgeRedBg

enum class MasteryBadgeTier(
    val tierIndex: Int,
    val title: String,
    val shortLabel: String,
    val rangeHint: String,
    val color: Color,
    val containerColor: Color,
    val pdfHex: Int,
) {
    SUPPORT_GRAY(
        tierIndex = 0,
        title = "Starter / Support",
        shortLabel = "Support",
        rangeHint = "Not started or <50%",
        color = BadgeGray,
        containerColor = BadgeGrayBg,
        pdfHex = 0xFF64748B.toInt(),
    ),
    EMERGING_RED(
        tierIndex = 1,
        title = "Emerging Reader",
        shortLabel = "Emerging",
        rangeHint = "50% – 64%",
        color = BadgeRed,
        containerColor = BadgeRedBg,
        pdfHex = 0xFFDC2626.toInt(),
    ),
    DEVELOPING_AMBER(
        tierIndex = 2,
        title = "Developing Reader",
        shortLabel = "Developing",
        rangeHint = "65% – 79%",
        color = BadgeAmber,
        containerColor = BadgeAmberBg,
        pdfHex = 0xFFD97706.toInt(),
    ),
    PROFICIENT_BLUE(
        tierIndex = 3,
        title = "Proficient Reader",
        shortLabel = "Proficient",
        rangeHint = "80% – 94%",
        color = BadgeBlue,
        containerColor = BadgeBlueBg,
        pdfHex = 0xFF0284C7.toInt(),
    ),
    MASTERED_GREEN(
        tierIndex = 4,
        title = "Mastered",
        shortLabel = "Mastered",
        rangeHint = "95% – 100%",
        color = BadgeGreen,
        containerColor = BadgeGreenBg,
        pdfHex = 0xFF15803D.toInt(),
    );

    companion object {
        fun fromTierIndex(index: Int): MasteryBadgeTier {
            return entries.firstOrNull { it.tierIndex == index } ?: SUPPORT_GRAY
        }

        fun fromScore(correctCount: Int, totalCount: Int, attemptsCount: Int = 1): MasteryBadgeTier {
            if (attemptsCount <= 0 || totalCount <= 0) return SUPPORT_GRAY
            val pct = (correctCount * 100.0) / totalCount
            return when {
                pct >= 95.0 -> MASTERED_GREEN
                pct >= 80.0 -> PROFICIENT_BLUE
                pct >= 65.0 -> DEVELOPING_AMBER
                pct >= 50.0 -> EMERGING_RED
                else -> SUPPORT_GRAY
            }
        }
    }
}
