package com.myimdad_por.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.InfoColor
import com.myimdad_por.ui.theme.BrandPrimary

enum class AppButtonVariant {
    Primary,
    Secondary,
    Elevated,
    Outlined,
    Text,
    Danger,
    Info
}

enum class AppButtonSize(
    val minHeight: Dp,
    val horizontalPadding: Dp,
    val iconSize: Dp,
    val loadingSize: Dp
) {
    Small(
        minHeight = 36.dp,
        horizontalPadding = 14.dp,
        iconSize = 16.dp,
        loadingSize = 16.dp
    ),
    Medium(
        minHeight = 48.dp,
        horizontalPadding = 18.dp,
        iconSize = 18.dp,
        loadingSize = 18.dp
    ),
    Large(
        minHeight = 56.dp,
        horizontalPadding = 20.dp,
        iconSize = 20.dp,
        loadingSize = 20.dp
    )
}

object AppButtonDefaultsEx {
    val shape = AppShapeTokens.button
    val pillShape = RoundedCornerShape(AppDimens.Radius.round)
    val iconSpacing = AppDimens.Spacing.extraSmall
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    contentDescription: String? = null
) {
    val isEnabled = enabled && !loading
    val buttonModifier = modifier
        .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
        .heightIn(min = size.minHeight)
        .semantics(mergeDescendants = true) {
            role = Role.Button
            if (!contentDescription.isNullOrBlank()) {
                this.contentDescription = contentDescription
            }
            if (loading) {
                stateDescription = "جاري التحميل"
                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo.Indeterminate
            }
        }

    val contentPadding = PaddingValues(
        horizontal = size.horizontalPadding,
        vertical = 0.dp
    )

    when (variant) {
        AppButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = AppButtonDefaultsEx.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = BrandPrimary.copy(alpha = 0.45f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = contentPadding
            ) {
                AppButtonContent(
                    text = text,
                    variant = variant,
                    size = size,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }

        AppButtonVariant.Secondary -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = AppButtonDefaultsEx.shape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = contentPadding
            ) {
                AppButtonContent(
                    text = text,
                    variant = variant,
                    size = size,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }

        AppButtonVariant.Elevated -> {
            ElevatedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = AppButtonDefaultsEx.shape,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = contentPadding
            ) {
                AppButtonContent(
                    text = text,
                    variant = variant,
                    size = size,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }

        AppButtonVariant.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = AppButtonDefaultsEx.shape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = contentPadding
            ) {
                AppButtonContent(
                    text = text,
                    variant = variant,
                    size = size,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }

        AppButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = AppButtonDefaultsEx.shape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = contentPadding
            ) {
                AppButtonContent(
                    text = text,
                    variant = variant,
                    size = size,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }

        AppButtonVariant.Danger -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = AppButtonDefaultsEx.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorColor,
                    contentColor = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = ErrorColor.copy(alpha = 0.45f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = contentPadding
            ) {
                AppButtonContent(
                    text = text,
                    variant = variant,
                    size = size,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }

        AppButtonVariant.Info -> {
            Button(
                onClick = onClick,
                modifier = buttonModifier,
                enabled = isEnabled,
                shape = AppButtonDefaultsEx.shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InfoColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = InfoColor.copy(alpha = 0.45f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                ),
                contentPadding = contentPadding
            ) {
                AppButtonContent(
                    text = text,
                    variant = variant,
                    size = size,
                    loading = loading,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon
                )
            }
        }
    }
}

@Composable
private fun AppButtonContent(
    text: String,
    variant: AppButtonVariant,
    size: AppButtonSize,
    loading: Boolean,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size.loadingSize),
                color = currentContentColor(variant),
                strokeWidth = 2.dp
            )
            if (text.isNotBlank()) {
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(AppButtonDefaultsEx.iconSpacing))
                AppButtonLabel(text = text)
            }
            return
        }

        if (leadingIcon != null) {
            Box(modifier = Modifier.size(size.iconSize), contentAlignment = Alignment.Center) {
                leadingIcon()
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(AppButtonDefaultsEx.iconSpacing))
        }

        AppButtonLabel(text = text)

        if (trailingIcon != null) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(AppButtonDefaultsEx.iconSpacing))
            Box(modifier = Modifier.size(size.iconSize), contentAlignment = Alignment.Center) {
                trailingIcon()
            }
        }
    }
}

@Composable
private fun AppButtonLabel(text: String) {
    Text(
        text = text,
        style = AppTypography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun currentContentColor(variant: AppButtonVariant): Color {
    val scheme = MaterialTheme.colorScheme
    return when (variant) {
        AppButtonVariant.Primary -> scheme.onPrimary
        AppButtonVariant.Secondary -> scheme.onSecondaryContainer
        AppButtonVariant.Elevated -> scheme.onSurface
        AppButtonVariant.Outlined -> scheme.onSurface
        AppButtonVariant.Text -> scheme.primary
        AppButtonVariant.Danger -> scheme.onError
        AppButtonVariant.Info -> scheme.onPrimary
    }
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.semantics {
            role = Role.Button
            if (!contentDescription.isNullOrBlank()) {
                this.contentDescription = contentDescription
            }
        },
        onClick = onClick,
        enabled = enabled,
        shape = AppButtonDefaultsEx.pillShape,
        color = Color.Transparent,
        contentColor = tint
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@Composable
fun AppButtonPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AppButtonVariant = AppButtonVariant.Primary,
    size: AppButtonSize = AppButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier,
        shape = AppButtonDefaultsEx.pillShape,
        color = Color.Transparent
    ) {
        AppButton(
            text = text,
            onClick = onClick,
            variant = variant,
            size = size,
            enabled = enabled,
            loading = loading,
            fullWidth = fullWidth,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            contentDescription = contentDescription
        )
    }
}