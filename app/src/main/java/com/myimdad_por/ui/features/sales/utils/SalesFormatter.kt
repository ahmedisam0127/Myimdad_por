package com.myimdad_por.ui.features.sales.utils

import com.myimdad_por.core.utils.CurrencyFormatter
import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.core.utils.orZero
import com.myimdad_por.core.utils.roundTo
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentStatus
import com.myimdad_por.domain.model.SaleInvoiceStatus
import com.myimdad_por.domain.model.UnitOfMeasure
import com.myimdad_por.ui.features.sales.SalesConstants
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

object SalesFormatter {

    private const val MONEY_SCALE = 2
    private const val PERCENT_SCALE = 2

    private val ZERO: BigDecimal =
        BigDecimal.ZERO.setScale(
            MONEY_SCALE,
            RoundingMode.HALF_UP
        )

    fun formatMoney(
        amount: BigDecimal?,
        withSymbol: Boolean = true
    ): String {
        return CurrencyFormatter.formatSDG(
            amount = amount
                .orZero()
                .roundMoney(),
            showSymbol = withSymbol
        )
    }

    fun formatPlainNumber(
        value: BigDecimal?,
        fractionDigits: Int = MONEY_SCALE
    ): String {
        return CurrencyFormatter.formatPlain(
            amount = value
                .orZero()
                .setScale(
                    fractionDigits,
                    RoundingMode.HALF_UP
                ),
            fractionDigits = fractionDigits
        )
    }

    fun formatQuantity(
        quantity: BigDecimal?,
        unit: UnitOfMeasure
    ): String {

        val scale = if (unit.isDecimalAllowed) {
            MONEY_SCALE
        } else {
            0
        }

        val formattedQuantity = formatPlainNumber(
            value = quantity,
            fractionDigits = scale
        )

        return "$formattedQuantity ${unit.displayName}"
    }

    fun formatUnit(
        unit: UnitOfMeasure
    ): String {
        return unit.displayName
    }

    fun formatUnitSymbol(
        unit: UnitOfMeasure
    ): String {
        return unit.symbol
    }

    fun formatPercentage(
        percentage: BigDecimal?
    ): String {
        return buildString {

            append(
                formatPlainNumber(
                    value = percentage.orZero(),
                    fractionDigits = PERCENT_SCALE
                )
            )

            append("%")
        }
    }

    fun formatInvoiceStatus(
        status: SaleInvoiceStatus
    ): String {

        return when (status) {

            SaleInvoiceStatus.DRAFT ->
                "مسودة"

            SaleInvoiceStatus.OPEN ->
                "مفتوحة"

            SaleInvoiceStatus.ISSUED ->
                "صادرة"

            SaleInvoiceStatus.PAID ->
                "مدفوعة"

            SaleInvoiceStatus.PARTIALLY_PAID ->
                "مدفوعة جزئياً"

            SaleInvoiceStatus.OVERDUE ->
                "متأخرة"

            SaleInvoiceStatus.CANCELLED ->
                "ملغية"

            SaleInvoiceStatus.VOID ->
                "باطلة"

            SaleInvoiceStatus.EXPIRED ->
                "منتهية"

            else ->
                "غير معروفة"
        }
    }

    fun formatPaymentStatus(
        status: PaymentStatus
    ): String {

        return when (status) {

            PaymentStatus.PENDING ->
                "معلق"

            PaymentStatus.PROCESSING ->
                "قيد المعالجة"

            PaymentStatus.REQUIRES_ACTION ->
                "يتطلب إجراء"

            PaymentStatus.AUTHORIZED ->
                "مصرح"

            PaymentStatus.PAID ->
                "مدفوع"

            PaymentStatus.PARTIALLY_PAID ->
                "مدفوع جزئياً"

            PaymentStatus.FAILED ->
                "فشلت العملية"

            PaymentStatus.CANCELED ->
                "ملغي"

            PaymentStatus.REFUNDED ->
                "تم الاسترجاع"

            PaymentStatus.PARTIALLY_REFUNDED ->
                "استرجاع جزئي"

            else ->
                "غير معروفة"
        }
    }

