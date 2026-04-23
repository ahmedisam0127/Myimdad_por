package com.myimdad_por.ui.theme
import androidx.compose.ui.graphics.Color
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.myimdad_por.R

private val lightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = IconOnBrandColor,
    primaryContainer = BrandPrimaryTint,
    onPrimaryContainer = TextPrimaryColor,

    secondary = BrandSecondary,
    onSecondary = WhiteColor,
    secondaryContainer = BrandPrimarySoft,
    onSecondaryContainer = TextPrimaryColor,

    tertiary = InfoColor,
    onTertiary = WhiteColor,
    tertiaryContainer = InfoContainer,
    onTertiaryContainer = TextPrimaryColor,

    error = ErrorColor,
    onError = WhiteColor,
    errorContainer = ErrorContainer,
    onErrorContainer = TextPrimaryColor,

    background = BackgroundColor,
    onBackground = TextPrimaryColor,

    surface = SurfaceColor,
    onSurface = TextPrimaryColor,
    surfaceVariant = SurfaceVariantColor,
    onSurfaceVariant = TextSecondaryColor,

    outline = BorderColor,
    outlineVariant = DividerColor,

    inverseOnSurface = WhiteColor,
    inverseSurface = DarkSurfaceColor,
    inversePrimary = BrandPrimary,

    scrim = BlackColor.copy(alpha = 0.32f)
)

private val darkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = IconOnBrandColor,
    primaryContainer = BrandPrimaryDark,
    onPrimaryContainer = WhiteColor,

    secondary = BrandPrimarySoft,
    onSecondary = DarkTextPrimaryColor,
    secondaryContainer = DarkSurfaceVariantColor,
    onSecondaryContainer = DarkTextPrimaryColor,

    tertiary = InfoColor,
    onTertiary = WhiteColor,
    tertiaryContainer = DarkSurfaceVariantColor,
    onTertiaryContainer = DarkTextPrimaryColor,

    error = ErrorColor,
    onError = WhiteColor,
    errorContainer = Color(0xFF5A1A1A),
    onErrorContainer = DarkTextPrimaryColor,

    background = DarkBackgroundColor,
    onBackground = DarkTextPrimaryColor,

    surface = DarkSurfaceColor,
    onSurface = DarkTextPrimaryColor,
    surfaceVariant = DarkSurfaceVariantColor,
    onSurfaceVariant = DarkTextSecondaryColor,

    outline = DarkBorderColor,
    outlineVariant = DarkDividerColor,

    inverseOnSurface = TextPrimaryColor,
    inverseSurface = SurfaceColor,
    inversePrimary = BrandPrimary,

    scrim = BlackColor.copy(alpha = 0.45f)
)

/**
 * الثيم الأساسي للتطبيق.
 *
 * يدعم:
 * - الوضع الفاتح والداكن
 * - الألوان الديناميكية على Android 12+
 * - Typography عربية مخصصة
 * - Shapes موحدة
 */
@Composable
fun ImdadPorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme
        else -> lightColorScheme
    }

    SideEffect {
        context.findActivity()?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme

            window.statusBarColor = colors.surface.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

/**
 * Alias بسيط للاستخدام اليومي داخل التطبيق.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    ImdadPorTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}

/**
 * مفيد للـ Preview أو الاختبارات.
 */
fun provideLightColorScheme(): ColorScheme = lightColorScheme

fun provideDarkColorScheme(): ColorScheme = darkColorScheme

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}