package com.myimdad_por.domain.model

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

/**
 * قاعدة تحويل بين وحدتين من نفس البُعد.
 *
 * مثال:
 * 1 كرتونة = 24 قطعة
 * 1 لتر = 1000 مل
 */
data class UnitConversion(
    val fromUnit: UnitOfMeasure,
    val toUnit: UnitOfMeasure,
    val multiplier: Double
) {

    init {
        require(fromUnit != toUnit) {
            "fromUnit and toUnit must be different"
        }

        require(multiplier.isFinite() && multiplier > 0.0) {
            "multiplier must be a positive finite number"
        }

        require(fromUnit.dimension == toUnit.dimension) {
            "fromUnit and toUnit must have the same dimension"
        }
    }

    /**
     * تحويل من [fromUnit] إلى [toUnit]
     */
    fun convert(
        quantity: Double,
        rounding: RoundingPolicy = RoundingPolicy.STRICT
    ): Double {
        validateQuantity(quantity, fromUnit)

        val raw = quantity * multiplier
        return applyRounding(raw, toUnit, rounding)
    }

    /**
     * التحويل العكسي
     */
    fun inverseConvert(
        quantity: Double,
        rounding: RoundingPolicy = RoundingPolicy.STRICT
    ): Double {
        validateQuantity(quantity, toUnit)

        val raw = quantity / multiplier
        return applyRounding(raw, fromUnit, rounding)
    }

    /**
     * هل هذه القاعدة عكس قاعدة أخرى؟
     */
    fun isReverseOf(other: UnitConversion): Boolean {
        return fromUnit == other.toUnit &&
            toUnit == other.fromUnit &&
            abs(multiplier * other.multiplier - 1.0) < EPSILON
    }

    private fun validateQuantity(quantity: Double, unit: UnitOfMeasure) {
        require(quantity.isFinite()) {
            "quantity must be finite"
        }

        require(quantity >= 0) {
            "quantity cannot be negative"
        }

        if (!unit.isDecimalAllowed) {
            require(quantity % 1.0 == 0.0) {
                "Unit ${unit.name} does not allow decimal quantities"
            }
        }
    }

    private fun applyRounding(
        value: Double,
        unit: UnitOfMeasure,
        policy: RoundingPolicy
    ): Double {
        if (unit.isDecimalAllowed) return value

        val hasFraction = value % 1.0 != 0.0

        return when (policy) {
            RoundingPolicy.STRICT -> {
                require(!hasFraction) {
                    "Result $value is not allowed for unit ${unit.name}"
                }
                value
            }

            RoundingPolicy.FLOOR -> floor(value)
            RoundingPolicy.CEIL -> ceil(value)
            RoundingPolicy.ROUND -> round(value)
        }
    }

    companion object {
        private const val EPSILON = 1e-9
    }
}

enum class RoundingPolicy {
    STRICT,
    FLOOR,
    CEIL,
    ROUND
}