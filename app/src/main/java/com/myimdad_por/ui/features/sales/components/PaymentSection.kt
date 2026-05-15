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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
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
import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import com.myimdad_por.ui.components.AppButton
import com.myimdad_por.ui.components.AppButtonSize
import com.myimdad_por.ui.components.AppButtonVariant
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

@Composable
fun PaymentSection(
    selectedPaymentMethod: PaymentMethod,
    onPaymentMethodSelected: (PaymentMethod) -> Unit,
    paidAmountInput: String,
    onPaidAmountInputChange: (String) -> Unit,
    subtotalAmount: BigDecimal,
    totalAmount: BigDecimal,
    remainingAmount: BigDecimal,
    referenceNumber: String,
    onReferenceNumberChange: (String) -> Unit,
    creditSaleEnabled: Boolean,
    onCreditSaleToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    paymentMethods: List<PaymentMethod> = PaymentMethod.values(),
    onApplyPayment: () -> Unit = {}
) {
    val safeSubtotal = subtotalAmount.orZero()
    val safeTotal = totalAmount.orZero()
    val safeRemaining = remainingAmount.orZero()
    val paidAmountPreview = paidAmountInput.toBigDecimalOrZero().coerceAtLeast(BigDecimal.ZERO)
    val effectivePaidAmount = paidAmountPreview.coerceAtMost(safeTotal)
    val showReferenceField = selectedPaymentMethod.requiresReference
    val referenceHint = when (selectedPaymentMethod.type) {
        PaymentMethodType.CASH -> "عادة لا يحتاج إلى مرجع"
        PaymentMethodType.BANK_TRANSFER -> "أدخل رقم التحويل البنكي"
        PaymentMethodType.WALLET -> "أدخل رقم العملية أو الإيصال"
        PaymentMethodType.POS -> "أدخل رقم الإيصال من الجهاز"
        PaymentMethodType.CHEQUE -> "أدخل رقم الشيك أو المرجع"
        PaymentMethodType.OTHER -> "أدخل المرجع"
    }
    val paymentNote = when {
        creditSaleEnabled -> "تم تفعيل البيع الآجل لهذا العميل"
        selectedPaymentMethod.type == PaymentMethodType.CASH -> "الدفع نقدًا يجري مباشرة دون مرجع"
        selectedPaymentMethod.isElectronic() -> "هذه الطريقة تحتاج مرجعًا لتوثيق العملية"
        else -> "اختر طريقة الدفع المناسبة"
    }

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
            PaymentSectionHeader()

            PaymentMethodRow(
                paymentMethods = paymentMethods,
                selectedPaymentMethod = selectedPaymentMethod,
                enabled = enabled && !loading,
                onPaymentMethodSelected = onPaymentMethodSelected
            )

            PaymentSummaryCard(
                subtotalAmount = safeSubtotal,
                totalAmount = safeTotal,
                remainingAmount = safeRemaining,
                paidAmount = effectivePaidAmount
            )

            AppTextField(
                value = paidAmountInput,
                onValueChange = onPaidAmountInputChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled && !loading,
                label = "المبلغ المدفوع",
                placeholder = "0.00",
                helperText = "أدخل المبلغ الذي تم استلامه فعلاً",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = BrandPrimary
                    )
                },
                variant = AppTextFieldVariant.Outlined,
                size = AppTextFieldSize.Large,
                contentDescription = "حقل المبلغ المدفوع"
            )

            if (showReferenceField) {
                AppTextField(
                    value = referenceNumber,
                    onValueChange = onReferenceNumberChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled && !loading,
                    label = "رقم المرجع",
                    placeholder = "رقم العملية، الإيصال، أو الشيك",
                    helperText = referenceHint,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = BrandPrimary
                        )
                    },
                    variant = AppTextFieldVariant.Outlined,
                    size = AppTextFieldSize.Large,
                    contentDescription = "حقل رقم المرجع"
                )
            }

            CreditSaleToggleCard(
                enabled = enabled && !loading,
                creditSaleEnabled = creditSaleEnabled,
                onCreditSaleToggle = onCreditSaleToggle
            )

            PaymentHintCard(message = paymentNote)

            AppButton(
                text = "اعتماد الدفع",
                onClick = onApplyPayment,
                modifier = Modifier.fillMaxWidth(),
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Large,
                enabled = enabled,
                loading = loading,
                fullWidth = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null
                    )
                }
            )
        }
    }
}

