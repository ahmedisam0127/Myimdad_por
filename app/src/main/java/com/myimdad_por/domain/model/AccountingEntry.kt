package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.util.UUID

/**
 * قيد محاسبي بنظام القيد المزدوج.
 *
 * كل قيد يمثل حركة مالية متوازنة:
 * - مدين
 * - دائن
 * - بنفس القيمة
 */
data class AccountingEntry(
    val id: String = UUID.randomUUID().toString(),
    val transactionDate: LocalDateTime,
    val referenceId: String? = null,
    val description: String,
    val debitAccount: String,
    val creditAccount: String,
    val amount: BigDecimal,
    val currency: CurrencyCode = CurrencyCode.SDG,
    val paymentMethod: PaymentMethod? = null,
    val source: AccountingSource = AccountingSource.MANUAL,
    val status: AccountingEntryStatus = AccountingEntryStatus.POSTED,
    val createdByEmployeeId: String? = null,
    val note: String? = null
) {

    init {
        require(description.isNotBlank()) { "description cannot be blank" }
        require(debitAccount.isNotBlank()) { "debitAccount cannot be blank" }
        require(creditAccount.isNotBlank()) { "creditAccount cannot be blank" }
        require(debitAccount != creditAccount) { "debitAccount and creditAccount must be different" }
        require(amount > BigDecimal.ZERO) { "amount must be greater than zero" }
        require(transactionDate.isBefore(LocalDateTime.now().plusSeconds(5))) {
            "transactionDate cannot be in the future"
        }

        paymentMethod?.let {
            require(it.supportsCurrency(currency)) {
                "Payment method ${it.displayName} does not support currency ${currency.code}"
            }
        }
    }

    val debitAmount: BigDecimal
        get() = amount.money()

    val creditAmount: BigDecimal
        get() = amount.money()

    val isBalanced: Boolean
        get() = debitAmount == creditAmount

    val isPosted: Boolean
        get() = status == AccountingEntryStatus.POSTED

    fun post(): AccountingEntry = copy(status = AccountingEntryStatus.POSTED)

    fun draft(): AccountingEntry = copy(status = AccountingEntryStatus.DRAFT)

    fun reverse(
        reversedReferenceId: String? = referenceId,
        reversedDescription: String? = null
    ): AccountingEntry {
        return AccountingEntry(
            transactionDate = LocalDateTime.now(),
            referenceId = reversedReferenceId,
            description = reversedDescription ?: "Reversal: $description",
            debitAccount = creditAccount,
            creditAccount = debitAccount,
            amount = amount,
            currency = currency,
            paymentMethod = paymentMethod,
            source = AccountingSource.REVERSAL,
            status = AccountingEntryStatus.POSTED,
            createdByEmployeeId = createdByEmployeeId,
            note = note
        )
    }

    companion object {
        fun saleCashEntry(
            saleReferenceId: String,
            salesRevenueAccount: String,
            cashAccount: String,
            amount: BigDecimal,
            currency: CurrencyCode = CurrencyCode.SDG,
            paymentMethod: PaymentMethod = PaymentMethod.CASH,
            description: String = "إثبات مبيعات نقدية"
        ): AccountingEntry {
            return AccountingEntry(
                transactionDate = LocalDateTime.now(),
                referenceId = saleReferenceId,
                description = description,
                debitAccount = cashAccount,
                creditAccount = salesRevenueAccount,
                amount = amount,
                currency = currency,
                paymentMethod = paymentMethod,
                source = AccountingSource.SALE,
                status = AccountingEntryStatus.POSTED
            )
        }

        fun expenseEntry(
            expenseReferenceId: String,
            expenseAccount: String,
            cashAccount: String,
            amount: BigDecimal,
            currency: CurrencyCode = CurrencyCode.SDG,
            paymentMethod: PaymentMethod = PaymentMethod.CASH,
            description: String = "إثبات مصروف"
        ): AccountingEntry {
            return AccountingEntry(
                transactionDate = LocalDateTime.now(),
                referenceId = expenseReferenceId,
                description = description,
                debitAccount = expenseAccount,
                creditAccount = cashAccount,
                amount = amount,
                currency = currency,
                paymentMethod = paymentMethod,
                source = AccountingSource.EXPENSE,
                status = AccountingEntryStatus.POSTED
            )
        }

        fun purchaseEntry(
            purchaseReferenceId: String,
            inventoryAccount: String,
            payableAccount: String,
            amount: BigDecimal,
            currency: CurrencyCode = CurrencyCode.SDG,
            paymentMethod: PaymentMethod? = null,
            description: String = "إثبات مشتريات"
        ): AccountingEntry {
            return AccountingEntry(
                transactionDate = LocalDateTime.now(),
                referenceId = purchaseReferenceId,
                description = description,
                debitAccount = inventoryAccount,
                creditAccount = payableAccount,
                amount = amount,
                currency = currency,
                paymentMethod = paymentMethod,
                source = AccountingSource.PURCHASE,
                status = AccountingEntryStatus.POSTED
            )
        }
    }
}

enum class AccountingSource {
    MANUAL,
    SALE,
    PURCHASE,
    EXPENSE,
    RETURN,
    PAYMENT,
    TRANSFER,
    ADJUSTMENT,
    REVERSAL,
    OPENING_BALANCE
}

enum class AccountingEntryStatus {
    DRAFT,
    POSTED,
    REVERSED,
    VOID
}

private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)