package com.myimdad_por.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.myimdad_por.R

/**
 * Font families used across the app.
 * ArabicFontFamily: for general Arabic text.
 * NumbersFontFamily: for pure numeric content like amounts, dates, and codes.
 */
private val ArabicFontFamily = FontFamily(
    Font(R.font.arabic, weight = FontWeight.Normal),
    Font(R.font.arabic, weight = FontWeight.Medium),
    Font(R.font.arabic, weight = FontWeight.SemiBold),
    Font(R.font.arabic, weight = FontWeight.Bold)
)

private val NumbersFontFamily = FontFamily(
    Font(R.font.numbers, weight = FontWeight.Normal),
    Font(R.font.numbers, weight = FontWeight.Medium),
    Font(R.font.numbers, weight = FontWeight.SemiBold),
    Font(R.font.numbers, weight = FontWeight.Bold)
)

/**
 * Main Material 3 typography for the app.
 * Arabic font is the default for all text styles.
 */
val AppTypography = Typography(
    displayLarge = arabicStyle(
        fontSize = 57.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = arabicStyle(
        fontSize = 45.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.Normal
    ),
    displaySmall = arabicStyle(
        fontSize = 36.sp,
        lineHeight = 44.sp,
        fontWeight = FontWeight.Normal
    ),

    headlineLarge = arabicStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineMedium = arabicStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold
    ),
    headlineSmall = arabicStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold
    ),

    titleLarge = arabicStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = arabicStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.15.sp
    ),
    titleSmall = arabicStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),

    bodyLarge = arabicStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = arabicStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    ),
    bodySmall = arabicStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    ),

    labelLarge = arabicStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp
    ),
    labelMedium = arabicStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    ),
    labelSmall = arabicStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
    )
)

/**
 * Reusable text styles for direct use in UI.
 */
object AppTextStyles {

    val ArabicDisplay = arabicStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold
    )

    val ArabicTitle = arabicStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold
    )

    val ArabicBody = arabicStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    )

    val ArabicCaption = arabicStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    )

    val NumberDisplay = numberStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold
    )

    val NumberTitle = numberStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold
    )

    val NumberBody = numberStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium
    )

    val NumberCaption = numberStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
}

/**
 * Creates a text style that uses the Arabic font family.
 */
private fun arabicStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight,
    letterSpacing: TextUnit = 0.sp
): TextStyle {
    return TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
}

/**
 * Creates a text style that uses the numeric font family.
 * Useful for money, dates, IDs, invoices, and counters.
 */
private fun numberStyle(
    fontSize: TextUnit,
    lineHeight: TextUnit,
    fontWeight: FontWeight = FontWeight.Medium,
    letterSpacing: TextUnit = 0.sp
): TextStyle {
    return TextStyle(
        fontFamily = NumbersFontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        fontFeatureSettings = "tnum",
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
}

/**
 * Convert any TextStyle to Arabic font family.
 */
fun TextStyle.asArabic(): TextStyle = copy(
    fontFamily = ArabicFontFamily,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * Convert any TextStyle to numeric font family.
 */
fun TextStyle.asNumber(): TextStyle = copy(
    fontFamily = NumbersFontFamily,
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)