package com.myimdad_por.domain.model

import java.math.BigDecimal
import java.util.UUID

/**
 * حالة الدفع المركزية للنظام.
 * تم نقلها هنا لمنع خطأ الـ Redeclaration في الملفات الأخرى.
 */

/**
 * وسيلة الدفع.
 * تم إضافة كلمة 'open' للخصائص للسماح بالوراثة وتجنب خطأ Compilation.
 */
sealed class PaymentMethod(
    open val id: String,
    open val name: String,
    open val type: PaymentMethodType,
    open val providerName: String? = null,
    open val requiresReference: Boolean,
    open val extraFees: BigDecimal = BigDecimal.ZERO,
    open val supportedCurrencies: Set<CurrencyCode> = setOf(CurrencyCode.SDG, CurrencyCode.EGP),
    open val isActive: Boolean = true
) {

    init {
        require(id.isNotBlank()) { "id cannot be blank" }
        require(name.isNotBlank()) { "name cannot be blank" }
        require(extraFees >= BigDecimal.ZERO) { "extraFees cannot be negative" }
        require(supportedCurrencies.isNotEmpty()) { "supportedCurrencies cannot be empty" }

        providerName?.let {
            require(it.isNotBlank()) { "providerName cannot be blank when provided" }
        }
    }

    val displayName: String
        get() = providerName?.trim().takeUnless { it.isNullOrBlank() } ?: name.trim()

    fun supportsCurrency(currencyCode: CurrencyCode): Boolean {
        return currencyCode in supportedCurrencies
    }

    fun isElectronic(): Boolean {
        return type != PaymentMethodType.CASH
    }

    object CASH : PaymentMethod(
        id = "cash",
        name = "Cash",
        type = PaymentMethodType.CASH,
        providerName = null,
        requiresReference = false,
        extraFees = BigDecimal.ZERO
    )

    object BANK_TRANSFER : PaymentMethod(
        id = "bank_transfer",
        name = "Bank Transfer",
        type = PaymentMethodType.BANK_TRANSFER,
        providerName = null,
        requiresReference = true,
        extraFees = BigDecimal.ZERO
    )

    object WALLET : PaymentMethod(
        id = "wallet",
        name = "Wallet",
        type = PaymentMethodType.WALLET,
        providerName = null,
        requiresReference = true,
        extraFees = BigDecimal.ZERO
    )

    object POS : PaymentMethod(
        id = "pos",
        name = "POS",
        type = PaymentMethodType.POS,
        providerName = null,
        requiresReference = true,
        extraFees = BigDecimal.ZERO
    )

    object CHEQUE : PaymentMethod(
        id = "cheque",
        name = "Cheque",
        type = PaymentMethodType.CHEQUE,
        providerName = null,
        requiresReference = true,
        extraFees = BigDecimal.ZERO
    )

    data class Custom(
        override val id: String = UUID.randomUUID().toString(),
        override val name: String,
        override val type: PaymentMethodType, // تم تغييرها لـ override لتطابق النوع الأساسي
        override val providerName: String? = null,
        override val requiresReference: Boolean = false,
        override val extraFees: BigDecimal = BigDecimal.ZERO,
        override val supportedCurrencies: Set<CurrencyCode> = setOf(CurrencyCode.SDG, CurrencyCode.EGP),
        override val isActive: Boolean = true
    ) : PaymentMethod(
        id = id,
        name = name,
        type = type,
        providerName = providerName,
        requiresReference = requiresReference,
        extraFees = extraFees,
        supportedCurrencies = supportedCurrencies,
        isActive = isActive
    )
}

enum class PaymentMethodType {
    CASH,
    BANK_TRANSFER,
    WALLET,
    POS,
    CHEQUE,
    OTHER
}

/**
 * العملات الحالية التي يدعمها النظام.
 */
enum class CurrencyCode(
    val code: String,
    val displayName: String,
    val symbol: String,
    val decimalPlaces: Int
) {
    SDG(
        code = "SDG",
        displayName = "جنيه سوداني",
        symbol = "ج.س",
        decimalPlaces = 2
    ),
    EGP(
        code = "EGP",
        displayName = "جنيه مصري",
        symbol = "ج.م",
        decimalPlaces = 2
    )
}
