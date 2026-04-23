package com.myimdad_por.domain.model

/**
 * Granular permission catalog for the application.
 *
 * The role decides the default baseline,
 * while the user may hold additional explicit grants.
 */
enum class Permission(
    val key: String,
    val label: String,
    val description: String
) {
    VIEW_DASHBOARD(
        key = "view_dashboard",
        label = "عرض لوحة التحكم",
        description = "Allows access to the dashboard overview."
    ),
    MANAGE_SALES(
        key = "manage_sales",
        label = "إدارة المبيعات",
        description = "Allows creating and editing sales operations."
    ),
    MANAGE_PURCHASES(
        key = "manage_purchases",
        label = "إدارة المشتريات",
        description = "Allows handling purchase operations."
    ),
    MANAGE_INVENTORY(
        key = "manage_inventory",
        label = "إدارة المخزون",
        description = "Allows stock adjustment and inventory control."
    ),
    MANAGE_CUSTOMERS(
        key = "manage_customers",
        label = "إدارة العملاء",
        description = "Allows creating and updating customer records."
    ),
    MANAGE_SUPPLIERS(
        key = "manage_suppliers",
        label = "إدارة الموردين",
        description = "Allows creating and updating supplier records."
    ),
    MANAGE_RETURNS(
        key = "manage_returns",
        label = "إدارة المرتجعات",
        description = "Allows processing returns and reversals."
    ),
    MANAGE_EXPENSES(
        key = "manage_expenses",
        label = "إدارة المصروفات",
        description = "Allows adding and editing expense entries."
    ),
    MANAGE_ACCOUNTING(
        key = "manage_accounting",
        label = "إدارة القيود المحاسبية",
        description = "Allows creating and posting accounting entries."
    ),
    VIEW_REPORTS(
        key = "view_reports",
        label = "عرض التقارير",
        description = "Allows access to business reports."
    ),
    EXPORT_REPORTS(
        key = "export_reports",
        label = "تصدير التقارير",
        description = "Allows exporting reports to external formats."
    ),
    MANAGE_PAYMENTS(
        key = "manage_payments",
        label = "إدارة المدفوعات",
        description = "Allows authorizing and processing payments."
    ),
    PROCESS_REFUND(
        key = "process_refund",
        label = "تنفيذ الاسترداد",
        description = "Allows creating refund operations."
    ),
    VOID_INVOICE(
        key = "void_invoice",
        label = "إلغاء الفاتورة",
        description = "Allows voiding already issued invoices."
    ),
    APPROVE_PAYMENT(
        key = "approve_payment",
        label = "اعتماد الدفع",
        description = "Allows approving sensitive payment operations."
    ),
    MANAGE_USERS(
        key = "manage_users",
        label = "إدارة المستخدمين",
        description = "Allows creating, editing, and disabling users."
    ),
    MANAGE_SETTINGS(
        key = "manage_settings",
        label = "إدارة الإعدادات",
        description = "Allows system configuration changes."
    ),
    VIEW_AUDIT_LOGS(
        key = "view_audit_logs",
        label = "عرض سجل التدقيق",
        description = "Allows reading audit history."
    ),
    MANAGE_SUBSCRIPTION(
        key = "manage_subscription",
        label = "إدارة الاشتراك",
        description = "Allows subscription and license operations."
    ),
    ACCESS_HARDWARE(
        key = "access_hardware",
        label = "الوصول للعتاد",
        description = "Allows use of scanners, printers, and attached devices."
    ),
    MANAGE_PRINTER(
        key = "manage_printer",
        label = "إدارة الطباعة",
        description = "Allows printer connection and printing actions."
    ),
    MANAGE_SCANNER(
        key = "manage_scanner",
        label = "إدارة المسح",
        description = "Allows barcode scanner usage."
    ),
    CHANGE_PASSWORD(
        key = "change_password",
        label = "تغيير كلمة المرور",
        description = "Allows changing the user's own password."
    ),
    CUSTOM(
        key = "custom",
        label = "صلاحية مخصصة",
        description = "Reserved for future extension."
    );
}