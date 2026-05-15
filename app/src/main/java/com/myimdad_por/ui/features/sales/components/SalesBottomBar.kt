package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ReceiptLong
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
fun SalesBottomBar(
    subtotalAmount: BigDecimal,
    discountAmount: BigDecimal,
    taxAmount: BigDecimal,
    totalAmount: BigDecimal,
    paidAmount: BigDecimal,
    remainingAmount: BigDecimal,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onPayClick: () -> Unit = {},
    onSaveDraftClick: () -> Unit = {},
    onClearCartClick: () -> Unit = {}
) {
    val subtotal = subtotalAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val discount = discountAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val tax = taxAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val total = totalAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val paid = paidAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val remaining = remainingAmount.orZero().coerceAtLeast(BigDecimal.ZERO)

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                .navigationBarsPadding()
                .padding(
                    horizontal = AppDimens.Spacing.large,
                    vertical = AppDimens.Spacing.medium
                ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            SalesBottomBarHeader()

            SalesBottomBarSummary(
                subtotalAmount = subtotal,
                discountAmount = discount,
                taxAmount = tax,
                totalAmount = total,
                paidAmount = paid,
                remainingAmount = remaining
            )

            SalesBottomBarActions(
                enabled = enabled,
                loading = loading,
                onPayClick = onPayClick,
                onSaveDraftClick = onSaveDraftClick,
                onClearCartClick = onClearCartClick
            )
        }
    }
}

@Composable
private fun SalesBottomBarHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Surface(
                shape = CircleShape,
                color = BrandPrimarySoft,
                contentColor = BrandPrimary,
                border = BorderStroke(1.dp, BrandPrimaryTint)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Column {
                Text(
                    text = "الملخص السفلي",
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "مراجعة نهائية قبل التنفيذ",
                    color = TextSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = BrandPrimary
                )
                Spacer(modifier = Modifier.width(AppDimens.Spacing.extraSmall))
                Text(
                    text = "جاهز",
                    color = BrandPrimary
                )
            }
        }
    }
}

@Composable
private fun SalesBottomBarSummary(
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
        color = BrandPrimarySoft,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            BottomSummaryRow(
                label = "الإجمالي",
                value = CurrencyFormatter.formatSDG(subtotalAmount),
                accent = TextSecondaryColor,
                icon = Icons.Default.AttachMoney
            )
            BottomSummaryRow(
                label = "الخصم",
                value = CurrencyFormatter.formatSDG(discountAmount),
                accent = ErrorColor,
                icon = Icons.Default.Savings
            )
            BottomSummaryRow(
                label = "الضريبة",
                value = CurrencyFormatter.formatSDG(taxAmount),
                accent = BrandPrimary,
                icon = Icons.Default.SwapHoriz
            )
            BottomSummaryRow(
                label = "المدفوع",
                value = CurrencyFormatter.formatSDG(paidAmount),
                accent = SuccessColor,
                icon = Icons.Default.Payments
            )
            BottomSummaryRow(
                label = "المتبقي",
                value = CurrencyFormatter.formatSDG(remainingAmount),
                accent = ErrorColor,
                icon = Icons.Default.AttachMoney
            )

            Surface(
                shape = AppShapeTokens.filledCard,
                color = WhiteColor,
                contentColor = TextPrimaryColor,
                border = BorderStroke(1.dp, BorderColor)
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
private fun BottomSummaryRow(
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
private fun SalesBottomBarActions(
    enabled: Boolean,
    loading: Boolean,
    onPayClick: () -> Unit,
    onSaveDraftClick: () -> Unit,
    onClearCartClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppButton(
            text = "تفريغ",
            onClick = onClearCartClick,
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
            onClick = onSaveDraftClick,
            variant = AppButtonVariant.Secondary,
            size = AppButtonSize.Medium,
            enabled = enabled && !loading,
            loading = false,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null
                )
            }
        )

        AppButton(
            text = "اعتماد",
            onClick = onPayClick,
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.Medium,
            enabled = enabled,
            loading = loading,
            fullWidth = false,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = null
                )
            }
        )
    }
}