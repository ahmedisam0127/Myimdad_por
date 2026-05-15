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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.orZero
import com.myimdad_por.ui.theme.AppDimens
import com.myimdad_por.ui.theme.AppShapeTokens
import com.myimdad_por.ui.theme.BrandPrimary
import com.myimdad_por.ui.theme.BrandPrimarySoft
import com.myimdad_por.ui.theme.BrandPrimaryTint
import com.myimdad_por.ui.theme.ErrorColor
import com.myimdad_por.ui.theme.SuccessColor
import com.myimdad_por.ui.theme.TextPrimaryColor
import com.myimdad_por.ui.theme.TextSecondaryColor
import com.myimdad_por.ui.theme.WhiteColor
import java.math.BigDecimal

@Composable
fun SaleTransactionPanel(
    subtotalAmount: BigDecimal,
    totalAmount: BigDecimal,
    modifier: Modifier = Modifier,
    discountAmount: BigDecimal = BigDecimal.ZERO,
    taxAmount: BigDecimal = BigDecimal.ZERO,
    paidAmount: BigDecimal = BigDecimal.ZERO,
    remainingAmount: BigDecimal = BigDecimal.ZERO,
    customerName: String? = null,
    customerPhone: String? = null,
    invoiceNumber: String? = null,
    paymentMethodLabel: String = "نقدًا",
    creditSaleEnabled: Boolean = false,
    notes: String = "",
    notesMaxLength: Int = 500,
    enabled: Boolean = true,
    loading: Boolean = false,
    onNotesChange: (String) -> Unit = {},
    onConfirmSale: () -> Unit = {},
    onSaveDraft: () -> Unit = {},
    onClearTransaction: () -> Unit = {},
    onRestoreDefaults: () -> Unit = {}
) {
    val safeSubtotal = subtotalAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val safeDiscount = discountAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val safeTax = taxAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val safeTotal = totalAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val safePaid = paidAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val safeRemaining = remainingAmount.orZero().coerceAtLeast(BigDecimal.ZERO)
    val canConfirm = enabled && !loading && safeTotal > BigDecimal.ZERO
    val notesText = notes.take(notesMaxLength.coerceAtLeast(0))
    val scrollState = rememberScrollState()

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
                .verticalScroll(scrollState)
                .padding(AppDimens.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.large)
        ) {
            TransactionHeader(
                invoiceNumber = invoiceNumber,
                customerName = customerName,
                customerPhone = customerPhone,
                paymentMethodLabel = paymentMethodLabel,
                creditSaleEnabled = creditSaleEnabled
            )

            TransactionHero(
                totalAmount = safeTotal,
                remainingAmount = safeRemaining,
                paidAmount = safePaid
            )

            TransactionMetrics(
                subtotalAmount = safeSubtotal,
                discountAmount = safeDiscount,
                taxAmount = safeTax,
                paidAmount = safePaid
            )

            TransactionNoteField(
                value = notesText,
                onValueChange = {
                    if (it.length <= notesMaxLength) onNotesChange(it)
                },
                enabled = enabled && !loading,
                notesMaxLength = notesMaxLength
            )

            TransactionActionRow(
                enabled = enabled,
                loading = loading,
                canConfirm = canConfirm,
                onConfirmSale = onConfirmSale,
                onSaveDraft = onSaveDraft,
                onClearTransaction = onClearTransaction,
                onRestoreDefaults = onRestoreDefaults
            )
        }
    }
}

