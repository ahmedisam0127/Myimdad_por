package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * مستوى واحد داخل سلسلة التعبئة.
 *
 * مثال:
 * طبلية -> 50 كرتونة
 * كرتونة -> 12 علبة
 * علبة -> 24 قطعة
 */
data class UnitLevel(
    val unit: UnitOfMeasure,
    val capacityToNext: Long
) {
    init {
        require(capacityToNext > 0) {
            "capacityToNext must be greater than zero"
        }
    }
}

/**
 * سلسلة التعبئة الخاصة بمنتج محدد.
 * الترتيب يكون من الأكبر إلى الأصغر.
 */
data class ProductUnitHierarchy(
    val productId: String,
    val levels: List<UnitLevel>
) {
    init {
        require(levels.isNotEmpty()) {
            "levels must not be empty"
        }

        require(levels.map { it.unit }.distinct().size == levels.size) {
            "Each unit must appear only once in the hierarchy"
        }

        require(levels.map { it.unit.dimension }.distinct().size == 1) {
            "All units in the hierarchy must have the same dimension"
        }
    }

    val smallestUnit: UnitOfMeasure
        get() = levels.last().unit

    val dimension: UnitOfMeasure.Dimension
        get() = levels.first().unit.dimension

    /**
     * المضاعف الإجمالي لهذا المستوى حتى أصغر وحدة.
     *
     * مثال:
     * طبلية -> كرتونة -> علبة -> قطعة
     * multiplier للطبلية = 50 * 12 * 24
     * multiplier للكرتونة = 12 * 24
     * multiplier للعلبة = 24
     * multiplier للقطعة = 1
     */
    fun multiplierToSmallest(levelIndex: Int): BigDecimal {
        require(levelIndex in levels.indices) {
            "levelIndex out of range"
        }

        return levels.drop(levelIndex + 1)
            .fold(BigDecimal.ONE) { acc, level ->
                acc.multiply(BigDecimal.valueOf(level.capacityToNext))
            }
    }

    fun indexOf(unit: UnitOfMeasure): Int {
        return levels.indexOfFirst { it.unit == unit }
    }

    fun contains(unit: UnitOfMeasure): Boolean {
        return indexOf(unit) != -1
    }
}

data class UnitBreakdown(
    val unit: UnitOfMeasure,
    val quantity: BigDecimal
)

/**
 * تفكيك وتجميع الكميات متعددة المستويات.
 */
object ProductUnitChainFormatter {

    /**
     * يفك الكمية المخزنة بأصغر وحدة إلى مستويات مفهومة.
     *
     * مثال:
     * 105 لتر -> 1 صندوق و 1 جركانة و 5 لتر
     */
    fun decompose(
        totalInSmallestUnit: BigDecimal,
        hierarchy: ProductUnitHierarchy
    ): List<UnitBreakdown> {
        require(totalInSmallestUnit >= BigDecimal.ZERO) {
            "totalInSmallestUnit cannot be negative"
        }

        var remaining = totalInSmallestUnit
        val result = mutableListOf<UnitBreakdown>()

        for (index in hierarchy.levels.indices) {
            val level = hierarchy.levels[index]
            val multiplier = hierarchy.multiplierToSmallest(index)

            val quantity = if (multiplier == BigDecimal.ONE) {
                remaining
            } else {
                remaining.divideToIntegralValue(multiplier)
            }

            val consumed = quantity.multiply(multiplier)
            remaining = remaining.subtract(consumed)

            if (quantity > BigDecimal.ZERO) {
                result.add(
                    UnitBreakdown(
                        unit = level.unit,
                        quantity = quantity.stripTrailingZeros()
                    )
                )
            }
        }

        return result
    }

    /**
     * تنسيق مباشر للعرض.
     */
    fun format(
        totalInSmallestUnit: BigDecimal,
        hierarchy: ProductUnitHierarchy
    ): String {
        val parts = decompose(totalInSmallestUnit, hierarchy)

        if (parts.isEmpty()) {
            return "0 ${hierarchy.smallestUnit.displayName}"
        }

        return parts.joinToString(" و ") { part ->
            "${formatQuantity(part.quantity)} ${part.unit.displayName}"
        }
    }

    /**
     * تنسيق مع اسم المنتج.
     *
     * مثال:
     * لديك 1 طبلية زيت طعام و 2 كرتونة و 5 قطعة
     */
    fun formatWithProductName(
        productName: String,
        totalInSmallestUnit: BigDecimal,
        hierarchy: ProductUnitHierarchy
    ): String {
        val parts = decompose(totalInSmallestUnit, hierarchy)

        if (parts.isEmpty()) {
            return "لديك 0 ${hierarchy.smallestUnit.displayName} من $productName"
        }

        val firstPart = parts.first()
        val remainingParts = parts.drop(1)

        val firstText = "${formatQuantity(firstPart.quantity)} ${firstPart.unit.displayName} $productName"

        return if (remainingParts.isEmpty()) {
            "لديك $firstText"
        } else {
            val restText = remainingParts.joinToString(" و ") {
                "${formatQuantity(it.quantity)} ${it.unit.displayName}"
            }
            "لديك $firstText و $restText"
        }
    }

    /**
     * تحويل كمية هرميّة مدخلة يدويًا إلى أصغر وحدة.
     *
     * مثال:
     * 1 طبلية + 2 كرتونة + 3 علبة + 4 قطعة
     * -> الناتج بالقطعة
     */
    fun compose(
        quantities: Map<UnitOfMeasure, BigDecimal>,
        hierarchy: ProductUnitHierarchy
    ): BigDecimal {
        val allowedUnits = hierarchy.levels.map { it.unit }.toSet()
        val unknownUnits = quantities.keys.filterNot { it in allowedUnits }

        require(unknownUnits.isEmpty()) {
            "Unknown units in quantities: ${unknownUnits.joinToString { it.name }}"
        }

        var total = BigDecimal.ZERO

        hierarchy.levels.forEachIndexed { index, level ->
            val qty = quantities[level.unit] ?: BigDecimal.ZERO

            require(qty >= BigDecimal.ZERO) {
                "Quantity for ${level.unit.name} cannot be negative"
            }

            val multiplier = hierarchy.multiplierToSmallest(index)
            total = total.add(qty.multiply(multiplier))
        }

        return total.stripTrailingZeros()
    }

    private fun formatQuantity(quantity: BigDecimal): String {
        return quantity.stripTrailingZeros().toPlainString()
    }
}