    fun formatPaymentMethod(
        paymentMethod: PaymentMethod?
    ): String {

        return paymentMethod
            ?.name
            ?.replace("_", " ")
            ?.lowercase()
            ?.replaceFirstChar {
                it.titlecase(Locale.getDefault())
            }
            ?: SalesConstants.Ui.ACTION_PAY
    }

    fun formatDateTime(
        value: LocalDateTime?
    ): String {

        if (value == null) {
            return "-"
        }

        return DateTimeUtils.formatForDisplay(value)
    }

    fun formatDate(
        value: LocalDate?
    ): String {

        if (value == null) {
            return "-"
        }

        return DateTimeUtils.formatDateForDisplay(value)
    }

    fun formatTime(
        value: LocalDateTime?
    ): String {

        if (value == null) {
            return "-"
        }

        return DateTimeUtils.formatTime(value)
    }

    fun formatRemainingAmount(
        totalAmount: BigDecimal?,
        paidAmount: BigDecimal?
    ): String {

        val remaining = totalAmount
            .orZero()
            .subtract(
                paidAmount.orZero()
            )
            .roundMoney()

        return formatMoney(remaining)
    }

    fun formatSubtotal(
        quantity: BigDecimal?,
        unitPrice: BigDecimal?
    ): String {

        val subtotal = quantity
            .orZero()
            .multiply(
                unitPrice.orZero()
            )
            .roundMoney()

        return formatMoney(subtotal)
    }

    fun formatCompactProductName(
        name: String?,
        maxLength: Int = 40
    ): String {

        val normalized = name
            ?.trim()
            .orEmpty()

        if (normalized.length <= maxLength) {
            return normalized
        }

        return normalized
            .take(maxLength)
            .trimEnd() + "..."
    }

    fun buildInvoiceTitle(
        invoiceNumber: String?,
        isDraft: Boolean
    ): String {

        val number = invoiceNumber
            ?.trim()
            .takeUnless {
                it.isNullOrBlank()
            }
            ?: "---"

        return if (isDraft) {
            "${SalesConstants.Invoice.WATERMARK_DRAFT} #$number"
        } else {
            "فاتورة #$number"
        }
    }

    fun buildCustomerDisplayName(
        customerName: String?,
        customerPhone: String?
    ): String {

        val name = customerName
            ?.trim()
            .orEmpty()

        val phone = customerPhone
            ?.trim()
            .orEmpty()

        return when {

            name.isNotBlank() &&
                phone.isNotBlank() -> {
                "$name • $phone"
            }

            name.isNotBlank() ->
                name

            phone.isNotBlank() ->
                phone

            else ->
                "عميل نقدي"
        }
    }

    fun buildItemSummary(
        quantity: BigDecimal?,
        unit: UnitOfMeasure,
        unitPrice: BigDecimal?
    ): String {

        return buildString {

            append(
                formatQuantity(
                    quantity = quantity,
                    unit = unit
                )
            )

            append(" × ")

            append(
                formatMoney(
                    amount = unitPrice
                )
            )
        }
    }

    fun normalizeSearchQuery(
        query: String?
    ): String {

        return query
            ?.trim()
            ?.lowercase(Locale.getDefault())
            .orEmpty()
    }

    fun sanitizeNote(
        note: String?,
        maxLength: Int = SalesConstants.Ui.MAX_NOTE_LENGTH
    ): String? {

        val normalized = note
            ?.trim()
            ?.replace(
                Regex("\\s+"),
                " "
            )
            .orEmpty()

        if (normalized.isBlank()) {
            return null
        }

        return normalized.take(maxLength)
    }

    private fun BigDecimal.roundMoney(): BigDecimal {

        return roundTo(
            scale = MONEY_SCALE,
            roundingMode = RoundingMode.HALF_UP
        )
    }

    fun zeroMoney(): BigDecimal = ZERO
}