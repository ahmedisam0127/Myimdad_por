package com.myimdad_por.ui.features.sales

import com.myimdad_por.core.utils.Constants
import com.myimdad_por.domain.model.LegalStatus
import com.myimdad_por.domain.model.PaymentMethod
import com.myimdad_por.domain.model.PaymentMethodType
import com.myimdad_por.domain.model.SaleInvoiceStatus
import java.math.BigDecimal

object SalesConstants {

    object Ui {

        const val SCREEN_TITLE = "المبيعات"
        const val SEARCH_HINT = "ابحث عن منتج أو باركود..."
        const val CUSTOMER_SEARCH_HINT = "ابحث عن عميل..."
        const val NOTES_HINT = "أضف ملاحظات..."
        const val EMPTY_CART_MESSAGE = "السلة فارغة"
        const val NO_PRODUCTS_MESSAGE = "لا توجد منتجات"
        const val NO_CUSTOMERS_MESSAGE = "لا يوجد عملاء"
        const val NO_RESULTS_MESSAGE = "لا توجد نتائج مطابقة"
        const val ADD_TO_CART_SUCCESS = "تمت إضافة المنتج إلى السلة"
        const val REMOVE_FROM_CART_SUCCESS = "تم حذف المنتج من السلة"
        const val SALE_COMPLETED_SUCCESS = "تم حفظ الفاتورة بنجاح"
        const val PAYMENT_COMPLETED_SUCCESS = "تم تسجيل الدفع بنجاح"
        const val CONFIRM_DELETE_ITEM = "هل تريد حذف العنصر من السلة؟"
        const val CONFIRM_CANCEL_SALE = "هل تريد إلغاء عملية البيع؟"

        const val DEFAULT_SEARCH_DEBOUNCE = 300L
        const val DEFAULT_ANIMATION_DURATION = 250L
        const val MAX_NOTE_LENGTH = 500
        const val MAX_BARCODE_LENGTH = 64
        const val MIN_QUANTITY = 1
        const val DEFAULT_GRID_SPAN = 2

        const val RECEIPT_COPY_CUSTOMER = "نسخة العميل"
        const val RECEIPT_COPY_STORE = "نسخة المتجر"

        const val LABEL_SUBTOTAL = "الإجمالي"
        const val LABEL_DISCOUNT = "الخصم"
        const val LABEL_TAX = "الضريبة"
        const val LABEL_TOTAL = "الإجمالي النهائي"
        const val LABEL_PAID = "المدفوع"
        const val LABEL_REMAINING = "المتبقي"

        const val ACTION_PAY = "دفع"
        const val ACTION_SAVE = "حفظ"
        const val ACTION_CANCEL = "إلغاء"
        const val ACTION_PRINT = "طباعة"
        const val ACTION_SHARE = "مشاركة"
        const val ACTION_ADD = "إضافة"
        const val ACTION_REMOVE = "حذف"
        const val ACTION_CLEAR = "تفريغ"
        const val ACTION_SCAN = "مسح"
    }

    object Validation {

        const val MIN_PRODUCT_QUANTITY = 1
        const val MAX_PRODUCT_QUANTITY = 999999

        const val MIN_PRICE = 0.0
        const val MAX_PRICE = 999999999.99

        const val MAX_DISCOUNT_PERCENTAGE = 100
        const val MAX_TAX_PERCENTAGE = 100

        const val MIN_CUSTOMER_NAME_LENGTH = 3
        const val MAX_CUSTOMER_NAME_LENGTH = 120

        const val MIN_BARCODE_LENGTH = 4
        const val MAX_BARCODE_LENGTH = 64

        const val MAX_REFERENCE_LENGTH = 128
        const val MAX_INVOICE_NUMBER_LENGTH = 64

        const val MIN_SEARCH_QUERY_LENGTH = 1
    }

    object Defaults {

        val DEFAULT_QUANTITY: BigDecimal =
            BigDecimal.ONE

        val DEFAULT_DISCOUNT: BigDecimal =
            BigDecimal.ZERO

        val DEFAULT_TAX: BigDecimal =
            BigDecimal.ZERO

        val DEFAULT_PAID_AMOUNT: BigDecimal =
            BigDecimal.ZERO

        val DEFAULT_EXTRA_FEES: BigDecimal =
            BigDecimal.ZERO

        val DEFAULT_PAYMENT_METHOD: PaymentMethod =
            PaymentMethod.CASH

        val DEFAULT_INVOICE_STATUS: SaleInvoiceStatus =
            SaleInvoiceStatus.DRAFT

        val DEFAULT_LEGAL_STATUS: LegalStatus =
            LegalStatus.NOT_SUBMITTED

        val DEFAULT_PAYMENT_METHOD_TYPE: PaymentMethodType =
            PaymentMethodType.CASH
    }

    object Invoice {

        const val PREFIX = "INV"
        const val DATE_PATTERN = "yyyyMMdd"
        const val NUMBER_SEPARATOR = "-"

        const val QR_PREFIX = "SALE"

        const val TERMS_AND_CONDITIONS = """
            • البضاعة المباعة لا ترد ولا تستبدل إلا حسب السياسة المعتمدة.
            • يرجى الاحتفاظ بالفاتورة لإثبات عملية الشراء.
            • الأسعار شاملة الضرائب إن وجدت.
        """

        const val FOOTER_MESSAGE =
            "شكراً لتعاملكم معنا"

        const val WATERMARK_DRAFT =
            "مسودة"

        const val WATERMARK_PAID =
            "مدفوعة"
    }

    object Cache {

        const val PRODUCTS_CACHE_KEY =
            "sales_products_cache"

        const val CUSTOMERS_CACHE_KEY =
            "sales_customers_cache"

        const val CART_CACHE_KEY =
            "sales_cart_cache"

        const val LAST_INVOICE_CACHE_KEY =
            "sales_last_invoice_cache"

        const val CACHE_EXPIRATION_MINUTES = 15L
    }

    object Navigation {

        const val ROUTE_SALES = "sales"
        const val ROUTE_CART = "sales_cart"
        const val ROUTE_CHECKOUT = "sales_checkout"
        const val ROUTE_INVOICE_PREVIEW = "sales_invoice_preview"
        const val ROUTE_PAYMENT = "sales_payment"
        const val ROUTE_HISTORY = "sales_history"

        const val ARG_INVOICE_ID = "invoice_id"
        const val ARG_CUSTOMER_ID = "customer_id"
        const val ARG_PRODUCT_ID = "product_id"
    }

    object Error {

        const val PRODUCT_NOT_FOUND =
            "المنتج غير موجود"

        const val CUSTOMER_NOT_FOUND =
            "العميل غير موجود"

        const val INVALID_QUANTITY =
            "الكمية غير صالحة"

        const val INVALID_PRICE =
            "السعر غير صالح"

        const val PAYMENT_FAILED =
            "فشل تسجيل عملية الدفع"

        const val EMPTY_CART =
            "لا يمكن إتمام البيع والسلة فارغة"

        const val NETWORK_ERROR =
            Constants.Ui.DEFAULT_ERROR_MESSAGE
    }

    object Tags {

        const val SALES_SCREEN = "sales_screen"
        const val PRODUCTS_LIST = "products_list"
        const val CART_LIST = "cart_list"
        const val CHECKOUT_BUTTON = "checkout_button"
        const val SEARCH_FIELD = "search_field"
        const val PAYMENT_DIALOG = "payment_dialog"
        const val CUSTOMER_SECTION = "customer_section"
        const val TOTAL_SECTION = "total_section"
    }
}