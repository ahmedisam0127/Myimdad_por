package com.myimdad_por.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTypography

enum class LoadingIndicatorVariant {
    Circular,
    Linear,
    Dots
}

enum class LoadingIndicatorSize(
    val indicatorSize: Dp,
    val strokeWidth: Dp
) {
    Small(
        indicatorSize = 20.dp,
        strokeWidth = 2.dp
    ),
    Medium(
        indicatorSize = 32.dp,
        strokeWidth = 3.dp
    ),
    Large(
        indicatorSize = 48.dp,
        strokeWidth = 4.dp
    )
}

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    variant: LoadingIndicatorVariant = LoadingIndicatorVariant.Circular,
    size: LoadingIndicatorSize = LoadingIndicatorSize.Medium,
    title: String? = null,
    message: String? = null,
    progress: Float? = null,
    tint: Color? = null,
    trackColor: Color? = null,
    centered: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(AppDimens.Spacing.large),
) {
    val colorScheme = MaterialTheme.colorScheme
    val resolvedTint = tint ?: colorScheme.primary
    val resolvedTrackColor = trackColor ?: resolvedTint.copy(alpha = 0.16f)
    val accessibilityText = title ?: message ?: "جاري التحميل"

    val semanticsModifier = Modifier.semantics {
        contentDescription = accessibilityText
        liveRegion = LiveRegionMode.Polite
    }

    if (centered) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .then(semanticsModifier),
            contentAlignment = Alignment.Center
        ) {
            LoadingContent(
                variant = variant,
                size = size,
                title = title,
                message = message,
                progress = progress,
                tint = resolvedTint,
                trackColor = resolvedTrackColor,
                contentPadding = contentPadding
            )
        }
    } else {
        LoadingContent(
            modifier = modifier.then(semanticsModifier),
            variant = variant,
            size = size,
            title = title,
            message = message,
            progress = progress,
            tint = resolvedTint,
            trackColor = resolvedTrackColor,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier,
    variant: LoadingIndicatorVariant,
    size: LoadingIndicatorSize,
    title: String?,
    message: String?,
    progress: Float?,
    tint: Color,
    trackColor: Color,
    contentPadding: PaddingValues,
) {
    when (variant) {
        LoadingIndicatorVariant.Circular -> {
            LoadingSurface(
                modifier = modifier,
                contentPadding = contentPadding
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (progress != null) {
                        CircularProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier.size(size.indicatorSize),
                            color = tint,
                            trackColor = trackColor,
                            strokeWidth = size.strokeWidth
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size.indicatorSize),
                            color = tint,
                            trackColor = trackColor,
                            strokeWidth = size.strokeWidth
                        )
                    }

                    if (title != null || message != null) {
                        Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))
                        LoadingTexts(
                            title = title,
                            message = message,
                            centered = true
                        )
                    }
                }
            }
        }

        LoadingIndicatorVariant.Linear -> {
            LoadingSurface(
                modifier = modifier.fillMaxWidth(),
                contentPadding = contentPadding
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (title != null || message != null) {
                        LoadingTexts(
                            title = title,
                            message = message,
                            centered = false
                        )
                        Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))
                    }

                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = progress.coerceIn(0f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = tint,
                            trackColor = trackColor
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = tint,
                            trackColor = trackColor
                        )
                    }
                }
            }
        }

        LoadingIndicatorVariant.Dots -> {
            LoadingSurface(
                modifier = modifier,
                contentPadding = contentPadding
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DotsLoadingIndicator(tint = tint)

                    if (title != null || message != null) {
                        Spacer(modifier = Modifier.height(AppDimens.Spacing.medium))
                        LoadingTexts(
                            title = title,
                            message = message,
                            centered = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingSurface(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.wrapContentHeight(),
        shape = AppShapeTokens.dialog,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun LoadingTexts(
    title: String?,
    message: String?,
    centered: Boolean
) {
    val horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start

    Column(horizontalAlignment = horizontalAlignment) {
        if (title != null) {
            Text(
                text = title,
                style = AppTypography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (message != null) {
            if (title != null) {
                Spacer(modifier = Modifier.height(AppDimens.Spacing.extraSmall))
            }
            Text(
                text = message,
                style = AppTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DotsLoadingIndicator(
    tint: Color
) {
    val transition = rememberInfiniteTransition(label = "dots_loading")

    val dot1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Dot(tint = tint, alpha = dot1)
        Dot(tint = tint, alpha = dot2)
        Dot(tint = tint, alpha = dot3)
    }
}

@Composable
private fun Dot(
    tint: Color,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = tint.copy(alpha = alpha),
                shape = RoundedCornerShape(50)
            )
    )
}

@Composable
fun LoadingOverlay(
    modifier: Modifier = Modifier,
    visible: Boolean,
    title: String? = null,
    message: String? = null,
    variant: LoadingIndicatorVariant = LoadingIndicatorVariant.Circular,
    size: LoadingIndicatorSize = LoadingIndicatorSize.Medium,
    tint: Color? = null,
    trackColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val resolvedTint = tint ?: MaterialTheme.colorScheme.primary
    val resolvedTrackColor = trackColor ?: resolvedTint.copy(alpha = 0.16f)

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    variant = variant,
                    size = size,
                    title = title,
                    message = message,
                    tint = resolvedTint,
                    trackColor = resolvedTrackColor,
                    centered = true
                )
            }
        }
    }
}

@Composable
fun LoadingInline(
    modifier: Modifier = Modifier,
    text: String = "جاري التحميل",
    tint: Color? = null
) {
    val resolvedTint = tint ?: MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = resolvedTint,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(AppDimens.Spacing.small))
        Text(
            text = text,
            style = AppTypography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}