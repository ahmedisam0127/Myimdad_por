package com.myimdad_por.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.domain.model.Product
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.AppTextStyles
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimaryDark
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.IconOnBrandColor
import com.myimdad_por.ui.theme.SurfaceColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    priceText: String = CurrencyFormatter.formatSDG(product.price),
    firstTagText: String? = null,
    secondTagText: String? = null,
    actionText: String = "بيع الآن",
    onActionClick: () -> Unit = {},
    onCardClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onCardClick != null) {
                    Modifier.clickable(onClick = onCardClick)
                } else {
                    Modifier
                }
            ),
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
                    .aspectRatio(1.15f)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BrandPrimary,
                                BrandPrimaryDark
                            )
                        )
                    )
                    .padding(
                        horizontal = AppDimens.Layout.screenPadding,
                        vertical = AppDimens.Spacing.large
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
                ) {
                    Text(
                        text = product.effectiveName,
                        style = AppTextStyles.ArabicTitle,
                        color = IconOnBrandColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = priceText,
                        style = AppTextStyles.NumberDisplay,
                        color = IconOnBrandColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        firstTagText?.let {
                            TagPill(
                                text = it,
                                containerColor = BrandPrimarySoft,
                                contentColor = BrandPrimaryDark
                            )
                        }
                        secondTagText?.let {
                            TagPill(
                                text = it,
                                containerColor = Color(0xFFF7EEF0),
                                contentColor = Color(0xFFB85C6B)
                            )
                        }
                    }
                }
            }

            Surface(
                color = SurfaceColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppDimens.Layout.screenPadding,
                            vertical = AppDimens.Spacing.large
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ActionButton(
                        text = actionText,
                        onClick = onActionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TagPill(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = AppShapeTokens.chip,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = AppDimens.Spacing.large,
                vertical = AppDimens.Spacing.small
            ),
            style = AppTextStyles.ArabicBody,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(AppShapeTokens.buttonPill)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        BrandPrimary,
                        BrandPrimaryDark
                    )
                )
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppTextStyles.ArabicTitle,
            color = IconOnBrandColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}