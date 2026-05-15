package com.myimdad_por.ui.features.sales.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.orZero
import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.ui.components.AppTextField
import com.myimdad_por.ui.components.AppTextFieldSize
import com.myimdad_por.ui.components.AppTextFieldVariant
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
import java.math.RoundingMode
import java.text.DecimalFormat

private val ZERO: BigDecimal = BigDecimal.ZERO

@Composable
fun DiscountSection(
    discountInput: String,
    onDiscountInputChange: (String) -> Unit,
    subtotalAmount: BigDecimal,
    modifier: Modifier = Modifier,
    discountTitle: String = "الخصم",
    enabled: Boolean = true,
    loading: Boolean = false,
    onApplyDiscount: () -> Unit = {},
    onClearDiscount: () -> Unit = {},
    onPresetDiscountClick: (BigDecimal) -> Unit = {}
) {
    val subtotal = subtotalAmount.orZero()
    val enteredDiscount = discountInput.toBigDecimalOrZero()
    val safeDiscount = clampDiscount(
        value = enteredDiscount,
        max = subtotal
    )
    val totalAfterDiscount = subtotal.subtract(safeDiscount).let { if (it < ZERO) ZERO else it }

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
            DiscountSectionHeader(
                title = discountTitle,
                loading = loading
            )

            AppTextField(
                value = discountInput,
                onValueChange = onDiscountInputChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !loading,
                label = "قيمة الخصم",
                placeholder = "أدخل الخصم كقيمة رقمية",
                helperText = "مثال: 10 أو 25.50",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = BrandPrimary
                    )
                },
                variant = AppTextFieldVariant.Outlined,
                size = AppTextFieldSize.Large,
                contentDescription = "حقل الخصم"
            )

            QuickDiscountPresets(
                enabled = enabled && !loading,
                onPresetDiscountClick = onPresetDiscountClick
            )

            DiscountSummaryCard(
                subtotalAmount = subtotal,
                discountAmount = safeDiscount,
                totalAfterDiscount = totalAfterDiscount
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClearDiscount,
                    enabled = enabled && !loading,
                    shape = AppShapeTokens.buttonPill,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        text = "مسح الخصم",
                        color = TextSecondaryColor
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                DiscountApplyButton(
                    enabled = enabled && !loading,
                    onClick = onApplyDiscount
                )
            }
        }
    }
}

@Composable
private fun DiscountSectionHeader(
    title: String,
    loading: Boolean
) {
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
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary)
                )
                Text(
                    text = title,
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = if (loading) "جارٍ تجهيز الخصم..." else "اختر خصمًا سريعًا أو أدخل قيمة مخصصة",
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
                    text = "مرن",
                    color = BrandPrimary
                )
            }
        }
    }
}

@Composable
private fun QuickDiscountPresets(
    enabled: Boolean,
    onPresetDiscountClick: (BigDecimal) -> Unit
) {
    val presets = listOf(
        BigDecimal("5"),
        BigDecimal("10"),
        BigDecimal("15"),
        BigDecimal("20")
    )

    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Text(
            text = "خصومات سريعة",
            color = TextSecondaryColor
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            presets.forEach { preset ->
                DiscountPresetChip(
                    value = preset,
                    enabled = enabled,
                    onClick = { onPresetDiscountClick(preset) }
                )
            }
        }
    }
}

@Composable
private fun DiscountPresetChip(
    value: BigDecimal,
    enabled: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                text = "${formatNumber(value)}%",
                color = if (enabled) BrandPrimary else TextSecondaryColor
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Percent,
                contentDescription = null,
                tint = if (enabled) BrandPrimary else TextSecondaryColor
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = WhiteColor,
            labelColor = BrandPrimary
        ),
        border = AssistChipDefaults.assistChipBorder(
            enabled = enabled,
            borderColor = BrandPrimaryTint,
            disabledBorderColor = BorderColor
        )
    )
}

@Composable
private fun DiscountSummaryCard(
    subtotalAmount: BigDecimal,
    discountAmount: BigDecimal,
    totalAfterDiscount: BigDecimal
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
            SummaryRow(
                label = "الإجمالي قبل الخصم",
                value = formatMoney(subtotalAmount),
                accent = TextSecondaryColor
            )
            SummaryRow(
                label = "قيمة الخصم",
                value = formatMoney(discountAmount),
                accent = ErrorColor
            )
            SummaryRow(
                label = "الإجمالي بعد الخصم",
                value = formatMoney(totalAfterDiscount),
                accent = SuccessColor,
                emphasized = true
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    accent: Color,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (emphasized) TextPrimaryColor else TextSecondaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = accent
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

@Composable
private fun DiscountApplyButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier,
        shape = AppShapeTokens.buttonPill,
        color = BrandPrimary,
        contentColor = WhiteColor,
        tonalElevation = AppDimens.Elevation.low,
        shadowElevation = AppDimens.Elevation.low
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = AppDimens.Spacing.large,
                    vertical = AppDimens.Spacing.medium
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                tint = WhiteColor
            )
            Spacer(modifier = Modifier.width(AppDimens.Spacing.small))
            Text(
                text = "تطبيق الخصم",
                color = WhiteColor
            )
        }
    }
}

private fun clampDiscount(
    value: BigDecimal,
    max: BigDecimal
): BigDecimal {
    val safeValue = when {
        value < ZERO -> ZERO
        else -> value
    }
    return when {
        safeValue > max -> max
        else -> safeValue
    }
}

private fun formatMoney(value: BigDecimal): String {
    return DecimalFormat("#,##0.##").format(value.setScale(2, RoundingMode.HALF_UP))
}

private fun formatNumber(value: BigDecimal): String {
    return DecimalFormat("#,##0.##").format(value.setScale(2, RoundingMode.HALF_UP))
}