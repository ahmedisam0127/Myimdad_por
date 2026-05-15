package com.myimdad_por.ui.navigation

/**
 * مصدر واحد ثابت لكل مسارات التنقل داخل التطبيق.
 *
 * المبدأ هنا:
 * - عدم تغيير أي قيمة Route موجودة بالفعل
 * - إضافة المسارات الناقصة فقط عند الحاجة
 * - استخدام دوال مساعدة للمسارات الديناميكية بدل تكرار النصوص
 */
object ScreenRoutes {

    // ───────────────────────── Core ─────────────────────────

    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Login = "login"
    const val Register = "register"
    const val ForgotPassword = "forgot_password"

    // ───────────────────────── Main ─────────────────────────

    const val Home = "home"
    const val Dashboard = "dashboard"
    const val Profile = "profile"
    const val Settings = "settings"
    const val Notifications = "notifications"
    const val Search = "search"

    // ───────────────────── Business Modules ─────────────────────

    const val Customers = "customers"
    const val CustomerCreate = "customer_create"
    const val Suppliers = "suppliers"
    const val SupplierCreate = "supplier_create"

    const val Products = "products"
    const val ProductCreate = "product_create"

    const val Inventory = "inventory"
    const val StockMovement = "stock_movement"

    const val Invoices = "invoices"
    const val InvoiceCreate = "invoice_create"

    const val Purchases = "purchases"
    const val PurchaseCreate = "purchase_create"

    const val Sales = "sales"
    const val SaleCreate = "sale_create"

    const val Payments = "payments"
    const val PaymentCreate = "payment_create"

    const val Returns = "returns"
    const val ReturnCreate = "return_create"

    const val Expenses = "expenses"
    const val ExpenseCreate = "expense_create"

    const val Reports = "reports"
    const val Analytics = "analytics"
    const val AuditLogs = "audit_logs"

    // إضافات مفيدة للقائمة الجانبية دون كسر أي روابط سابقة
    const val Accounting = "accounting"
    const val Security = "security"

    // ───────────────── Subscription / Access Control ─────────────────

    const val Subscription = "subscription"
    const val SubscriptionExpired = "subscription_expired"
    const val SubscriptionRenew = "subscription_renew"

    // ───────────────────────── Shared Arguments ─────────────────────────

    const val ARG_ID = "id"
    const val ARG_STATUS = "status"
    const val ARG_MODE = "mode"
    const val ARG_QUERY = "query"

    // ───────────────────────── Detail Patterns ─────────────────────────

    const val CustomerDetailsPattern = "customer_details/{$ARG_ID}"
    const val SupplierDetailsPattern = "supplier_details/{$ARG_ID}"
    const val ProductDetailsPattern = "product_details/{$ARG_ID}"
    const val InvoiceDetailsPattern = "invoice_details/{$ARG_ID}"
    const val PurchaseDetailsPattern = "purchase_details/{$ARG_ID}"
    const val SaleDetailsPattern = "sale_details/{$ARG_ID}"
    const val PaymentDetailsPattern = "payment_details/{$ARG_ID}"
    const val ReturnDetailsPattern = "return_details/{$ARG_ID}"
    const val ExpenseDetailsPattern = "expense_details/{$ARG_ID}"

    // ───────────────────────── Builders ─────────────────────────

    fun customerDetails(id: String): String = "customer_details/$id"
    fun supplierDetails(id: String): String = "supplier_details/$id"
    fun productDetails(id: String): String = "product_details/$id"
    fun invoiceDetails(id: String): String = "invoice_details/$id"
    fun purchaseDetails(id: String): String = "purchase_details/$id"
    fun saleDetails(id: String): String = "sale_details/$id"
    fun paymentDetails(id: String): String = "payment_details/$id"
    fun returnDetails(id: String): String = "return_details/$id"
    fun expenseDetails(id: String): String = "expense_details/$id"

    fun search(query: String): String = "search/$query"
    fun notifications(status: String): String = "notifications/$status"
}