package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.orZero
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.BorderColor
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor
import java.math.BigDecimal

@Composable
fun SalesSummary(
    subtotalAmount: BigDecimal,
    discountAmount: BigDecimal,
    taxAmount: BigDecimal,
    totalAmount: BigDecimal,
    paidAmount: BigDecimal,
    remainingAmount: BigDecimal,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onConfirmSale: () -> Unit = {},
    onCreateDraft: () -> Unit = {},
    onClearCart: () -> Unit = {}
) {
    val subtotal = subtotalAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val discount = discountAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val tax = taxAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val total = totalAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val paid = paidAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val remaining = remainingAmount.orZero().coerceAtLeast(BigDecimal.ZERO)

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
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
        ) {
            SalesSummaryHeader()

            SummaryHighlightCard(
                label = "الإجمالي النهائي",
                value = CurrencyFormatter.formatSDG(total),
                accent = BrandPrimary
            )

            SummaryDetailsCard(
                subtotalAmount = subtotal,
                discountAmount = discount,
                taxAmount = tax,
                totalAmount = total,
                paidAmount = paid,
                remainingAmount = remaining
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppButton(
                    text = "تفريغ السلة",
                    onClick = onClearCart,
                    variant = AppButtonVariant.Outlined,
                    size = AppButtonSize.Medium,
                    enabled = enabled && !loading,
                    loading = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null
                        )
                    }
                )

                AppButton(
                    text = "مسودة",
                    onClick = onCreateDraft,
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Medium,
                    enabled = enabled && !loading,
                    loading = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null
                        )
                    }
                )

                AppButton(
                    text = "اعتماد البيع",
                    onClick = onConfirmSale,
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Medium,
                    enabled = enabled && !loading,
                    loading = loading,
                    fullWidth = false,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingCartCheckout,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SalesSummaryHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
            ) {
                BoxDot()
                Text(
                    text = "ملخص البيع",
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "مراجعة نهائية سريعة قبل حفظ الفاتورة",
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
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = BrandPrimary
                )
                Spacer(modifier = Modifier.width(AppDimens.Spacing.extraSmall))
                Text(
                    text = "منظم",
                    color = BrandPrimary
                )
            }
        }
    }
}

@Composable
private fun SummaryHighlightCard(
    label: String,
    value: String,
    accent: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.filledCard,
        color = BrandPrimarySoft,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f),
                contentColor = accent
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = label,
                    color = TextSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SummaryDetailsCard(
    subtotalAmount: BigDecimal,
    discountAmount: BigDecimal,
    taxAmount: BigDecimal,
    totalAmount: BigDecimal,
    paidAmount: BigDecimal,
    remainingAmount: BigDecimal
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.filledCard,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            SummaryLine(
                label = "الإجمالي",
                value = CurrencyFormatter.formatSDG(subtotalAmount),
                accent = TextSecondaryColor,
                icon = Icons.Default.AttachMoney
            )
            SummaryLine(
                label = "الخصم",
                value = CurrencyFormatter.formatSDG(discountAmount),
                accent = ErrorColor,
                icon = Icons.Default.Savings
            )
            SummaryLine(
                label = "الضريبة",
                value = CurrencyFormatter.formatSDG(taxAmount),
                accent = BrandPrimary,
                icon = Icons.Default.SwapHoriz
            )
            SummaryLine(
                label = "المدفوع",
                value = CurrencyFormatter.formatSDG(paidAmount),
                accent = SuccessColor,
                icon = Icons.Default.AttachMoney
            )
            SummaryLine(
                label = "المتبقي",
                value = CurrencyFormatter.formatSDG(remainingAmount),
                accent = ErrorColor,
                icon = Icons.Default.ArrowForward
            )

            Surface(
                shape = AppShapeTokens.filledCard,
                color = BrandPrimarySoft,
                contentColor = TextPrimaryColor,
                border = BorderStroke(1.dp, BrandPrimaryTint)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.Spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الإجمالي النهائي",
                        color = TextPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = CurrencyFormatter.formatSDG(totalAmount),
                        color = BrandPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent
            )
            Text(
                text = label,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = value,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BoxDot() {
    Column(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(BrandPrimary)
    ) {}
}