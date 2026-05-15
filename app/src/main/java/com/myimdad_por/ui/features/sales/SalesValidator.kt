package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.utils.ValidationUtils
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import com.myimdad_por.domain.model.Product
import com.myimdad_por.domain.model.SaleInvoice
import com.myimdad_por.domain.model.SaleItem
import com.myimdad_por.domain.model.SaleInvoiceStatus
import com.myimdad_por.domain.model.UnitOfMeasure
import java.math.BigDecimal

object SalesValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    ) {
        val firstError: String?
            get() = errors.firstOrNull()

        companion object {
            fun success(): ValidationResult = ValidationResult(
                isValid = true,
                errors = emptyList()
            )

            fun failure(vararg errors: String): ValidationResult = ValidationResult(
                isValid = false,
                errors = errors.filter { it.isNotBlank() }
            )

            fun failure(errors: List<String>): ValidationResult = ValidationResult(
                isValid = false,
                errors = errors.filter { it.isNotBlank() }
            )
        }
    }

    fun validateProduct(product: Product?): ValidationResult {
        val errors = mutableListOf<String>()

        if (product == null) {
            errors += SalesConstants.Error.PRODUCT_NOT_FOUND
            return ValidationResult.failure(errors)
        }

        if (!ValidationUtils.isNotEmpty(product.barcode)) {
            errors += "الباركود لا يمكن أن يكون فارغاً"
        } else {
            val barcode = product.barcode.trim()
            if (barcode.length !in SalesConstants.Validation.MIN_BARCODE_LENGTH..SalesConstants.Validation.MAX_BARCODE_LENGTH) {
                errors += "طول الباركود غير صالح"
            }
        }

        if (!ValidationUtils.isNotEmpty(product.name)) {
            errors += "اسم المنتج لا يمكن أن يكون فارغاً"
        }

        if (product.price < BigDecimal.ZERO) {
            errors += SalesConstants.Error.INVALID_PRICE
        }

        if (product.unitFactor <= BigDecimal.ZERO) {
            errors += "معامل التحويل يجب أن يكون أكبر من صفر"
        }

        if (product.largeUnit == product.smallUnit) {
            errors += "الوحدة الكبيرة والوحدة الصغيرة يجب أن تكونا مختلفتين"
        }

        if (product.largeUnit.dimension != product.smallUnit.dimension) {
            errors += "الوحدتان يجب أن تنتميا إلى نفس البُعد"
        }

        if (product.unitOfMeasure.dimension != product.smallUnit.dimension) {
            errors += "وحدة المنتج الأساسية يجب أن تطابق بُعد الوحدات"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validateProductUiModel(product: com.myimdad_por.ui.features.sales.models.ProductUiModel?): ValidationResult {
        if (product == null) {
            return ValidationResult.failure(SalesConstants.Error.PRODUCT_NOT_FOUND)
        }

        val errors = mutableListOf<String>()

        if (product.barcode.isBlank()) {
            errors += "الباركود لا يمكن أن يكون فارغاً"
        }

        if (product.name.isBlank()) {
            errors += "اسم المنتج لا يمكن أن يكون فارغاً"
        }

        if (product.price < BigDecimal.ZERO) {
            errors += SalesConstants.Error.INVALID_PRICE
        }

        if (product.unitFactor <= BigDecimal.ZERO) {
            errors += "معامل التحويل يجب أن يكون أكبر من صفر"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validateSaleItem(item: SaleItem?): ValidationResult {
        val errors = mutableListOf<String>()

        if (item == null) {
            errors += SalesConstants.Error.INVALID_QUANTITY
            return ValidationResult.failure(errors)
        }

        if (!ValidationUtils.isNotEmpty(item.productId)) {
            errors += "معرف المنتج لا يمكن أن يكون فارغاً"
        }

        if (!ValidationUtils.isNotEmpty(item.productName)) {
            errors += "اسم المنتج لا يمكن أن يكون فارغاً"
        }

        if (item.quantity <= BigDecimal.ZERO) {
            errors += SalesConstants.Error.INVALID_QUANTITY
        }

        if (item.unitPrice < BigDecimal.ZERO) {
            errors += SalesConstants.Error.INVALID_PRICE
        }

        if (item.taxAmount < BigDecimal.ZERO) {
            errors += "قيمة الضريبة لا يمكن أن تكون سالبة"
        }

        if (item.discountAmount < BigDecimal.ZERO) {
            errors += "قيمة الخصم لا يمكن أن تكون سالبة"
        }

        if (!item.unit.isDecimalAllowed && item.quantity.stripTrailingZeros().scale() > 0) {
            errors += "هذه الوحدة لا تسمح بالكسور"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validateCartItem(
        quantity: BigDecimal?,
        unitPrice: BigDecimal?,
        taxAmount: BigDecimal? = BigDecimal.ZERO,
        discountAmount: BigDecimal? = BigDecimal.ZERO,
        unit: UnitOfMeasure = UnitOfMeasure.DEFAULT
    ): ValidationResult {
        val errors = mutableListOf<String>()

        if (quantity == null || quantity <= BigDecimal.ZERO) {
            errors += SalesConstants.Error.INVALID_QUANTITY
        } else if (!unit.isDecimalAllowed && quantity.stripTrailingZeros().scale() > 0) {
            errors += "هذه الوحدة لا تسمح بالكسور"
        }

        if (unitPrice == null || unitPrice < BigDecimal.ZERO) {
            errors += SalesConstants.Error.INVALID_PRICE
        }

        if ((taxAmount ?: BigDecimal.ZERO) < BigDecimal.ZERO) {
            errors += "قيمة الضريبة لا يمكن أن تكون سالبة"
        }

        if ((discountAmount ?: BigDecimal.ZERO) < BigDecimal.ZERO) {
            errors += "قيمة الخصم لا يمكن أن تكون سالبة"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validateCustomer(customer: Customer?): ValidationResult {
        val errors = mutableListOf<String>()

        if (customer == null) {
            errors += SalesConstants.Error.CUSTOMER_NOT_FOUND
            return ValidationResult.failure(errors)
        }

        if (!ValidationUtils.isNotEmpty(customer.fullName)) {
            errors += "اسم العميل لا يمكن أن يكون فارغاً"
        }

        if (customer.creditLimit < BigDecimal.ZERO) {
            errors += "حد الائتمان لا يمكن أن يكون سالباً"
        }

        if (customer.outstandingBalance < BigDecimal.ZERO) {
            errors += "الرصيد المستحق لا يمكن أن يكون سالباً"
        }

        if (customer.phoneNumber != null && !ValidationUtils.isValidPhone(customer.phoneNumber)) {
            errors += "رقم الهاتف غير صالح"
        }

        if (customer.email != null && !ValidationUtils.isValidEmail(customer.email)) {
            errors += "البريد الإلكتروني غير صالح"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validateInvoice(invoice: SaleInvoice?): ValidationResult {
        val errors = mutableListOf<String>()

        if (invoice == null) {
            errors += "الفاتورة غير موجودة"
            return ValidationResult.failure(errors)
        }

        if (!ValidationUtils.isNotEmpty(invoice.invoiceNumber)) {
            errors += "رقم الفاتورة لا يمكن أن يكون فارغاً"
        }

        if (!ValidationUtils.isNotEmpty(invoice.saleId)) {
            errors += "معرف البيع لا يمكن أن يكون فارغاً"
        }

        if (!ValidationUtils.isNotEmpty(invoice.employeeId)) {
            errors += "معرف الموظف لا يمكن أن يكون فارغاً"
        }

        if (invoice.items.isEmpty()) {
            errors += SalesConstants.Error.EMPTY_CART
        }

        if (invoice.paidAmount < BigDecimal.ZERO) {
            errors += "المبلغ المدفوع لا يمكن أن يكون سالباً"
        }

        if (invoice.totalAmount < BigDecimal.ZERO) {
            errors += "الإجمالي لا يمكن أن يكون سالباً"
        }

        if (invoice.dueDate != null && invoice.dueDate.isBefore(invoice.issueDate)) {
            errors += "تاريخ الاستحقاق لا يمكن أن يسبق تاريخ الإصدار"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validatePaymentMethod(paymentMethod: PaymentMethod?): ValidationResult {
        if (paymentMethod == null) {
            return ValidationResult.failure("طريقة الدفع غير محددة")
        }

        val errors = mutableListOf<String>()

        if (!ValidationUtils.isNotEmpty(paymentMethod.id)) {
            errors += "معرف طريقة الدفع لا يمكن أن يكون فارغاً"
        }

        if (!ValidationUtils.isNotEmpty(paymentMethod.name)) {
            errors += "اسم طريقة الدفع لا يمكن أن يكون فارغاً"
        }

        if (paymentMethod.extraFees < BigDecimal.ZERO) {
            errors += "الرسوم الإضافية لا يمكن أن تكون سالبة"
        }

        if (paymentMethod.supportedCurrencies.isEmpty()) {
            errors += "يجب تحديد عملة مدعومة واحدة على الأقل"
        }

        if (paymentMethod is PaymentMethod.Custom &&
            paymentMethod.type == PaymentMethodType.CASH &&
            paymentMethod.requiresReference
        ) {
            errors += "الدفع النقدي لا يتطلب مرجعاً"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validateSearchQuery(query: String?): ValidationResult {
        val value = query?.trim().orEmpty()
        if (value.isEmpty()) {
            return ValidationResult.failure("نص البحث لا يمكن أن يكون فارغاً")
        }
        if (value.length < SalesConstants.Validation.MIN_SEARCH_QUERY_LENGTH) {
            return ValidationResult.failure("نص البحث قصير جداً")
        }
        return ValidationResult.success()
    }

    fun validateBarcode(barcode: String?): ValidationResult {
        val value = barcode?.trim().orEmpty()
        val errors = mutableListOf<String>()

        if (value.isEmpty()) {
            errors += "الباركود لا يمكن أن يكون فارغاً"
        } else {
            if (value.length !in SalesConstants.Validation.MIN_BARCODE_LENGTH..SalesConstants.Validation.MAX_BARCODE_LENGTH) {
                errors += "طول الباركود غير صالح"
            }
            if (!value.all { ch -> ch.isLetterOrDigit() || ch == '-' || ch == '_' || ch == '.' }) {
                errors += "الباركود يحتوي على أحرف غير مسموحة"
            }
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validateQuantity(
        quantity: BigDecimal?,
        unit: UnitOfMeasure = UnitOfMeasure.DEFAULT
    ): ValidationResult {
        if (quantity == null) {
            return ValidationResult.failure(SalesConstants.Error.INVALID_QUANTITY)
        }

        val errors = mutableListOf<String>()

        if (quantity <= BigDecimal.ZERO) {
            errors += SalesConstants.Error.INVALID_QUANTITY
        }

        if (!unit.isDecimalAllowed && quantity.stripTrailingZeros().scale() > 0) {
            errors += "هذه الوحدة لا تسمح بالكسور"
        }

        return if (errors.isEmpty()) ValidationResult.success() else ValidationResult.failure(errors)
    }

    fun validatePrice(price: BigDecimal?): ValidationResult {
        if (price == null) {
            return ValidationResult.failure(SalesConstants.Error.INVALID_PRICE)
        }

        return if (price < BigDecimal.ZERO) {
            ValidationResult.failure(SalesConstants.Error.INVALID_PRICE)
        } else {
            ValidationResult.success()
        }
    }

    fun canProceedToCheckout(items: List<SaleItem>): ValidationResult {
        if (items.isEmpty()) {
            return ValidationResult.failure(SalesConstants.Error.EMPTY_CART)
        }

        val invalidItem = items.firstOrNull { !validateSaleItem(it).isValid }
        return if (invalidItem == null) {
            ValidationResult.success()
        } else {
            ValidationResult.failure("يوجد عنصر غير صالح في السلة")
        }
    }

    fun isEditable(invoice: SaleInvoice): Boolean {
        return invoice.status.isEditable && invoice.items.isNotEmpty()
    }

    fun isCancelable(invoice: SaleInvoice): Boolean {
        return invoice.status != SaleInvoiceStatus.CANCELLED &&
            invoice.status != SaleInvoiceStatus.VOID
    }

    fun requireValidProduct(product: Product?) {
        val result = validateProduct(product)
        require(result.isValid) {
            result.firstError ?: SalesConstants.Error.PRODUCT_NOT_FOUND
        }
    }

    fun requireValidSaleItem(item: SaleItem?) {
        val result = validateSaleItem(item)
        require(result.isValid) {
            result.firstError ?: SalesConstants.Error.INVALID_QUANTITY
        }
    }

    fun requireValidInvoice(invoice: SaleInvoice?) {
        val result = validateInvoice(invoice)
        require(result.isValid) {
            result.firstError ?: "الفاتورة غير صالحة"
        }
    }
}