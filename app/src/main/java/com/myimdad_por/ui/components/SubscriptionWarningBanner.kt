package com.myimdad_por.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTypography
import com.myimdad_por.ui.theme.WarningColor
import com.myimdad_por.ui.theme.WarningContainer
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor

@Composable
fun SubscriptionWarningBanner(
    modifier: Modifier = Modifier,
    title: String = "تنبيه الاشتراك",
    message: String? = null,
    daysRemaining: Int? = null,
    actionText: String = "تجديد الآن",
    onActionClick: () -> Unit,
    onDismissClick: (() -> Unit)? = null,
    visible: Boolean = true,
    showIcon: Boolean = true,
    backgroundColor: Color = WarningContainer,
    contentColor: Color = TextPrimaryColor,
    subtitleColor: Color = TextSecondaryColor
) {
    if (!visible) return

    val resolvedMessage = when {
        !message.isNullOrBlank() -> message
        daysRemaining != null && daysRemaining > 0 -> "متبقي $daysRemaining يوم قبل انتهاء الاشتراك."
        daysRemaining == 0 -> "ينتهي الاشتراك اليوم."
        else -> "الاشتراك يحتاج إلى مراجعة."
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = title
            },
        shape = AppShapeTokens.card,
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = AppDimens.Elevation.low
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Layout.screenPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Start
            ) {
                if (showIcon) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = WarningColor.copy(alpha = 0.12f),
                                shape = AppShapeTokens.buttonPill
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = WarningColor
                        )
                    }
                    Spacer(modifier = Modifier.width(AppDimens.Spacing.small))
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp)
                ) {
                    Text(
                        text = title,
                        style = AppTypography.titleMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.size(AppDimens.Spacing.extraSmall))

                    Text(
                        text = resolvedMessage,
                        style = AppTypography.bodyMedium,
                        color = subtitleColor,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (daysRemaining != null) {
                        Spacer(modifier = Modifier.size(AppDimens.Spacing.small))
                        SubscriptionCountdownPill(daysRemaining = daysRemaining)
                    }
                }
            }

            if (onActionClick != null || onDismissClick != null) {
                Spacer(modifier = Modifier.size(AppDimens.Spacing.medium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDismissClick != null) {
                        AppButton(
                            text = "لاحقًا",
                            onClick = onDismissClick,
                            variant = AppButtonVariant.Outlined,
                            size = AppButtonSize.Small,
                            fullWidth = false
                        )
                        Spacer(modifier = Modifier.width(AppDimens.Spacing.small))
                    }

                    AppButton(
                        text = actionText,
                        onClick = onActionClick,
                        variant = AppButtonVariant.Primary,
                        size = AppButtonSize.Small,
                        fullWidth = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCountdownPill(
    daysRemaining: Int
) {
    val text = when {
        daysRemaining > 1 -> "متبقي $daysRemaining أيام"
        daysRemaining == 1 -> "متبقٍ يوم واحد"
        else -> "ينتهي اليوم"
    }

    Surface(
        shape = AppShapeTokens.chip,
        color = WarningColor.copy(alpha = 0.16f),
        contentColor = WarningColor
    ) {
        Text(
            text = text,
            style = AppTypography.labelMedium,
            modifier = Modifier
                .padding(horizontal = AppDimens.Spacing.small, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SubscriptionExpiredBanner(
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit,
    onDismissClick: (() -> Unit)? = null,
    visible: Boolean = true
) {
    SubscriptionWarningBanner(
        modifier = modifier,
        title = "انتهت مدة الاشتراك",
        message = "يرجى تجديد الاشتراك لمتابعة استخدام جميع ميزات التطبيق.",
        actionText = "تجديد الاشتراك",
        onActionClick = onActionClick,
        onDismissClick = onDismissClick,
        visible = visible,
        showIcon = true
    )
}

@Composable
fun SubscriptionDueSoonBanner(
    modifier: Modifier = Modifier,
    daysRemaining: Int,
    onActionClick: () -> Unit,
    onDismissClick: (() -> Unit)? = null,
    visible: Boolean = true
) {
    SubscriptionWarningBanner(
        modifier = modifier,
        title = "الاشتراك يقترب من الانتهاء",
        daysRemaining = daysRemaining,
        actionText = "تجديد الآن",
        onActionClick = onActionClick,
        onDismissClick = onDismissClick,
        visible = visible,
        showIcon = true
    )
}