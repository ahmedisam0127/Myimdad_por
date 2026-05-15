package com.myimdad_por.data.mapper

import com.myimdad_por.core.utils.DateTimeUtils
import com.myimdad_por.core.utils.toBigDecimalOrZero
import com.myimdad_por.data.remote.dto.ExpenseDto
import com.myimdad_por.domain.model.Expense
import com.myimdad_por.domain.model.ExpenseCategory
import com.myimdad_por.domain.model.ExpenseStatus
import com.myimdad_por.domain.model.PaymentMethod
import java.util.UUID

/**
 * دالة مساعدة لتحويل النصوص إلى Enum بأمان تام.
 * تستخدم فقط مع (ExpenseCategory و ExpenseStatus)
 */
inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
    if (this.isNullOrBlank()) return default
    return try {
        enumValueOf<T>(this.trim().uppercase())
    } catch (e: Exception) {
        default
    }
}

/**
 * دالة مساعدة مخصصة لتحويل النص القادم من الـ API إلى (Sealed Class) بأمان.
 */
fun String?.toPaymentMethod(): PaymentMethod {
    return when (this?.trim()?.uppercase()) {
        "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER
        "WALLET" -> PaymentMethod.WALLET
        "POS" -> PaymentMethod.POS
        "CHEQUE" -> PaymentMethod.CHEQUE
        "CASH" -> PaymentMethod.CASH
        else -> PaymentMethod.CASH // القيمة الافتراضية الآمنة
    }
}

/**
 * Maps [ExpenseDto] from the data layer to [Expense] in the domain layer.
 */
fun ExpenseDto.toDomain(): Expense {
    return Expense(
        id = this.id ?: UUID.randomUUID().toString(),
        expenseNumber = this.expenseNumber.orEmpty(),
        category = this.category.toEnumOrDefault(ExpenseCategory.OTHER),
        title = this.title ?: "Untitled Expense",
        amount = this.amount.toBigDecimalOrZero(),
        expenseDate = this.expenseDate?.let { DateTimeUtils.parseDateTime(it) } 
            ?: DateTimeUtils.now(),
        paidAmount = this.paidAmount.toBigDecimalOrZero(),
        // هنا نستخدم الدالة الجديدة المخصصة للـ Sealed Class
        paymentMethod = this.paymentMethod.toPaymentMethod(),
        status = this.status.toEnumOrDefault(ExpenseStatus.PENDING),
        referenceNumber = this.referenceNumber,
        supplierName = this.supplierName,
        employeeId = this.employeeId,
        note = this.note
    )
}

/**
 * Maps [Expense] from the domain layer back to [ExpenseDto] for API requests.
 */
fun Expense.toDto(): ExpenseDto {
    return ExpenseDto(
        id = this.id,
        expenseNumber = this.expenseNumber,
        category = this.category.name,
        title = this.title,
        amount = this.amount.toPlainString(),
        expenseDate = DateTimeUtils.toIsoString(this.expenseDate),
        paidAmount = this.paidAmount.toPlainString(),
        // نستخدم type.name للحصول على النص (مثل "CASH" أو "BANK_TRANSFER") المتوافق مع الـ API
        paymentMethod = this.paymentMethod.type.name, 
        status = this.status.name,
        referenceNumber = this.referenceNumber,
        supplierName = this.supplierName,
        employeeId = this.employeeId,
        note = this.note
    )
}
