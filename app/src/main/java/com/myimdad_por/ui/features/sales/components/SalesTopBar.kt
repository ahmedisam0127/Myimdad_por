package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens

private val SalesTopBarPrimary =
    Color(0xFF22C55E)

private val SalesTopBarPrimaryDark =
    Color(0xFF16A34A)

private val SalesTopBarSoft =
    Color(0xFFEAFBF1)

private val SalesTopBarTextLight =
    Color.White

@Composable
fun SalesTopBar(
    title: String = "المبيعات",
    modifier: Modifier = Modifier,
    subtitle: String = "إدارة الفواتير والمبيعات اليومية",
    onBackClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shadowElevation = AppDimens.Elevation.medium
    ) {

        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 28.dp,
                            bottomEnd = 28.dp
                        )
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SalesTopBarPrimary,
                                SalesTopBarPrimaryDark
                            )
                        )
                    )
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppDimens.Layout.screenPadding,
                            vertical = AppDimens.Spacing.large
                        )
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            if (onBackClick != null) {

                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Color.White.copy(alpha = 0.14f)
                                        )
                                ) {

                                    Icon(
                                        imageVector = Icons.Rounded.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = SalesTopBarTextLight
                                    )
                                }

                                Spacer(
                                    modifier = Modifier.size(
                                        AppDimens.Spacing.medium
                                    )
                                )
                            }

                            Column {

                                Text(
                                    text = title,
                                    color = SalesTopBarTextLight,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(
                                    modifier = Modifier.height(
                                        AppDimens.Spacing.extraSmall
                                    )
                                )

                                Text(
                                    text = subtitle,
                                    color = SalesTopBarTextLight.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            content = actions
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(
                            AppDimens.Spacing.large
                        )
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SalesTopBarSoft
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = AppDimens.Spacing.large,
                                    vertical = AppDimens.Spacing.large
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        SalesTopBarPrimary.copy(alpha = 0.14f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                Icon(
                                    imageVector = Icons.Rounded.PointOfSale,
                                    contentDescription = null,
                                    tint = SalesTopBarPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(
                                modifier = Modifier.size(
                                    AppDimens.Spacing.large
                                )
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "نظام المبيعات الذكي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(
                                    modifier = Modifier.height(
                                        AppDimens.Spacing.extraSmall
                                    )
                                )

                                Text(
                                    text = "إدارة سريعة للفواتير والمنتجات والعملاء",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (showDivider) {

                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}