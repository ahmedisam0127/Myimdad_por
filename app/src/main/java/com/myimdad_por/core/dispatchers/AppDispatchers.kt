package com.myimdad_por.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * مجموعة الـ Dispatchers الخاصة بالتطبيق
 * الهدف منها:
 * - فصل الاعتماد المباشر عن Dispatchers
 * - تسهيل الاختبار
 * - توحيد استخدام الـ coroutines داخل المشروع
 */
interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/**
 * التنفيذ الافتراضي في التطبيق الحقيقي
 */
object DefaultAppDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}