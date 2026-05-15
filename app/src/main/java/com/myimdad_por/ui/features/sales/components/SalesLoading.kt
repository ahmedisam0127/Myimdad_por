package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor

@Composable
fun SalesLoading(
    modifier: Modifier = Modifier,
    title: String = "جاري تحميل شاشة المبيعات",
    message: String = "يرجى الانتظار قليلًا أثناء تجهيز البيانات..."
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
    ) {
        SalesLoadingHeader(
            title = title,
            message = message
        )

        SalesLoadingCard(
            height = 140.dp,
            titleWidth = 180.dp,
            lines = 3
        )

        SalesLoadingCard(
            height = 180.dp,
            titleWidth = 140.dp,
            lines = 4
        )

        SalesLoadingBottomBar()
    }
}

@Composable
private fun SalesLoadingHeader(
    title: String,
    message: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint),
        tonalElevation = AppDimens.Elevation.low,
        shadowElevation = AppDimens.Elevation.low
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BrandPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.5.dp,
                    color = BrandPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = message,
                    color = TextSecondaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = BrandPrimarySoft,
                contentColor = BrandPrimary,
                border = BorderStroke(1.dp, BrandPrimaryTint)
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = AppDimens.Spacing.small,
                        vertical = 6.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = BrandPrimary
                    )
                    Spacer(modifier = Modifier.width(AppDimens.Spacing.extraSmall))
                    Text(
                        text = "تحميل",
                        color = BrandPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun SalesLoadingCard(
    height: androidx.compose.ui.unit.Dp,
    titleWidth: androidx.compose.ui.unit.Dp,
    lines: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.55f)),
        tonalElevation = AppDimens.Elevation.low,
        shadowElevation = AppDimens.Elevation.low
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            SkeletonLine(
                width = titleWidth,
                height = 18.dp
            )

            repeat(lines) { index ->
                SkeletonLine(
                    width = when (index) {
                        0 -> 220.dp
                        1 -> 260.dp
                        2 -> 200.dp
                        else -> 240.dp
                    },
                    height = 14.dp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(AppShapeTokens.filledCard)
                    .background(BrandPrimarySoft.copy(alpha = 0.55f))
            )
        }
    }
}

@Composable
private fun SalesLoadingBottomBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.bottomBar,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint),
        tonalElevation = AppDimens.Elevation.medium,
        shadowElevation = AppDimens.Elevation.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppDimens.Spacing.large,
                    vertical = AppDimens.Spacing.medium
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            SkeletonLine(
                width = 120.dp,
                height = 16.dp
            )

            SkeletonLine(
                width = 240.dp,
                height = 14.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                SkeletonButton(width = 92.dp)
                SkeletonButton(width = 92.dp)
                SkeletonButton(width = 124.dp)
            }
        }
    }
}

@Composable
private fun SkeletonLine(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(BrandPrimarySoft.copy(alpha = 0.8f))
    )
}

@Composable
private fun SkeletonButton(width: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(48.dp)
            .clip(AppShapeTokens.buttonPill)
            .background(BrandPrimarySoft.copy(alpha = 0.8f))
    )
}