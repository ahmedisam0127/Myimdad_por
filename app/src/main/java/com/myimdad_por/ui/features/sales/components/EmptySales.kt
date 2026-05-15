package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
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
fun EmptySales(
    modifier: Modifier = Modifier,
    title: String = "لا توجد بيانات بعد",
    message: String = "ابدأ بإضافة منتج إلى السلة، أو ابحث عن عميل، أو أنشئ أول فاتورة.",
    primaryActionText: String = "إضافة منتج",
    secondaryActionText: String = "تحديث",
    onPrimaryActionClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapeTokens.card,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint),
        tonalElevation = AppDimens.Elevation.low,
        shadowElevation = AppDimens.Elevation.low
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
        ) {
            EmptySalesIllustration()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
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
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            EmptySalesHints()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppButton(
                    text = secondaryActionText,
                    onClick = onSecondaryActionClick,
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Medium,
                    fullWidth = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                    }
                )

                AppButton(
                    text = primaryActionText,
                    onClick = onPrimaryActionClick,
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Medium,
                    fullWidth = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptySalesIllustration() {
    Surface(
        shape = CircleShape,
        color = BrandPrimarySoft,
        contentColor = BrandPrimary,
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(40.dp)
            )

            Surface(
                shape = CircleShape,
                color = WhiteColor,
                contentColor = BrandPrimary,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySalesHints() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmptyHintChip(text = "بحث سريع")
        EmptyHintChip(text = "عميل جديد")
        EmptyHintChip(text = "فاتورة نظيفة")
    }
}

@Composable
private fun EmptyHintChip(text: String) {
    Surface(
        shape = AppShapeTokens.buttonPill,
        color = BrandPrimarySoft,
        contentColor = BrandPrimary,
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing.medium,
                vertical = AppDimens.Spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxDot()
            Spacer(modifier = Modifier.width(AppDimens.Spacing.extraSmall))
            Text(
                text = text,
                color = BrandPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BoxDot() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(BrandPrimary)
    )
}