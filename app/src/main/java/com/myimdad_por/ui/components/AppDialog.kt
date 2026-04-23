package com.myimdad_por.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppTypography

enum class AppDialogVariant {
    Default,
    Info,
    Success,
    Warning,
    Error
}

@Stable
object AppDialogDefaults {
    val contentPadding = AppDimens.Spacing.large
    val actionsSpacing = AppDimens.Spacing.small
}

@Composable
fun AppDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    variant: AppDialogVariant = AppDialogVariant.Default,
    confirmText: String = "حسنًا",
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null,
    loading: Boolean = false,
    extraContent: @Composable (() -> Unit)? = null
) {
    if (!visible) return

    val colorScheme = MaterialTheme.colorScheme
    val accentColor = variant.accentColor(colorScheme)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = variant.label(),
                    style = AppTypography.labelLarge,
                    color = accentColor
                )
                Spacer(modifier = Modifier.padding(top = AppDimens.Spacing.small))
                Text(
                    text = title,
                    style = AppTypography.titleLarge,
                    color = colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    style = AppTypography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )

                if (extraContent != null) {
                    Spacer(modifier = Modifier.padding(top = AppDialogDefaults.contentPadding))
                    extraContent()
                }
            }
        },
        confirmButton = {
            AppButton(
                text = confirmText,
                onClick = { onConfirm?.invoke() ?: onDismissRequest() },
                variant = variant.toButtonVariant(),
                loading = loading
            )
        },
        dismissButton = {
            if (!dismissText.isNullOrBlank()) {
                TextButton(
                    onClick = { onDismissClick?.invoke() ?: onDismissRequest() },
                    enabled = !loading
                ) {
                    Text(
                        text = dismissText,
                        style = AppTypography.labelLarge
                    )
                }
            }
        }
    )
}

@Composable
fun AppConfirmDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    confirmText: String = "تأكيد",
    dismissText: String = "إلغاء",
    onConfirm: () -> Unit,
    variant: AppDialogVariant = AppDialogVariant.Default,
    loading: Boolean = false
) {
    AppDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = title,
        message = message,
        modifier = modifier,
        variant = variant,
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText,
        onDismissClick = onDismissRequest,
        loading = loading
    )
}

@Composable
fun AppLoadingDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit = {},
    message: String = "جاري التحميل..."
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {},
        title = {
            Text(
                text = "انتظر قليلًا",
                style = AppTypography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
                Text(
                    text = message,
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun AppSimpleDialog(
    visible: Boolean,
    title: String,
    message: String,
    onDismissRequest: () -> Unit
) {
    AppDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = title,
        message = message,
        confirmText = "حسناً",
        onConfirm = onDismissRequest
    )
}

private fun AppDialogVariant.accentColor(colorScheme: ColorScheme): Color {
    return when (this) {
        AppDialogVariant.Default -> colorScheme.primary
        AppDialogVariant.Info -> colorScheme.tertiary
        AppDialogVariant.Success -> colorScheme.secondary
        AppDialogVariant.Warning -> colorScheme.error
        AppDialogVariant.Error -> colorScheme.error
    }
}

private fun AppDialogVariant.label(): String {
    return when (this) {
        AppDialogVariant.Default -> "تنبيه"
        AppDialogVariant.Info -> "معلومة"
        AppDialogVariant.Success -> "نجاح"
        AppDialogVariant.Warning -> "تحذير"
        AppDialogVariant.Error -> "خطأ"
    }
}

private fun AppDialogVariant.toButtonVariant(): AppButtonVariant {
    return when (this) {
        AppDialogVariant.Default -> AppButtonVariant.Primary
        AppDialogVariant.Info -> AppButtonVariant.Info
        AppDialogVariant.Success -> AppButtonVariant.Primary
        AppDialogVariant.Warning -> AppButtonVariant.Outlined
        AppDialogVariant.Error -> AppButtonVariant.Danger
    }
}