@Composable
private fun PaymentSectionHeader() {
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
                    text = "الدفع",
                    color = TextPrimaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "اختر طريقة الدفع وأكمل العملية بأناقة ووضوح",
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
                    imageVector = Icons.Default.KeyboardArrowDown,
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
private fun PaymentMethodRow(
    paymentMethods: List<PaymentMethod>,
    selectedPaymentMethod: PaymentMethod,
    enabled: Boolean,
    onPaymentMethodSelected: (PaymentMethod) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Text(
            text = "طريقة الدفع",
            color = TextSecondaryColor
        )

        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
            paymentMethods.forEach { method ->
                PaymentMethodItem(
                    method = method,
                    selected = method.id == selectedPaymentMethod.id,
                    enabled = enabled,
                    onClick = { onPaymentMethodSelected(method) }
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodItem(
    method: PaymentMethod,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val accent = when (method.type) {
        PaymentMethodType.CASH -> BrandPrimary
        PaymentMethodType.BANK_TRANSFER -> SuccessColor
        PaymentMethodType.WALLET -> BrandPrimary
        PaymentMethodType.POS -> SuccessColor
        PaymentMethodType.CHEQUE -> TextSecondaryColor
        PaymentMethodType.OTHER -> BrandPrimary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = AppShapeTokens.filledCard,
        color = if (selected) BrandPrimarySoft else WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) BrandPrimary else BorderColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = paymentIcon(method),
                    contentDescription = null,
                    tint = accent
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                ) {
                    Text(
                        text = method.displayName,
                        color = TextPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (selected) {
                        StatusPill(text = "محدد", tint = BrandPrimary)
                    }
                }

                Text(
                    text = method.name,
                    color = TextSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (method.requiresReference) {
                Text(
                    text = "مرجع",
                    color = SuccessColor
                )
            }
        }
    }
}

@Composable
private fun PaymentSummaryCard(
    subtotalAmount: BigDecimal,
    totalAmount: BigDecimal,
    remainingAmount: BigDecimal,
    paidAmount: BigDecimal
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
                label = "الإجمالي الفرعي",
                value = CurrencyFormatter.formatSDG(subtotalAmount),
                accent = TextSecondaryColor
            )
            SummaryRow(
                label = "المدفوع",
                value = CurrencyFormatter.formatSDG(paidAmount),
                accent = SuccessColor
            )
            SummaryRow(
                label = "المتبقي",
                value = CurrencyFormatter.formatSDG(remainingAmount),
                accent = ErrorColor,
                emphasized = true
            )
            SummaryRow(
                label = "الإجمالي النهائي",
                value = CurrencyFormatter.formatSDG(totalAmount),
                accent = BrandPrimary,
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
                imageVector = Icons.Default.SwapHoriz,
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
private fun CreditSaleToggleCard(
    enabled: Boolean,
    creditSaleEnabled: Boolean,
    onCreditSaleToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(BrandPrimarySoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = BrandPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "بيع آجل",
                    color = TextPrimaryColor
                )
                Text(
                    text = "استخدم هذا الخيار إذا كان العميل سيدفع لاحقًا",
                    color = TextSecondaryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            androidx.compose.material3.Switch(
                checked = creditSaleEnabled,
                onCheckedChange = onCreditSaleToggle,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun PaymentHintCard(message: String) {
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
                .padding(AppDimens.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    tint = BrandPrimary
                )
            }

            Text(
                text = message,
                color = TextSecondaryColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.12f),
        contentColor = tint,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.20f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AppDimens.Spacing.small, vertical = 4.dp),
            color = tint,
            maxLines = 1
        )
    }
}

private fun paymentIcon(method: PaymentMethod) = when (method.type) {
    PaymentMethodType.CASH -> Icons.Default.AttachMoney
    PaymentMethodType.BANK_TRANSFER -> Icons.Default.AccountBalance
    PaymentMethodType.WALLET -> Icons.Default.AccountBalanceWallet
    PaymentMethodType.POS -> Icons.Default.CreditCard
    PaymentMethodType.CHEQUE -> Icons.Default.ReceiptLong
    PaymentMethodType.OTHER -> Icons.Default.Payments
}