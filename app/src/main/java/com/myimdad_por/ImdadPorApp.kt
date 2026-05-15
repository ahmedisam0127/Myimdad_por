package com.myimdad_por

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * الكلاس الرئيسي للتطبيق (Application Class).
 * يتم وضع علامة @HiltAndroidApp هنا لإطلاق عملية توليد الكود الخاصة بـ Dagger Hilt.
 * هذا الملف هو المكان الذي يبدأ فيه "عمر" التطبيق، ويظل حياً طالما أن التطبيق يعمل في الخلفية.
 */
@HiltAndroidApp
class ImdadPorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // هنا يمكنك إضافة مكتبات التحليل (Analytics) 
        // أو تهيئة قواعد البيانات المحلية (Room) 
        // أو أي إعدادات تحتاج للعمل لمرة واحدة عند فتح التطبيق.
    }
}
