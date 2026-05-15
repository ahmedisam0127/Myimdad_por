package com.myimdad_por.ui.features.inventory.contract

import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.ProductUnitHierarchy
import com.myimdad_por.domain.model.StockItem
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Immutable UI state for inventory forms.
 *
 * هذا الإصدار يدعم:
 * - الإضافة العادية
 * - التعديل
 * - إدخال كمية واحدة قديمة (Backward compatible)
 * - إدخال متعدد المستويات حسب ProductUnitHierarchy
 */
data class InventoryFormState(
    val barcode: String = "",
    val productName: String = "",
    val description: String = "",
    val location: String = "",
    val quantity: String = "",
    val quantityByUnit: Map<UnitOfMeasure, String> = emptyMap(),
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.DEFAULT,
    val productUnitHierarchy: ProductUnitHierarchy? = null,
    val expiryDate: LocalDate? = null,
    val selectedProduct: Product? = null,
    val existingStockItem: StockItem? = null,
    val isLoadingProduct: Boolean = false,
    val isSubmitting: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val normalizedBarcode: String
        get() = barcode.trim()

    val normalizedProductName: String
        get() = productName.trim()

    val normalizedDescription: String
        get() = description.trim()

    val normalizedLocation: String
        get() = location.trim()

    val normalizedQuantity: String
        get() = quantity.trim().replace(',', '.')

    val normalizedQuantityByUnit: Map<UnitOfMeasure, String>
        get() = quantityByUnit.mapValues { (_, value) ->
            value.trim().replace(',', '.')
        }

    val effectiveUnitOfMeasure: UnitOfMeasure
        get() = if (unitOfMeasure == UnitOfMeasure.DEFAULT) UnitOfMeasure.UNIT else unitOfMeasure

    val storageUnitOfMeasure: UnitOfMeasure
        get() = productUnitHierarchy?.smallestUnit ?: effectiveUnitOfMeasure

    val quantityAsBigDecimal: BigDecimal?
        get() = normalizedQuantity.toBigDecimalOrNull()

    val quantityAsDouble: Double?
        get() = quantityAsBigDecimal?.toDouble()

    val hasHierarchyInput: Boolean
        get() = normalizedQuantityByUnit.values.any { it.isNotBlank() }

    val hasHierarchy: Boolean
        get() = productUnitHierarchy != null

    val quantityInSmallestUnitAsBigDecimal: BigDecimal?
        get() = when {
            hasHierarchy && hasHierarchyInput -> hierarchyQuantityAsBigDecimalOrNull()
            else -> quantityAsBigDecimal
        }

    val quantityDisplayValue: String
        get() = quantityAsBigDecimal
            ?.stripTrailingZeros()
            ?.let { value ->
                val plain = value.toPlainString()
                if (plain == "-0") "0" else plain
            }
            .orEmpty()

    val hierarchyQuantityDisplayValue: String
        get() {
            val hierarchy = productUnitHierarchy ?: return quantityDisplayValue
            val total = hierarchyQuantityAsBigDecimalOrNull() ?: return quantityDisplayValue

            return formatQuantity(total)
        }

    val isBarcodeValid: Boolean
        get() = isValidBarcode(normalizedBarcode)

    val isProductNameValid: Boolean
        get() = normalizedProductName.isNotEmpty()

    val isLocationValid: Boolean
        get() = normalizedLocation.isNotEmpty()

    val isQuantityValid: Boolean
        get() = when {
            hasHierarchy && hasHierarchyInput -> areHierarchyQuantitiesValid()
            else -> isValidQuantity(
                quantity = quantityAsBigDecimal,
                unitOfMeasure = effectiveUnitOfMeasure
            )
        }

    val isExpiryDateValid: Boolean
        get() = expiryDate == null || !expiryDate.isBefore(MIN_ALLOWED_DATE)

    val isFormValid: Boolean
        get() = isBarcodeValid &&
            isProductNameValid &&
            isLocationValid &&
            isQuantityValid &&
            isExpiryDateValid &&
            !isSubmitting &&
            !isLoadingProduct

    val canSubmit: Boolean
        get() = isFormValid

    val hasSelectedProduct: Boolean
        get() = selectedProduct != null

    val hasExistingStockItem: Boolean
        get() = existingStockItem != null

    val isNewItemMode: Boolean
        get() = !isEditing && existingStockItem == null

    val title: String
        get() = if (isEditing) "تعديل المخزون" else "إضافة إلى المخزون"

    val subtitle: String
        get() = when {
            selectedProduct != null -> selectedProduct.name
            normalizedProductName.isNotEmpty() -> "منتج جديد: $normalizedProductName"
            isEditing && existingStockItem != null -> existingStockItem.productName
            else -> "إدخال بيانات الصنف"
        }

    val validationErrorMessage: String?
        get() = when {
            normalizedBarcode.isEmpty() -> "الباركود مطلوب"
            !isBarcodeValid -> "الباركود غير صالح"
            normalizedProductName.isEmpty() -> "اسم المنتج مطلوب"
            normalizedLocation.isEmpty() -> "الموقع مطلوب"
            !isQuantityValid -> quantityValidationMessage()
            !isExpiryDateValid -> "تاريخ الصلاحية غير صالح"
            else -> null
        }

    fun withResolvedProduct(product: Product?): InventoryFormState {
        return copy(
            selectedProduct = product,
            barcode = product?.barcode ?: barcode,
            productName = product?.name ?: productName,
            description = product?.description.orEmpty().ifBlank { description },
            unitOfMeasure = product?.unitOfMeasure ?: unitOfMeasure,
            productUnitHierarchy = productUnitHierarchy
        )
    }

    fun withResolvedStockItem(stockItem: StockItem?): InventoryFormState {
        return copy(
            existingStockItem = stockItem,
            selectedProduct = selectedProduct ?: stockItem?.let {
                Product(
                    barcode = it.productBarcode,
                    name = it.productName,
                    price = BigDecimal.ZERO,
                    unitOfMeasure = it.unitOfMeasure,
                    displayName = it.displayName,
                    description = null,
                    isActive = true
                )
            },
            barcode = stockItem?.productBarcode ?: barcode,
            productName = stockItem?.productName ?: productName,
            location = stockItem?.location ?: location,
            quantity = stockItem?.quantity
                ?.let { quantityValue ->
                    BigDecimal.valueOf(quantityValue)
                        .stripTrailingZeros()
                        .toPlainString()
                }
                ?: quantity,
            unitOfMeasure = stockItem?.unitOfMeasure ?: unitOfMeasure,
            expiryDate = stockItem?.expiryDate ?: expiryDate,
            isEditing = true
        )
    }

    fun withHierarchy(hierarchy: ProductUnitHierarchy?): InventoryFormState {
        return copy(
            productUnitHierarchy = hierarchy
        )
    }

    fun withHierarchyQuantity(
        unit: UnitOfMeasure,
        value: String
    ): InventoryFormState {
        return copy(
            quantityByUnit = quantityByUnit + (unit to value)
        )
    }

    fun clearHierarchyQuantity(unit: UnitOfMeasure): InventoryFormState {
        return copy(
            quantityByUnit = quantityByUnit - unit
        )
    }

    fun clearMessages(): InventoryFormState = copy(
        errorMessage = null,
        successMessage = null
    )

    fun startLoadingProduct(): InventoryFormState = copy(
        isLoadingProduct = true,
        errorMessage = null
    )

    fun stopLoadingProduct(): InventoryFormState = copy(
        isLoadingProduct = false
    )

    fun startSubmitting(): InventoryFormState = copy(
        isSubmitting = true,
        errorMessage = null,
        successMessage = null
    )

    fun stopSubmitting(): InventoryFormState = copy(
        isSubmitting = false
    )

    fun markSuccess(message: String): InventoryFormState = copy(
        isSubmitting = false,
        isLoadingProduct = false,
        errorMessage = null,
        successMessage = message
    )

    fun markError(message: String): InventoryFormState = copy(
        isSubmitting = false,
        isLoadingProduct = false,
        errorMessage = message,
        successMessage = null
    )

    /**
     * Converts the current form into a domain stock item.
     *
     * يدعم وضعين:
     * 1) إدخال كمية واحدة قديمة
     * 2) إدخال متعدد المستويات عبر hierarchy
     *
     * بالنسبة للحفظ، يتم تخزين الكمية في أصغر وحدة عندما تكون hierarchy موجودة.
     */
    fun toStockItemOrNull(): StockItem? {
        if (!isFormValid) return null

        val safeQuantity = quantityInSmallestUnitAsBigDecimal ?: return null
        if (!isEditing && safeQuantity <= BigDecimal.ZERO) return null

        val safeQuantityDouble = safeQuantity.toDouble()

        return StockItem(
            productBarcode = normalizedBarcode,
            productName = normalizedProductName,
            quantity = safeQuantityDouble,
            location = normalizedLocation,
            unitOfMeasure = storageUnitOfMeasure,
            displayName = normalizedDescription.ifBlank { null },
            expiryDate = expiryDate
        )
    }

    private fun hierarchyQuantityAsBigDecimalOrNull(): BigDecimal? {
        val hierarchy = productUnitHierarchy ?: return null

        if (!hasHierarchyInput) return null

        val knownUnits = hierarchy.levels.map { it.unit }.toSet()
        val unknownUnits = normalizedQuantityByUnit.keys.filter { it !in knownUnits }
        if (unknownUnits.isNotEmpty()) return null

        var total = BigDecimal.ZERO

        hierarchy.levels.forEachIndexed { index, level ->
            val rawValue = normalizedQuantityByUnit[level.unit].orEmpty()
            if (rawValue.isBlank()) return@forEachIndexed

            val quantity = rawValue.toBigDecimalOrNull() ?: return null
            if (quantity < BigDecimal.ZERO) return null

            if (!level.unit.isDecimalAllowed && hasFraction(quantity)) return null

            val multiplier = hierarchy.multiplierToSmallest(index)
            total = total.add(quantity.multiply(multiplier))
        }

        return total.stripTrailingZeros()
    }

    private fun areHierarchyQuantitiesValid(): Boolean {
        val hierarchy = productUnitHierarchy ?: return false
        if (!hasHierarchyInput) return false

        val knownUnits = hierarchy.levels.map { it.unit }.toSet()
        if (normalizedQuantityByUnit.keys.any { it !in knownUnits }) return false

        return hierarchy.levels.all { level ->
            val rawValue = normalizedQuantityByUnit[level.unit].orEmpty()
            if (rawValue.isBlank()) return true

            val quantity = rawValue.toBigDecimalOrNull() ?: return false
            if (quantity < BigDecimal.ZERO) return false
            if (!level.unit.isDecimalAllowed && hasFraction(quantity)) return false

            true
        }
    }

    private fun quantityValidationMessage(): String {
        val hierarchy = productUnitHierarchy
        if (hierarchy != null && hasHierarchyInput) {
            val knownUnits = hierarchy.levels.map { it.unit }.toSet()
            val unknownUnit = normalizedQuantityByUnit.keys.firstOrNull { it !in knownUnits }
            if (unknownUnit != null) {
                return "الوحدة ${unknownUnit.displayName} غير مسموح بها في هذا المنتج"
            }

            val invalidUnit = hierarchy.levels.firstOrNull { level ->
                val rawValue = normalizedQuantityByUnit[level.unit].orEmpty()
                if (rawValue.isBlank()) return@firstOrNull false

                val quantity = rawValue.toBigDecimalOrNull() ?: return@firstOrNull true
                quantity < BigDecimal.ZERO || (!level.unit.isDecimalAllowed && hasFraction(quantity))
            }

            if (invalidUnit != null) {
                return "الكمية في وحدة ${invalidUnit.unit.displayName} غير صالحة"
            }

            return "الكمية غير صالحة"
        }

        return "الكمية غير صالحة"
    }

    private fun formatQuantity(quantity: BigDecimal): String {
        val value = quantity.stripTrailingZeros().toPlainString()
        return if (value == "-0") "0" else value
    }

    companion object {
        private val MIN_ALLOWED_DATE: LocalDate = LocalDate.of(2000, 1, 1)
        private val BARCODE_PATTERN = Regex("^[A-Za-z0-9._-]{4,64}$")

        private fun isValidBarcode(value: String): Boolean {
            return value.isNotBlank() && BARCODE_PATTERN.matches(value)
        }

        private fun isValidQuantity(
            quantity: BigDecimal?,
            unitOfMeasure: UnitOfMeasure
        ): Boolean {
            val safeQuantity = quantity ?: return false
            if (safeQuantity < BigDecimal.ZERO) return false

            return if (unitOfMeasure.isDecimalAllowed) {
                true
            } else {
                !hasFraction(safeQuantity)
            }
        }

        private fun hasFraction(value: BigDecimal): Boolean {
            return value.stripTrailingZeros().scale() > 0
        }

        fun initial(isEditing: Boolean = false): InventoryFormState {
            return InventoryFormState(isEditing = isEditing)
        }
    }
}

private fun String.toBigDecimalOrNull(): BigDecimal? {
    val text = trim()
    if (text.isEmpty()) return null
    return runCatching { BigDecimal(text) }.getOrNull()
}