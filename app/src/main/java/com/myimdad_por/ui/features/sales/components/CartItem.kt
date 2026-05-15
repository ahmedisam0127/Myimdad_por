package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTextStyles
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor

@Composable
fun CartItem(
    item: CartUiModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onIncreaseClick: (() -> Unit)? = null,
    onDecreaseClick: (() -> Unit)? = null,
    onRemoveClick: (() -> Unit)? = null,
    showBarcode: Boolean = true,
    showNote: Boolean = true,
    showSubtotal: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.high)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
                ) {
                    Text(
                        text = item.productDisplayName,
                        style = AppTextStyles.ArabicTitle,
                        color = TextPrimaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BrandPrimarySoft,
                            shape = AppShapeTokens.badge,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = item.itemTypeLabel,
                                modifier = Modifier.padding(
                                    horizontal = AppDimens.Spacing.medium,
                                    vertical = AppDimens.Spacing.extraSmall
                                ),
                                style = AppTextStyles.ArabicBody,
                                color = BrandPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(AppDimens.Spacing.small))

                        Surface(
                            color = Color(0xFFF3F4F6),
                            shape = AppShapeTokens.badge,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = item.itemCountLabel,
                                modifier = Modifier.padding(
                                    horizontal = AppDimens.Spacing.medium,
                                    vertical = AppDimens.Spacing.extraSmall
                                ),
                                style = AppTextStyles.ArabicBody,
                                color = TextSecondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
                ) {
                    Text(
                        text = item.formattedTotalAmount,
                        style = AppTextStyles.NumberDisplay.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = BrandPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = item.formattedUnitPrice,
                        style = AppTextStyles.ArabicBody,
                        color = TextSecondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showBarcode || showNote) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                ) {
                    if (showBarcode && item.barcode.isNotBlank()) {
                        InfoRow(
                            icon = Icons.Filled.ReceiptLong,
                            text = item.barcode,
                            tint = BrandPrimaryDark
                        )
                    }

                    if (showNote && item.noteLabel.isNotBlank()) {
                        InfoRow(
                            icon = Icons.Filled.Straighten,
                            text = item.noteLabel,
                            tint = SuccessColor
                        )
                    }
                }
            }

            if (showSubtotal) {
                Surface(
                    color = Color(0xFFF7FBF8),
                    shape = AppShapeTokens.filledCard,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppDimens.Spacing.medium,
                                vertical = AppDimens.Spacing.small
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الإجمالي الجزئي",
                            style = AppTextStyles.ArabicBody,
                            color = TextSecondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.formattedSubtotal,
                            style = AppTextStyles.NumberDisplay.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = BrandPrimaryDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuantityControlButton(
                    icon = Icons.Filled.Remove,
                    contentDescription = "إنقاص",
                    tint = BrandPrimaryDark,
                    onClick = onDecreaseClick ?: {}
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(IntrinsicSize.Min)
                        .clip(AppShapeTokens.buttonPill)
                        .background(Color(0xFFF4F7F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.formattedQuantity,
                        modifier = Modifier.padding(
                            horizontal = AppDimens.Spacing.medium,
                            vertical = AppDimens.Spacing.small
                        ),
                        style = AppTextStyles.ArabicTitle,
                        color = TextPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                QuantityControlButton(
                    icon = Icons.Filled.Add,
                    contentDescription = "زيادة",
                    tint = BrandPrimary,
                    onClick = onIncreaseClick ?: {}
                )

                Spacer(modifier = Modifier.width(AppDimens.Spacing.extraSmall))

                QuantityControlButton(
                    icon = Icons.Filled.DeleteOutline,
                    contentDescription = "حذف",
                    tint = ErrorColor,
                    containerColor = Color(0xFFFDECEC),
                    onClick = onRemoveClick ?: {}
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
        }

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = AppTextStyles.ArabicBody,
            color = TextSecondaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QuantityControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    containerColor: Color = Color(0xFFF3F4F6),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(AppShapeTokens.buttonPill)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}