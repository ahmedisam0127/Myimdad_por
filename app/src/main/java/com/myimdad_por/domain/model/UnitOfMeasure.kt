package com.myimdad_por.domain.model

enum class UnitOfMeasure(
    val displayName: String,
    val symbol: String,
    val category: Category,
    val dimension: Dimension,
    val order: Int,
    val isDecimalAllowed: Boolean = false,
    val aliases: Set<String> = emptySet()
) {

    TON(
        displayName = "طن",
        symbol = "طن",
        category = Category.LARGE,
        dimension = Dimension.MASS,
        order = 1,
        isDecimalAllowed = true,
        aliases = setOf("طن", "ton", "tons")
    ),
    QUINTAL(
        displayName = "قنطار",
        symbol = "قنطار",
        category = Category.LARGE,
        dimension = Dimension.MASS,
        order = 2,
        isDecimalAllowed = true,
        aliases = setOf("قنطار", "quintal", "q")
    ),
    SHAWL(
        displayName = "شوال",
        symbol = "شوال",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 3,
        isDecimalAllowed = false,
        aliases = setOf("شوال", "shawl", "sack")
    ),
    CARTON(
        displayName = "كرتونة",
        symbol = "كرتونة",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 4,
        isDecimalAllowed = false,
        aliases = setOf("كرتونة", "carton", "ctn")
    ),
    DOZEN(
        displayName = "دستة",
        symbol = "دستة",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 5,
        isDecimalAllowed = false,
        aliases = setOf("دستة", "dozen")
    ),
    GROSS(
        displayName = "جروس",
        symbol = "جروس",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 6,
        isDecimalAllowed = false,
        aliases = setOf("جروس", "gross")
    ),
    PACK(
        displayName = "باكيت",
        symbol = "باكيت",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 7,
        isDecimalAllowed = false,
        aliases = setOf("باكيت", "pack", "pkt")
    ),
    BUNDLE(
        displayName = "ربطة",
        symbol = "ربطة",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 8,
        isDecimalAllowed = false,
        aliases = setOf("ربطة", "bundle")
    ),
    CRATE(
        displayName = "صندوق",
        symbol = "صندوق",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 9,
        isDecimalAllowed = false,
        aliases = setOf("صندوق", "crate")
    ),
    TRAY(
        displayName = "صينية",
        symbol = "صينية",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 10,
        isDecimalAllowed = false,
        aliases = setOf("صينية", "tray")
    ),
    TIN(
        displayName = "صفيحة",
        symbol = "صفيحة",
        category = Category.LARGE,
        dimension = Dimension.COUNT,
        order = 11,
        isDecimalAllowed = false,
        aliases = setOf("صفيحة", "tin")
    ),
    JERRICAN(
        displayName = "جركانة",
        symbol = "جركانة",
        category = Category.LARGE,
        dimension = Dimension.VOLUME,
        order = 12,
        isDecimalAllowed = true,
        aliases = setOf("جركانة", "jerrican", "jerrycan")
    ),
    BARREL(
        displayName = "برميل",
        symbol = "برميل",
        category = Category.LARGE,
        dimension = Dimension.VOLUME,
        order = 13,
        isDecimalAllowed = true,
        aliases = setOf("برميل", "barrel", "drum")
    ),

    KILOGRAM(
        displayName = "كجم",
        symbol = "كجم",
        category = Category.SMALL,
        dimension = Dimension.MASS,
        order = 1,
        isDecimalAllowed = true,
        aliases = setOf("كجم", "كيلو", "kilogram", "kg")
    ),
    GRAM(
        displayName = "جرام",
        symbol = "جم",
        category = Category.SMALL,
        dimension = Dimension.MASS,
        order = 2,
        isDecimalAllowed = true,
        aliases = setOf("جرام", "gram", "g")
    ),
    MILLIGRAM(
        displayName = "ملجم",
        symbol = "ملجم",
        category = Category.SMALL,
        dimension = Dimension.MASS,
        order = 3,
        isDecimalAllowed = true,
        aliases = setOf("ملجم", "milligram", "mg")
    ),
    LITER(
        displayName = "لتر",
        symbol = "لتر",
        category = Category.SMALL,
        dimension = Dimension.VOLUME,
        order = 4,
        isDecimalAllowed = true,
        aliases = setOf("لتر", "liter", "l")
    ),
    MILLILITER(
        displayName = "مل",
        symbol = "مل",
        category = Category.SMALL,
        dimension = Dimension.VOLUME,
        order = 5,
        isDecimalAllowed = true,
        aliases = setOf("مل", "مليلتر", "milliliter", "ml")
    ),
    GALLON(
        displayName = "جالون",
        symbol = "جالون",
        category = Category.SMALL,
        dimension = Dimension.VOLUME,
        order = 6,
        isDecimalAllowed = true,
        aliases = setOf("جالون", "gallon")
    ),
    METER(
        displayName = "م",
        symbol = "م",
        category = Category.SMALL,
        dimension = Dimension.LENGTH,
        order = 7,
        isDecimalAllowed = true,
        aliases = setOf("م", "متر", "meter", "m")
    ),
    CENTIMETER(
        displayName = "سم",
        symbol = "سم",
        category = Category.SMALL,
        dimension = Dimension.LENGTH,
        order = 8,
        isDecimalAllowed = true,
        aliases = setOf("سم", "سنتيمتر", "centimeter", "cm")
    ),
    MILLIMETER(
        displayName = "مم",
        symbol = "مم",
        category = Category.SMALL,
        dimension = Dimension.LENGTH,
        order = 9,
        isDecimalAllowed = true,
        aliases = setOf("مم", "مليمتر", "millimeter", "mm")
    ),
    PIECE(
        displayName = "قطعة",
        symbol = "قطعة",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 10,
        isDecimalAllowed = false,
        aliases = setOf("قطعة", "piece", "pcs", "pc")
    ),
    PAIR(
        displayName = "زوج",
        symbol = "زوج",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 11,
        isDecimalAllowed = false,
        aliases = setOf("زوج", "pair")
    ),
    BOTTLE(
        displayName = "زجاجة",
        symbol = "زجاجة",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 12,
        isDecimalAllowed = false,
        aliases = setOf("زجاجة", "bottle")
    ),
    BAG(
        displayName = "كيس",
        symbol = "كيس",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 13,
        isDecimalAllowed = false,
        aliases = setOf("كيس", "bag")
    ),
    BOX(
        displayName = "علبة",
        symbol = "علبة",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 14,
        isDecimalAllowed = false,
        aliases = setOf("علبة", "box")
    ),
    ROLL(
        displayName = "لفة",
        symbol = "لفة",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 15,
        isDecimalAllowed = false,
        aliases = setOf("لفة", "roll")
    ),
    SHEET(
        displayName = "ورقة",
        symbol = "ورقة",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 16,
        isDecimalAllowed = false,
        aliases = setOf("ورقة", "sheet")
    ),
    UNIT(
        displayName = "وحدة",
        symbol = "وحدة",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 17,
        isDecimalAllowed = true,
        aliases = setOf("وحدة", "unit")
    ),

    DEFAULT(
        displayName = "وحدة",
        symbol = "وحدة",
        category = Category.SMALL,
        dimension = Dimension.COUNT,
        order = 99,
        isDecimalAllowed = true
    );

    enum class Category {
        LARGE,
        SMALL
    }

    enum class Dimension {
        MASS,
        VOLUME,
        LENGTH,
        COUNT
    }

    companion object {
        val largeUnits: List<UnitOfMeasure> =
            values()
                .filter { it.category == Category.LARGE && it != DEFAULT }
                .sortedBy { it.order }

        val smallUnits: List<UnitOfMeasure> =
            values()
                .filter { it.category == Category.SMALL && it != DEFAULT }
                .sortedBy { it.order }

        fun fromName(name: String): UnitOfMeasure {
            val normalized = name.trim()
            return values().firstOrNull { unit ->
                unit.name.equals(normalized, ignoreCase = true) ||
                    unit.aliases.any { it.equals(normalized, ignoreCase = true) } ||
                    unit.displayName.equals(normalized, ignoreCase = true)
            } ?: throw IllegalArgumentException("Unknown UnitOfMeasure: $name")
        }
    }
}