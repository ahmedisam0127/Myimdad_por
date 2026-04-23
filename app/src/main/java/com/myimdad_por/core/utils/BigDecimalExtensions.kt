package com.myimdad_por.core.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private const val DEFAULT_SCALE = 2
private const val PERCENT_INTERMEDIATE_SCALE = 4

private val ZERO: BigDecimal = BigDecimal.ZERO
private val ONE_HUNDRED: BigDecimal = BigDecimal("100")


fun BigDecimal?.orZero(): BigDecimal = this ?: ZERO


fun BigDecimal.isZero(): Boolean = compareTo(ZERO) == 0
fun BigDecimal.isNotZero(): Boolean = compareTo(ZERO) != 0
fun BigDecimal.isPositive(): Boolean = compareTo(ZERO) > 0
fun BigDecimal.isNegative(): Boolean = compareTo(ZERO) < 0

fun BigDecimal.isGreaterThan(other: BigDecimal): Boolean = compareTo(other) > 0
fun BigDecimal.isLessThan(other: BigDecimal): Boolean = compareTo(other) < 0
fun BigDecimal.isAtLeast(other: BigDecimal): Boolean = compareTo(other) >= 0
fun BigDecimal.isAtMost(other: BigDecimal): Boolean = compareTo(other) <= 0


fun BigDecimal.absScaled(scale: Int = DEFAULT_SCALE): BigDecimal =
    abs().setScale(scale, RoundingMode.HALF_UP)

fun BigDecimal.roundTo(
    scale: Int = DEFAULT_SCALE,
    roundingMode: RoundingMode = RoundingMode.HALF_UP
): BigDecimal = setScale(scale, roundingMode)

fun BigDecimal.floorTo(scale: Int = DEFAULT_SCALE): BigDecimal =
    setScale(scale, RoundingMode.DOWN)

fun BigDecimal.ceilTo(scale: Int = DEFAULT_SCALE): BigDecimal =
    setScale(scale, RoundingMode.UP)


fun BigDecimal.safeDivide(
    divisor: BigDecimal?,
    scale: Int = DEFAULT_SCALE,
    roundingMode: RoundingMode = RoundingMode.HALF_UP
): BigDecimal {
    val safeDivisor = divisor.orZero()
    if (safeDivisor.isZero()) {
        return ZERO.setScale(scale, roundingMode)
    }
    return divide(safeDivisor, scale, roundingMode)
}

fun BigDecimal.plusOrZero(other: BigDecimal?): BigDecimal = add(other.orZero())
fun BigDecimal.minusOrZero(other: BigDecimal?): BigDecimal = subtract(other.orZero())
fun BigDecimal.timesOrZero(other: BigDecimal?): BigDecimal = multiply(other.orZero())

fun BigDecimal.minWith(other: BigDecimal): BigDecimal = if (this <= other) this else other
fun BigDecimal.maxWith(other: BigDecimal): BigDecimal = if (this >= other) this else other


fun BigDecimal.percentOf(
    total: BigDecimal?,
    scale: Int = DEFAULT_SCALE
): BigDecimal {
    val safeTotal = total.orZero()
    if (safeTotal.isZero()) {
        return ZERO.setScale(scale, RoundingMode.HALF_UP)
    }

    return multiply(ONE_HUNDRED)
        .divide(safeTotal, scale + PERCENT_INTERMEDIATE_SCALE, RoundingMode.HALF_UP)
        .setScale(scale, RoundingMode.HALF_UP)
}

fun BigDecimal.percentageOf(
    base: BigDecimal?,
    scale: Int = DEFAULT_SCALE
): BigDecimal = percentOf(base, scale)


fun BigDecimal.toPlainScaledString(scale: Int = DEFAULT_SCALE): String =
    roundTo(scale).toPlainString()

fun BigDecimal.toFormattedNumberString(
    locale: Locale = Locale.getDefault(),
    scale: Int = DEFAULT_SCALE
): String {
    val format = NumberFormat.getNumberInstance(locale)
    format.minimumFractionDigits = scale
    format.maximumFractionDigits = scale
    return format.format(roundTo(scale))
}

fun BigDecimal.toCurrencyString(
    locale: Locale = Locale.getDefault(),
    currency: Currency? = null,
    scale: Int = DEFAULT_SCALE
): String {
    val format = NumberFormat.getCurrencyInstance(locale)
    if (currency != null) {
        format.currency = currency
    }
    format.minimumFractionDigits = scale
    format.maximumFractionDigits = scale
    return format.format(roundTo(scale))
}


fun String?.toBigDecimalOrZero(): BigDecimal {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return ZERO

    return try {
        BigDecimal(raw)
    } catch (_: Exception) {
        ZERO
    }
}

fun Int.toBigDecimalValue(): BigDecimal = BigDecimal.valueOf(toLong())

fun Long.toBigDecimalValue(): BigDecimal = BigDecimal.valueOf(this)

fun Double.toBigDecimalValue(scale: Int = DEFAULT_SCALE): BigDecimal =
    BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP)

fun Float.toBigDecimalValue(scale: Int = DEFAULT_SCALE): BigDecimal =
    BigDecimal.valueOf(toDouble()).setScale(scale, RoundingMode.HALF_UP)

fun Number.toBigDecimalValue(scale: Int = DEFAULT_SCALE): BigDecimal {
    return when (this) {
        is BigDecimal -> setScale(scale, RoundingMode.HALF_UP)
        is Int -> BigDecimal.valueOf(toLong())
        is Long -> BigDecimal.valueOf(this)
        is Double -> BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP)
        is Float -> BigDecimal.valueOf(toDouble()).setScale(scale, RoundingMode.HALF_UP)
        is Short -> BigDecimal.valueOf(toLong())
        is Byte -> BigDecimal.valueOf(toLong())
        else -> toString().toBigDecimalOrZero().setScale(scale, RoundingMode.HALF_UP)
    }
}