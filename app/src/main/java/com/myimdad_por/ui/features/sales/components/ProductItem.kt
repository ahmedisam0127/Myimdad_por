package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTextStyles
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor

@Composable
fun ProductItem(
    product: ProductUiModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onAddClick: (() -> Unit)? = null,
    showBarcode: Boolean = true,
    showConversion: Boolean = true,
    showStatus: Boolean = true,
    addButtonText: String = "إضافة",
    compact: Boolean = false
) {
    val cardModifier = modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Card(
        modifier = cardModifier,
        shape = AppShapeTokens.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimens.Elevation.high)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 132.dp else 156.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                BrandPrimary,
                                Color(0xFF1FB45A),
                                BrandPrimaryDark
                            )
                        )
                    )
                    .padding(AppDimens.Spacing.large)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.14f),
                            shape = AppShapeTokens.badge,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = product.primaryUnitLabel,
                                modifier = Modifier.padding(
                                    horizontal = AppDimens.Spacing.medium,
                                    vertical = AppDimens.Spacing.extraSmall
                                ),
                                style = AppTextStyles.ArabicBody,
                                color = IconOnBrandColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (showStatus) {
                            StatusChip(
                                active = product.isActive && product.isAvailableForSale
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                    ) {
                        Text(
                            text = product.displayName,
                            style = AppTextStyles.ArabicTitle,
                            color = IconOnBrandColor,
                            maxLines = if (compact) 2 else 3,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = product.formattedPrice,
                            style = AppTextStyles.NumberDisplay.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = IconOnBrandColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppDimens.Spacing.large,
                        vertical = AppDimens.Spacing.large
                    ),
                verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
            ) {
                if (showBarcode) {
                    InfoRow(
                        icon = Icons.Filled.Inventory2,
                        text = product.barcode,
                        tint = BrandPrimaryDark
                    )
                }

                if (showConversion && product.supportsUnitHierarchy) {
                    InfoRow(
                        icon = Icons.Filled.LocalOffer,
                        text = product.conversionLabel,
                        tint = SuccessColor
                    )
                }

                if (product.shortDescription.isNotBlank()) {
                    Text(
                        text = product.shortDescription,
                        style = AppTextStyles.ArabicBody,
                        color = TextSecondaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                ActionButton(
                    text = addButtonText,
                    enabled = product.isActive && product.isAvailableForSale,
                    onClick = onAddClick ?: {}
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    active: Boolean
) {
    val container = if (active) BrandPrimarySoft else Color(0xFFF3F4F6)
    val content = if (active) BrandPrimaryDark else TextSecondaryColor

    Surface(
        color = container.copy(alpha = 0.92f),
        shape = AppShapeTokens.badge,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing.medium,
                vertical = AppDimens.Spacing.extraSmall
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.extraSmall)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (active) "متاح" else "غير متاح",
                style = AppTextStyles.ArabicBody,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
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
            color = TextPrimaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val gradient = if (enabled) {
        Brush.horizontalGradient(
            colors = listOf(BrandPrimary, BrandPrimaryDark)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFB7C9BF),
                Color(0xFF9DAAA2)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppShapeTokens.buttonPill)
            .background(brush = gradient)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTextStyles.ArabicTitle,
            color = IconOnBrandColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}