@Composable
private fun TransactionHeader(
    invoiceNumber: String?,
    customerName: String?,
    customerPhone: String?,
    paymentMethodLabel: String,
    creditSaleEnabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
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
                Surface(
                    shape = CircleShape,
                    color = BrandPrimarySoft,
                    contentColor = BrandPrimary,
                    border = BorderStroke(1.dp, BrandPrimaryTint)
                ) {
                    Icon(
                        imageVector = Icons.Default.PriceCheck,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "لوحة المعاملة",
                        color = TextPrimaryColor,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ملخص منظم قبل اعتماد البيع",
                        color = TextSecondaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            TransactionPill(
                text = if (creditSaleEnabled) "آجل" else "فوري",
                icon = if (creditSaleEnabled) Icons.Default.Today else Icons.Default.CheckCircle,
                tint = if (creditSaleEnabled) SuccessColor else BrandPrimary
            )
        }

        if (!invoiceNumber.isNullOrBlank() || !customerName.isNullOrBlank() || !customerPhone.isNullOrBlank()) {
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
                        .padding(AppDimens.Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
                ) {
                    DetailLine(
                        label = "رقم الفاتورة",
                        value = invoiceNumber?.takeIf { it.isNotBlank() } ?: "غير محدد",
                        icon = Icons.Default.ReceiptLong,
                        accent = BrandPrimary
                    )
                    DetailLine(
                        label = "العميل",
                        value = customerName?.takeIf { it.isNotBlank() } ?: "بدون عميل",
                        icon = Icons.Default.AccountBalanceWallet,
                        accent = SuccessColor
                    )
                    DetailLine(
                        label = "الهاتف",
                        value = customerPhone?.takeIf { it.isNotBlank() } ?: "غير متوفر",
                        icon = Icons.Default.CheckCircle,
                        accent = TextSecondaryColor
                    )
                    DetailLine(
                        label = "طريقة الدفع",
                        value = paymentMethodLabel,
                        icon = Icons.Default.Savings,
                        accent = BrandPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionHero(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.medium)
        ) {
            Surface(
                shape = CircleShape,
                color = BrandPrimary.copy(alpha = 0.12f),
                contentColor = BrandPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCartCheckout,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "الإجمالي النهائي",
                    color = TextSecondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = CurrencyFormatter.formatSDG(totalAmount),
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransactionMiniStat(
                    label = "المدفوع",
                    value = CurrencyFormatter.formatSDG(paidAmount),
                    tint = SuccessColor
                )
                TransactionMiniStat(
                    label = "المتبقي",
                    value = CurrencyFormatter.formatSDG(remainingAmount),
                    tint = if (remainingAmount > BigDecimal.ZERO) ErrorColor else BrandPrimary
                )
            }
        }
    }
}

@Composable
private fun TransactionMetrics(
    subtotalAmount: BigDecimal,
    discountAmount: BigDecimal,
    taxAmount: BigDecimal,
    paidAmount: BigDecimal
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppShapeTokens.filledCard,
        color = WhiteColor,
        contentColor = TextPrimaryColor,
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            MetricLine(
                label = "الإجمالي الفرعي",
                value = CurrencyFormatter.formatSDG(subtotalAmount),
                accent = TextSecondaryColor,
                icon = Icons.Default.PriceCheck
            )
            MetricLine(
                label = "الخصم",
                value = CurrencyFormatter.formatSDG(discountAmount),
                accent = ErrorColor,
                icon = Icons.Default.DeleteOutline
            )
            MetricLine(
                label = "الضريبة",
                value = CurrencyFormatter.formatSDG(taxAmount),
                accent = SuccessColor,
                icon = Icons.Default.Savings
            )
            MetricLine(
                label = "المدفوع",
                value = CurrencyFormatter.formatSDG(paidAmount),
                accent = BrandPrimary,
                icon = Icons.Default.AccountBalanceWallet
            )
        }
    }
}

@Composable
private fun TransactionNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    notesMaxLength: Int
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        maxLines = 4,
        label = { Text(text = "ملاحظات العملية") },
        placeholder = { Text(text = "أضف أي ملاحظات مهمة حول البيع أو التسليم") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Notes,
                contentDescription = null,
                tint = BrandPrimary
            )
        },
        supportingText = {
            Text(
                text = "${value.length.coerceAtMost(notesMaxLength)}/$notesMaxLength",
                color = TextSecondaryColor
            )
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun TransactionActionRow(
    enabled: Boolean,
    loading: Boolean,
    canConfirm: Boolean,
    onConfirmSale: () -> Unit,
    onSaveDraft: () -> Unit,
    onClearTransaction: () -> Unit,
    onRestoreDefaults: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            OutlineActionButton(
                modifier = Modifier.weight(1f),
                text = "تفريغ",
                icon = Icons.Default.DeleteOutline,
                enabled = enabled && !loading,
                onClick = onClearTransaction
            )
            OutlineActionButton(
                modifier = Modifier.weight(1f),
                text = "استعادة",
                icon = Icons.Default.Restore,
                enabled = enabled && !loading,
                onClick = onRestoreDefaults
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing.small)
        ) {
            FilledActionButton(
                modifier = Modifier.weight(1f),
                text = "مسودة",
                icon = Icons.Default.ReceiptLong,
                enabled = enabled && !loading,
                containerColor = BrandPrimarySoft,
                contentColor = BrandPrimary,
                onClick = onSaveDraft
            )
            FilledActionButton(
                modifier = Modifier.weight(1.2f),
                text = if (loading) "جارٍ الاعتماد..." else "اعتماد البيع",
                icon = Icons.Default.ShoppingCartCheckout,
                enabled = canConfirm,
                containerColor = BrandPrimary,
                contentColor = WhiteColor,
                onClick = onConfirmSale
            )
        }
    }
}

@Composable
private fun FilledActionButton(
    modifier: Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = contentColor.copy(alpha = 0.65f)
        ),
        border = if (containerColor == BrandPrimarySoft) BorderStroke(1.dp, BrandPrimaryTint) else null
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OutlineActionButton(
    modifier: Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BrandPrimary,
            disabledContentColor = BrandPrimary.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, BrandPrimaryTint)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TransactionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.10f),
        contentColor = tint,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(
                text = text,
                color = tint,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MetricLine(
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
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
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            color = TextPrimaryColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TransactionMiniStat(
    label: String,
    value: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            color = TextSecondaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = tint,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
