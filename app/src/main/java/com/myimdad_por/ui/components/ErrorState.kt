package com.myimdad_por.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.ErrorContainer
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor

enum class ErrorStateStyle {
    FullScreen,
    Card,
    Inline
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "حدث خطأ",
    details: String? = null,
    retryText: String = "إعادة المحاولة",
    dismissText: String? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    style: ErrorStateStyle = ErrorStateStyle.FullScreen,
    contentDescription: String? = null
) {
    val safeMessage = remember(message) {
        message.trim().ifBlank { "تعذر إكمال العملية" }
    }

    when (style) {
        ErrorStateStyle.FullScreen -> FullScreenErrorState(
            title = title,
            message = safeMessage,
            details = details,
            retryText = retryText,
            dismissText = dismissText,
            onRetry = onRetry,
            onDismiss = onDismiss,
            modifier = modifier,
            contentDescription = contentDescription
        )

        ErrorStateStyle.Card -> CardErrorState(
            title = title,
            message = safeMessage,
            details = details,
            retryText = retryText,
            dismissText = dismissText,
            onRetry = onRetry,
            onDismiss = onDismiss,
            modifier = modifier,
            contentDescription = contentDescription
        )

        ErrorStateStyle.Inline -> InlineErrorState(
            title = title,
            message = safeMessage,
            details = details,
            retryText = retryText,
            dismissText = dismissText,
            onRetry = onRetry,
            onDismiss = onDismiss,
            modifier = modifier,
            contentDescription = contentDescription
        )
    }
}

@Composable
fun FullScreenErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    details: String? = null,
    retryText: String = "إعادة المحاولة",
    dismissText: String? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
            .semantics {
                role = Role.Image
                if (!contentDescription.isNullOrBlank()) {
                    this.contentDescription = contentDescription
                }
            },
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = AppDimens.Layout.contentMaxWidth),
                shape = RoundedCornerShape(AppDimens.Radius.large),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.medium)
            ) {
                ErrorStateContent(
                    title = title,
                    message = message,
                    details = details,
                    retryText = retryText,
                    dismissText = dismissText,
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
fun CardErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    details: String? = null,
    retryText: String = "إعادة المحاولة",
    dismissText: String? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Image
                if (!contentDescription.isNullOrBlank()) {
                    this.contentDescription = contentDescription
                }
            },
        shape = RoundedCornerShape(AppDimens.Radius.large),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.low)
    ) {
        ErrorStateContent(
            title = title,
            message = message,
            details = details,
            retryText = retryText,
            dismissText = dismissText,
            onRetry = onRetry,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun InlineErrorState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    details: String? = null,
    retryText: String = "إعادة المحاولة",
    dismissText: String? = null,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Image
                if (!contentDescription.isNullOrBlank()) {
                    this.contentDescription = contentDescription
                }
            },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = ErrorContainer,
                    shape = RoundedCornerShape(AppDimens.Radius.large)
                )
                .padding(AppDimens.Layout.screenPadding),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            ErrorBadge()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                Text(
                    text = title,
                    style = AppTypography.titleMedium,
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = message,
                    style = AppTypography.bodyMedium,
                    color = TextSecondaryColor
                )
                if (!details.isNullOrBlank()) {
                    Text(
                        text = details,
                        style = AppTypography.bodySmall,
                        color = TextSecondaryColor
                    )
                }
                ErrorActionsRow(
                    retryText = retryText,
                    dismissText = dismissText,
                    onRetry = onRetry,
                    onDismiss = onDismiss
                )
            }
        }
    }
}

@Composable
private fun ErrorStateContent(
    title: String,
    message: String,
    details: String?,
    retryText: String,
    dismissText: String?,
    onRetry: (() -> Unit)?,
    onDismiss: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorContainer)
            .padding(AppDimens.Layout.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            ErrorBadge()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
            ) {
                Text(
                    text = title,
                    style = AppTypography.titleLarge,
                    color = TextPrimaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = message,
                    style = AppTypography.bodyLarge,
                    color = TextSecondaryColor
                )
            }
        }

        if (!details.isNullOrBlank()) {
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Text(
                text = details,
                style = AppTypography.bodyMedium,
                color = TextSecondaryColor
            )
        }

        ErrorActionsRow(
            retryText = retryText,
            dismissText = dismissText,
            onRetry = onRetry,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ErrorActionsRow(
    retryText: String,
    dismissText: String?,
    onRetry: (() -> Unit)?,
    onDismiss: (() -> Unit)?
) {
    if (onRetry == null && onDismiss == null) return

    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onRetry != null) {
            AppButton(
                text = retryText,
                onClick = onRetry,
                variant = AppButtonVariant.Primary,
                fullWidth = false
            )
        }

        if (onDismiss != null) {
            AppButton(
                text = dismissText ?: "إغلاق",
                onClick = onDismiss,
                variant = AppButtonVariant.Outlined,
                fullWidth = false
            )
        }
    }
}

@Composable
private fun ErrorBadge() {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(
                color = Color.White.copy(alpha = 0.8f),
                shape = RoundedCornerShape(AppDimens.Radius.round)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "!",
            style = AppTypography.titleLarge,
            color = ErrorColor,
            textAlign = TextAlign.Center
        )
    }
}