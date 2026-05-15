package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * قاعدة تحويل بين وحدتين من نفس البُعد.
 *
 * مثال:
 * 1 كرتونة = 24 قطعة
 * 1 لتر = 1000 مل
 *
 * ملاحظة:
 * تم التحويل إلى BigDecimal لتفادي أخطاء Double.
 */
data class UnitConversion(
    val fromUnit: UnitOfMeasure,
    val toUnit: UnitOfMeasure,
    val multiplier: BigDecimal
) {

    init {
        require(fromUnit != toUnit) {
            "fromUnit and toUnit must be different"
        }

        require(multiplier > BigDecimal.ZERO) {
            "multiplier must be a positive number"
        }

        require(fromUnit.dimension == toUnit.dimension) {
            "fromUnit and toUnit must have the same dimension"
        }
    }

    /**
     * تحويل من [fromUnit] إلى [toUnit]
     */
    fun convert(
        quantity: BigDecimal,
        rounding: RoundingPolicy = RoundingPolicy.STRICT
    ): BigDecimal {
        validateQuantity(quantity, fromUnit)

        val raw = quantity.multiply(multiplier)
        return applyRounding(raw, toUnit, rounding)
    }

    /**
     * التحويل العكسي
     */
    fun inverseConvert(
        quantity: BigDecimal,
        rounding: RoundingPolicy = RoundingPolicy.STRICT
    ): BigDecimal {
        validateQuantity(quantity, toUnit)

        val raw = quantity.divide(
            multiplier,
            DEFAULT_DIVISION_SCALE,
            RoundingMode.HALF_UP
        )

        return applyRounding(raw, fromUnit, rounding)
    }

    /**
     * هل هذه القاعدة عكس قاعدة أخرى؟
     */
    fun isReverseOf(other: UnitConversion): Boolean {
        return fromUnit == other.toUnit &&
            toUnit == other.fromUnit &&
            multiplier.multiply(other.multiplier)
                .subtract(BigDecimal.ONE)
                .abs() <= EPSILON
    }

    private fun validateQuantity(quantity: BigDecimal, unit: UnitOfMeasure) {
        require(quantity >= BigDecimal.ZERO) {
            "quantity cannot be negative"
        }

        if (!unit.isDecimalAllowed) {
            require(!hasFraction(quantity)) {
                "Unit ${unit.name} does not allow decimal quantities"
            }
        }
    }

    private fun applyRounding(
        value: BigDecimal,
        unit: UnitOfMeasure,
        policy: RoundingPolicy
    ): BigDecimal {
        if (unit.isDecimalAllowed) return value.stripTrailingZeros()

        return when (policy) {
            RoundingPolicy.STRICT -> {
                require(!hasFraction(value)) {
                    "Result $value is not allowed for unit ${unit.name}"
                }
                value.setScale(0, RoundingMode.UNNECESSARY)
            }

            RoundingPolicy.FLOOR -> value.setScale(0, RoundingMode.FLOOR)
            RoundingPolicy.CEIL -> value.setScale(0, RoundingMode.CEILING)
            RoundingPolicy.ROUND -> value.setScale(0, RoundingMode.HALF_UP)
        }
    }

    private fun hasFraction(value: BigDecimal): Boolean {
        return value.stripTrailingZeros().scale() > 0
    }

    companion object {
        private val EPSILON = BigDecimal("0.000000001")
        private const val DEFAULT_DIVISION_SCALE = 12
    }
}

enum class RoundingPolicy {
    STRICT,
    FLOOR,
    CEIL,
    ROUND
}