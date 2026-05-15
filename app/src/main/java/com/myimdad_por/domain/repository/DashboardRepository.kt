package com.myimdad_por.domain.repository

import com.myimdad_por.domain.model.Dashboard
import kotlinx.coroutines.flow.Flow

/**
 * واجهة المستودع (Repository Interface) الخاصة بلوحة التحكم.
 *
 * تعمل هذه الواجهة كعقد (Contract) يفصل بين منطق العمل (Domain Layer) وطريقة جلب البيانات (Data Layer).
 * تم تصميمها لتدعم استراتيجية الـ Offline-first والتحديثات الحية.
 */
interface DashboardRepository {

    /**
     * جلب بيانات لوحة التحكم بالكامل.
     *
     * @param forceRefresh إذا كانت true، سيقوم المستودع بتجاهل الكاش المحلي وجلب البيانات من السيرفر.
     * @return [Result] يحتوي على كائن [Dashboard] في حال النجاح.
     */
    suspend fun getDashboard(forceRefresh: Boolean = false): Result<Dashboard>

    /**
     * تحديث بيانات لوحة التحكم قسرياً من المصدر الخارجي (Remote) وتحديث الكاش المحلي.
     * * مفيد عند تنفيذ عملية "اسحب للتحديث" (Pull-to-refresh).
     */
    suspend fun refreshDashboard(): Result<Dashboard>

    /**
     * مراقبة التغيرات في بيانات لوحة التحكم بشكل لحظي.
     * * @return [Flow] يبعث كائن [Dashboard] جديد كلما حدث تغيير في قاعدة البيانات المحلية.
     */
    fun observeDashboard(): Flow<Dashboard>

    /**
     * استرجاع آخر نسخة محفوظة من البيانات في الكاش المحلي دون الاتصال بالإنترنت.
     * * @return كائن [Dashboard] أو null إذا لم تتوفر بيانات مخزنة.
     */
    suspend fun getCachedDashboard(): Dashboard?

    /**
     * تحديد تنبيه معين كـ "تمت قراءته".
     * * @param alertId المعرف الفريد للتنبيه.
     */
    suspend fun markAlertAsRead(alertId: String): Result<Unit>

    /**
     * إزالة تنبيه من لوحة التحكم (إخفاء أو حذف).
     * * @param alertId المعرف الفريد للتنبيه.
     */
    suspend fun dismissAlert(alertId: String): Result<Unit>
}
