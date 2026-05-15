package com.myimdad_por.ui.features.sales

import androidx.compose.runtime.Immutable
import com.myimdad_por.core.base.UiState
import com.myimdad_por.domain.model.Customer
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.SubscriptionInfo
import com.myimdad_por.ui.features.sales.models.BillUiModel
import com.myimdad_por.ui.features.sales.models.CartUiModel
import com.myimdad_por.ui.features.sales.models.ProductUiModel
import java.math.BigDecimal

@Immutable
data class SalesUiState(

    /**
     * حالات التحميل الموحدة
     */
    val uiState: UiState<Unit> = UiState.Idle,

    /**
     * الاشتراك
     */
    val subscriptionInfo: SubscriptionInfo? = null,

    /**
     * هل المستخدم يستطيع إكمال عملية البيع
     * البيع لا يكتمل إلا عند وجود اشتراك نشط
     */
    val canCompleteSale: Boolean = false,

    /**
     * هل الاشتراك منتهي
     */
    val isSubscriptionExpired: Boolean = false,

    /**
     * هل الاشتراك مطلوب
     */
    val requiresSubscription: Boolean = true,

    /**
     * رسالة حالة الاشتراك
     */
    val subscriptionMessage: String? = null,

    /**
     * بيانات البحث
     */
    val searchQuery: String = "",
    val barcodeQuery: String = "",

    /**
     * المنتجات
     */
    val products: List<ProductUiModel> = emptyList(),
    val filteredProducts: List<ProductUiModel> = emptyList(),

    /**
     * السلة
     */
    val cartItems: List<CartUiModel> = emptyList(),

    /**
     * العميل المحدد
     */
    val selectedCustomer: Customer? = null,

    /**
     * طريقة الدفع
     */
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,

    /**
     * الفاتورة الحالية
     */
    val currentBill: BillUiModel? = null,

    /**
     * رقم الفاتورة
     */
    val invoiceNumber: String = "",
    val referenceNumber: String = "",


    /**
     * ملاحظات الفاتورة
     */
    val notes: String = "",

    /**
     * هل البيع آجل
     */
    val isCreditSale: Boolean = false,

    /**
     * نسبة الضريبة
     */
    val taxRate: BigDecimal = BigDecimal.ZERO,

    /**
     * حالات الواجهة
     */
    val isSearching: Boolean = false,
    val isSubmittingSale: Boolean = false,
    val isLoadingProducts: Boolean = false,
    val isLoadingCustomers: Boolean = false,
    val isCartExpanded: Boolean = false,
    val isPaymentSheetVisible: Boolean = false,
    val isCustomerSheetVisible: Boolean = false,
    val isScannerEnabled: Boolean = false,

    /**
     * الرسائل والأخطاء
     */
    val message: String? = null,
    val errorMessage: String? = null,

    /**
     * المجاميع المالية
     */
    val subtotalAmount: BigDecimal = BigDecimal.ZERO,
    val taxAmount: BigDecimal = BigDecimal.ZERO,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val paidAmount: BigDecimal = BigDecimal.ZERO,
    val remainingAmount: BigDecimal = BigDecimal.ZERO,

    /**
     * عدد العناصر
     */
    val totalItemsCount: Int = 0,

    /**
     * آخر فاتورة ناجحة
     */
    val lastCompletedBill: BillUiModel? = null
) {

    /**
     * هل السلة فارغة
     */
    val isCartEmpty: Boolean
        get() = cartItems.isEmpty()

    /**
     * هل يوجد عميل
     */
    val hasCustomer: Boolean
        get() = selectedCustomer != null

    /**
     * هل توجد منتجات
     */
    val hasProducts: Boolean
        get() = products.isNotEmpty()

    /**
     * هل توجد نتائج بحث
     */
    val hasSearchResults: Boolean
        get() = filteredProducts.isNotEmpty()

    /**
     * هل يوجد خطأ
     */
    val hasError: Boolean
        get() = !errorMessage.isNullOrBlank()

    /**
     * هل يوجد إشعار
     */
    val hasMessage: Boolean
        get() = !message.isNullOrBlank()

    /**
     * هل البيع جاهز للتنفيذ
     */
    val isSaleReady: Boolean
        get() = canCompleteSale &&
            !isCartEmpty &&
            totalAmount > BigDecimal.ZERO &&
            !isSubmittingSale

    /**
     * هل الدفع مكتمل
     */
    val isPaymentCompleted: Boolean
        get() = paidAmount >= totalAmount &&
            totalAmount > BigDecimal.ZERO

    /**
     * هل توجد عناصر مرتجعة
     */
    val hasReturns: Boolean
        get() = cartItems.any { it.isReturn }

    /**
     * هل يوجد مبلغ متبقٍ
     */
    val hasRemainingAmount: Boolean
        get() = remainingAmount > BigDecimal.ZERO

    /**
     * هل الدفع جزئي
     */
    val isPartialPayment: Boolean
        get() = paidAmount > BigDecimal.ZERO &&
            paidAmount < totalAmount

    /**
     * إجمالي الكميات
     */
    val totalQuantity: BigDecimal
        get() = cartItems.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.quantity
        }

    /**
     * إجمالي عدد المنتجات الفريدة
     */
    val uniqueProductsCount: Int
        get() = cartItems.distinctBy { it.productId }.size

    companion object {

        fun initial(): SalesUiState {
            return SalesUiState()
        }

        fun subscriptionRequired(
            message: String = "يجب تفعيل الاشتراك لإكمال عملية البيع"
        ): SalesUiState {
            return SalesUiState(
                requiresSubscription = true,
                canCompleteSale = false,
                subscriptionMessage = message,
                errorMessage = message
            )
        }
    }
}