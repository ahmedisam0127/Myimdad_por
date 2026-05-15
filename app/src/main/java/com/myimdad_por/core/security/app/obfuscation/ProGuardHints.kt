package com.myimdad_por.core.security.app.obfuscation

import androidx.annotation.Keep

/**
 * إشارات للمترجم وR8/ProGuard للحفاظ على كود الحماية ومنع حذفه أو تبسيطه بشكل مضر.
 *
 * هذا الملف لا يضيف منطقًا أمنيًا مباشرًا، بل يعمل كمرساة (anchor) لضمان
 * بقاء المكونات الحساسة قابلة للاستدعاء من Reflection أو عبر مسارات غير مباشرة.
 */
@Keep
object ProGuardHints {

    /**
     * واجهة وهمية يمكن ربطها بمسارات داخلية أو Reflection.
     * اسمها يبدو طبيعيًا لتقليل الانتباه أثناء الهندسة العكسية.
     */
    @Keep
    interface InternalLoggerStub {
        fun logEvent(id: Int, data: Any?)
    }

    /**
     * واجهة وهمية أخرى لتقليل فرص إزالة الكود المرتبط بعمليات التحقق.
     */
    @Keep
    interface AnalyticsProvider {
        fun track(code: Int, payload: Any? = null)
    }

    /**
     * نقطة تثبيت تمنع R8 من اعتبار هذا المسار غير مستخدم.
     * يمكن استدعاؤها من أماكن متعددة دون أي أثر وظيفي.
     */
    @Keep
    fun anchor() {
        // Intentionally empty.
        // إبقاء المرجع حيًا داخل الـ bytecode.
    }

    /**
     * استدعاء وهمي لتثبيت المراجع المتعلقة بطبقة الحماية.
     */
    @Keep
    fun touchSecurityGraph() {
        // هذه المراجع تساعد في إبقاء الكلاسات الأمنية المهمة حية بعد التمويه.
        val refs = arrayOf(
            "com.myimdad_por.core.security.RootDetector",
            "com.myimdad_por.core.security.DebugDetector",
            "com.myimdad_por.core.security.TamperProtection",
            "com.myimdad_por.core.security.IntegrityChecker",
            "com.myimdad_por.core.security.SubscriptionGuard",
            "com.myimdad_por.core.security.SessionManager"
        )

        // لا يتم استخدام القيم وظيفيًا، فقط إبقاؤها مرئية للمترجم.
        if (refs.isEmpty()) {
            anchor()
        }
    }

    /**
     * Stub آمن للاستدعاء عبر Reflection عند الحاجة.
     */
    @Keep
    fun resolve(name: String): String {
        require(name.isNotBlank()) { "name must not be blank." }
        return name.trim()
    }

    /**
     * دالة صغيرة تساعد على منع إزالة سلسلة استدعاءات الحماية.
     */
    @Keep
    fun markUsed(token: Any?) {
        token?.hashCode()
    }
}