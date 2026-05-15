package com.myimdad_por.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * لوحة ألوان وظيفية للتطبيق.
 *
 * الفكرة الأساسية:
 * - BrandPrimary / BrandSecondary: ألوان الهوية
 * - Success / Error / Warning / Info: ألوان دلالية للمعاني
 * - Income / Expense: ألوان محاسبية واضحة
 * - Surface / Background / Text: ألوان الواجهات
 *
 * بهذه الطريقة يمكن تغيير هوية التطبيق لاحقًا دون لمس منطق الألوان الدلالية.
 */

/* -------------------------------------------------------------------------- */
/* Brand colors                                                               */
/* -------------------------------------------------------------------------- */

val BrandPrimary = Color(0xFF25D366)
val BrandPrimaryDark = Color(0xFF128C7E)
val BrandSecondary = Color(0xFF075E54)
val BrandPrimaryTint = Color(0xFFE7F8EE)
val BrandPrimarySoft = Color(0xFFF0FFF5)

/* -------------------------------------------------------------------------- */
/* Semantic colors                                                            */
/* -------------------------------------------------------------------------- */

val SuccessColor = Color(0xFF1E9E5A)
val SuccessContainer = Color(0xFFE8F7EE)

val ErrorColor = Color(0xFFE53935)
val ErrorContainer = Color(0xFFFDECEC)

val WarningColor = Color(0xFFFFB300)
val WarningContainer = Color(0xFFFFF6DB)

val InfoColor = Color(0xFF1A73E8)
val InfoContainer = Color(0xFFE8F1FE)

/* -------------------------------------------------------------------------- */
/* Accounting colors                                                           */
/* -------------------------------------------------------------------------- */

val IncomeColor = Color(0xFF1E9E5A)
val IncomeContainer = Color(0xFFE8F7EE)

val ExpenseColor = Color(0xFFD93025)
val ExpenseContainer = Color(0xFFFDECEC)

val CreditColor = Color(0xFF0F9D58)
val DebitColor = Color(0xFFDB4437)

/* -------------------------------------------------------------------------- */
/* Background / surface                                                       */
/* -------------------------------------------------------------------------- */

val BackgroundColor = Color(0xFFF7F8FA)
val SurfaceColor = Color(0xFFFFFFFF)
val SurfaceVariantColor = Color(0xFFF1F3F5)
val DividerColor = Color(0xFFE9EDEF)
val BorderColor = Color(0xFFDDE3E8)

/* -------------------------------------------------------------------------- */
/* Text colors                                                                 */
/* -------------------------------------------------------------------------- */

val TextPrimaryColor = Color(0xFF111B21)
val TextSecondaryColor = Color(0xFF667781)
val TextTertiaryColor = Color(0xFF8696A0)
val TextDisabledColor = Color(0xFFADB8C1)

/* -------------------------------------------------------------------------- */
/* Icons / controls                                                            */
/* -------------------------------------------------------------------------- */

val IconColor = Color(0xFF54656F)
val IconMutedColor = Color(0xFF8696A0)
val IconOnBrandColor = Color(0xFFFFFFFF)

/* -------------------------------------------------------------------------- */
/* Chat / message bubbles                                                      */
/* -------------------------------------------------------------------------- */

val IncomingBubbleColor = Color(0xFFFFFFFF)
val OutgoingBubbleColor = Color(0xFFD9FDD3)
val IncomingBubbleTextColor = Color(0xFF111B21)
val OutgoingBubbleTextColor = Color(0xFF111B21)

/* -------------------------------------------------------------------------- */
/* Navigation / action elements                                                */
/* -------------------------------------------------------------------------- */

val NavigationSelectedColor = Color(0xFF25D366)
val NavigationUnselectedColor = Color(0xFF667781)
val FloatingActionButtonColor = Color(0xFF25D366)
val FloatingActionButtonContentColor = Color(0xFFFFFFFF)
val BadgeColor = Color(0xFF25D366)
val BadgeContentColor = Color(0xFFFFFFFF)

/* -------------------------------------------------------------------------- */
/* Dark theme colors                                                           */
/* -------------------------------------------------------------------------- */

val DarkBackgroundColor = Color(0xFF0B141A)
val DarkSurfaceColor = Color(0xFF111B21)
val DarkSurfaceVariantColor = Color(0xFF1F2C33)
val DarkDividerColor = Color(0xFF2A3942)
val DarkBorderColor = Color(0xFF33424C)

val DarkTextPrimaryColor = Color(0xFFE9EDEF)
val DarkTextSecondaryColor = Color(0xFFAEBAC1)
val DarkTextTertiaryColor = Color(0xFF8696A0)
val DarkTextDisabledColor = Color(0xFF6E7B83)

val DarkIconColor = Color(0xFFAEBAC1)
val DarkIconMutedColor = Color(0xFF8696A0)

val DarkIncomingBubbleColor = Color(0xFF202C33)
val DarkOutgoingBubbleColor = Color(0xFF005C4B)
val DarkIncomingBubbleTextColor = Color(0xFFE9EDEF)
val DarkOutgoingBubbleTextColor = Color(0xFFE9EDEF)

/* -------------------------------------------------------------------------- */
/* Utility colors                                                              */
/* -------------------------------------------------------------------------- */

val TransparentColor = Color(0x00000000)
val WhiteColor = Color(0xFFFFFFFF)
val BlackColor = Color(0xFF000000)