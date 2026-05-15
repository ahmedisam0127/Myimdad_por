package com.myimdad_por.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens

object AppTopBarDefaults {
    val horizontalPadding: Dp = AppDimens.Layout.screenPadding
    val verticalPadding: Dp = AppDimens.Spacing.small
    val minHeight: Dp = AppDimens.Component.topBarHeight
    val dividerThickness: Dp = 1.dp
    val navigationButtonSize: Dp = 48.dp
}

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    centeredTitle: Boolean = false,
    showDivider: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppTopBarDefaults.minHeight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTopBarDefaults.horizontalPadding,
                        vertical = AppTopBarDefaults.verticalPadding
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navIcon = when {
                    navigationIcon != null -> navigationIcon
                    onBackClick != null -> {
                        {
                            AppTopBarBackButton(onClick = onBackClick)
                        }
                    }
                    else -> null
                }

                if (centeredTitle) {
                    if (navIcon != null) {
                        navIcon()
                    } else {
                        Spacer(modifier = Modifier.size(AppTopBarDefaults.navigationButtonSize))
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        TitleBlock(
                            title = title,
                            subtitle = subtitle,
                            subtitleColor = subtitleColor,
                            centered = true
                        )
                    }

                    Spacer(modifier = Modifier.size(AppTopBarDefaults.navigationButtonSize))
                } else {
                    if (navIcon != null) {
                        navIcon()
                    }

                    Spacer(modifier = Modifier.size(AppDimens.Spacing.small))

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        TitleBlock(
                            title = title,
                            subtitle = subtitle,
                            subtitleColor = subtitleColor,
                            centered = false
                        )
                    }

                    Spacer(modifier = Modifier.size(AppDimens.Spacing.small))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }

            if (showDivider) {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = AppTopBarDefaults.dividerThickness,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
private fun TitleBlock(
    title: String,
    subtitle: String?,
    subtitleColor: Color,
    centered: Boolean
) {
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.padding(top = AppDimens.Spacing.extraSmall))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppTopBarBackButton(
    onClick: () -> Unit,
    contentDescription: String = "رجوع"
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(AppTopBarDefaults.navigationButtonSize)
            .semantics {
                this.contentDescription = contentDescription
            }
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AppTopBarSimple(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
    centeredTitle: Boolean = false
) {
    AppTopBar(
        title = title,
        modifier = modifier,
        onBackClick = onBackClick,
        actions = actions,
        centeredTitle = centeredTitle,
        showDivider = true
    )
}