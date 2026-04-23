package com.myimdad_por.core.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.ParsePosition
import java.util.Currency
import java.util.Locale

/**
 * CurrencyFormatter مخصص للسودان (SDG)
 *
 * Production-Ready:
 * - دقة مالية باستخدام BigDecimal
 * - دعم الأرقام العربية (١٢٣) والإنجليزية (123)
 * - مقاوم للأخطاء في parsing
 * - مناسب للتضخم (أرقام كبيرة)
 */
object CurrencyFormatter {

    private val DEFAULT_LOCALE: Locale = Locale("ar", "SD")
    private val FALLBACK_LOCALE: Locale = Locale.ENGLISH

    private val SDG: Currency = Currency.getInstance("SDG")

    private const val DEFAULT_FRACTION = 2

    /**
     * تنسيق العملة (SDG بشكل افتراضي)
     */
    fun formatSDG(
        amount: BigDecimal?,
        showSymbol: Boolean = true,
        locale: Locale = DEFAULT_LOCALE
    ): String {
        return format(
            amount = amount,
            currency = SDG,
            locale = locale,
            showSymbol = showSymbol
        )
    }

    /**
     * تنسيق عام لأي عملة
     */
    fun format(
        amount: BigDecimal?,
        currency: Currency,
        locale: Locale = DEFAULT_LOCALE,
        showSymbol: Boolean = true,
        fractionDigits: Int = currency.defaultFractionDigits.coerceAtLeast(DEFAULT_FRACTION)
    ): String {

        if (amount == null) return Constants.Ui.EMPTY_TEXT

        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
            maximumFractionDigits = fractionDigits
            minimumFractionDigits = fractionDigits
            isGroupingUsed = true
        }

        val formatted = formatter.format(amount)

        return if (showSymbol) {
            formatted
        } else {
            formatted
                .replace(currency.getSymbol(locale), "")
                .trim()
        }
    }

    /**
     * تنسيق رقم فقط (بدون عملة)
     * مهم في الجداول والتقارير
     */
    fun formatPlain(
        amount: BigDecimal?,
        locale: Locale = DEFAULT_LOCALE,
        fractionDigits: Int = DEFAULT_FRACTION
    ): String {

        if (amount == null) return Constants.Ui.EMPTY_TEXT

        val formatter = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = fractionDigits
            minimumFractionDigits = 0
            isGroupingUsed = true
        }

        return formatter.format(amount)
    }

    /**
     * Parsing قوي:
     * - يدعم "1,000"
     * - يدعم "١٠٠٠"
     * - يدعم "ج.س 1,000"
     */
    fun parse(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return null

        val cleaned = value
            .replace("ج.س", "")
            .replace("SDG", "")
            .trim()

        return parseWithLocale(cleaned, DEFAULT_LOCALE)
            ?: parseWithLocale(cleaned, FALLBACK_LOCALE)
    }

    private fun parseWithLocale(
        value: String,
        locale: Locale
    ): BigDecimal? {
        return try {
            val formatter = NumberFormat.getNumberInstance(locale)
            val position = ParsePosition(0)
            val number = formatter.parse(value, position)

            if (position.index != value.length) return null

            number?.toString()?.toBigDecimalOrNull()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * تقريب بنكي (Banker's Rounding)
     */
    fun round(
        amount: BigDecimal,
        scale: Int = DEFAULT_FRACTION
    ): BigDecimal {
        return amount.setScale(scale, RoundingMode.HALF_EVEN)
    }

    /**
     * تحويل آمن من Double (للاستخدامات غير الحرجة فقط)
     */
    fun fromDouble(value: Double?): BigDecimal? {
        return value?.toBigDecimal()
    }

    /**
     * صفر مالي
     */
    fun zero(): BigDecimal = BigDecimal.ZERO.setScale(DEFAULT_FRACTION)

    /**
     * تحقق إذا القيمة صفر
     */
    fun isZero(amount: BigDecimal?): Boolean {
        return amount?.compareTo(BigDecimal.ZERO) == 0
    }
}