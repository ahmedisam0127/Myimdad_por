package com.myimdad_por.domain.usecase.Product

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.RoundingPolicy
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal
import javax.inject.Inject
class ConvertProductUnitUseCase @Inject constructor( ){

    operator fun invoke(
        product: Product?,
        quantity: BigDecimal?,
        fromUnit: UnitOfMeasure? = null,
        toUnit: UnitOfMeasure? = null,
        roundingPolicy: RoundingPolicy = RoundingPolicy.STRICT
    ): ConversionResult {
        if (product == null) {
            return ConversionResult.failure("المنتج غير موجود")
        }

        val safeQuantity = quantity ?: return ConversionResult.failure("الكمية غير موجودة")
        if (safeQuantity < BigDecimal.ZERO) {
            return ConversionResult.failure("الكمية لا يمكن أن تكون سالبة")
        }

        val sourceUnit = fromUnit ?: product.largeUnit
        val targetUnit = toUnit ?: product.smallUnit

        if (sourceUnit == targetUnit) {
            return ConversionResult.success(
                quantity = safeQuantity,
                fromUnit = sourceUnit,
                toUnit = targetUnit
            )
        }

        if (sourceUnit.dimension != targetUnit.dimension) {
            return ConversionResult.failure("لا يمكن التحويل بين وحدتين من نوعين مختلفين")
        }

        val conversion = product.getConversion()

        return try {
            when {
                sourceUnit == conversion.fromUnit && targetUnit == conversion.toUnit -> {
                    ConversionResult.success(
                        quantity = conversion.convert(safeQuantity, roundingPolicy),
                        fromUnit = sourceUnit,
                        toUnit = targetUnit
                    )
                }

                sourceUnit == conversion.toUnit && targetUnit == conversion.fromUnit -> {
                    ConversionResult.success(
                        quantity = conversion.inverseConvert(safeQuantity, roundingPolicy),
                        fromUnit = sourceUnit,
                        toUnit = targetUnit
                    )
                }

                else -> {
                    ConversionResult.failure(
                        "التحويل بين هاتين الوحدتين غير مدعوم لهذا المنتج"
                    )
                }
            }
        } catch (e: IllegalArgumentException) {
            ConversionResult.failure(e.message ?: "فشل التحويل")
        } catch (e: ArithmeticException) {
            ConversionResult.failure(e.message ?: "فشل التحويل الحسابي")
        }
    }

    fun toSmallUnit(
        product: Product?,
        largeQuantity: BigDecimal?,
        roundingPolicy: RoundingPolicy = RoundingPolicy.STRICT
    ): ConversionResult {
        return invoke(
            product = product,
            quantity = largeQuantity,
            fromUnit = product?.largeUnit,
            toUnit = product?.smallUnit,
            roundingPolicy = roundingPolicy
        )
    }

    fun toLargeUnit(
        product: Product?,
        smallQuantity: BigDecimal?,
        roundingPolicy: RoundingPolicy = RoundingPolicy.STRICT
    ): ConversionResult {
        return invoke(
            product = product,
            quantity = smallQuantity,
            fromUnit = product?.smallUnit,
            toUnit = product?.largeUnit,
            roundingPolicy = roundingPolicy
        )
    }

    data class ConversionResult(
        val isSuccess: Boolean,
        val quantity: BigDecimal? = null,
        val fromUnit: UnitOfMeasure? = null,
        val toUnit: UnitOfMeasure? = null,
        val errorMessage: String? = null
    ) {
        companion object {
            fun success(
                quantity: BigDecimal,
                fromUnit: UnitOfMeasure,
                toUnit: UnitOfMeasure
            ): ConversionResult {
                return ConversionResult(
                    isSuccess = true,
                    quantity = quantity,
                    fromUnit = fromUnit,
                    toUnit = toUnit,
                    errorMessage = null
                )
            }

            fun failure(message: String): ConversionResult {
                return ConversionResult(
                    isSuccess = false,
                    quantity = null,
                    fromUnit = null,
                    toUnit = null,
                    errorMessage = message
                )
            }
        }
    }
}