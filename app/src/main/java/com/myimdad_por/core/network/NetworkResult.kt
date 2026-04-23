package com.myimdad_por.core.network

/**
 * نتيجة موحّدة لعمليات الشبكة.
 *
 * التصميم:
 * - Success يحمل البيانات
 * - Error يحمل ApiException
 * - Loading يمثل حالة التحميل
 *
 * هذه النسخة مصممة لتكون:
 * - آمنة نوعيًا
 * - سهلة الاستخدام مع ViewModel وRepository
 * - خالية من خطأ التباين الذي ظهر أثناء البناء
 */
sealed class NetworkResult<out T> {

    data class Success<out T>(
        val data: T
    ) : NetworkResult<T>()

    data class Error(
        val exception: ApiException
    ) : NetworkResult<Nothing>()

    data object Loading : NetworkResult<Nothing>()

    val isSuccess: Boolean
        get() = this is Success<*>

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
        Loading -> null
    }

    fun exceptionOrNull(): ApiException? = when (this) {
        is Error -> exception
        is Success -> null
        Loading -> null
    }

    /**
     * إرجاع البيانات عند النجاح،
     * أو القيمة الافتراضية الجاهزة عند الفشل/التحميل.
     *
     * ملاحظة:
     * استخدم قيمة جاهزة بدل lambda حتى يبقى التباين النوعي صحيحًا.
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = when (this) {
        is Success -> data
        is Error -> defaultValue
        Loading -> defaultValue
    }

    /**
     * تحويل البيانات عند النجاح.
     */
    inline fun <R> map(transform: (T) -> R): NetworkResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(exception)
            Loading -> Loading
        }
    }

    /**
     * تحويل النجاح إلى نتيجة جديدة.
     */
    inline fun <R> flatMap(transform: (T) -> NetworkResult<R>): NetworkResult<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> Error(exception)
            Loading -> Loading
        }
    }

    /**
     * تنفيذ callback عند النجاح.
     */
    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    /**
     * تنفيذ callback عند الخطأ.
     */
    inline fun onError(action: (ApiException) -> Unit): NetworkResult<T> {
        if (this is Error) action(exception)
        return this
    }

    /**
     * تنفيذ callback عند التحميل.
     */
    inline fun onLoading(action: () -> Unit): NetworkResult<T> {
        if (this is Loading) action()
        return this
    }

    /**
     * دالة شاملة للتعامل مع الحالات الثلاث.
     */
    inline fun <R> fold(
        onSuccess: (T) -> R,
        onError: (ApiException) -> R,
        onLoading: () -> R
    ): R {
        return when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(exception)
            Loading -> onLoading()
        }
    }

    companion object {
        fun <T> success(data: T): NetworkResult<T> = Success(data)

        fun error(exception: ApiException): NetworkResult<Nothing> = Error(exception)

        fun loading(): NetworkResult<Nothing> = Loading
    }
}

/**
 * تحويل أي قيمة إلى نتيجة نجاح.
 */
fun <T> T.asSuccess(): NetworkResult<T> = NetworkResult.Success(this)

/**
 * تحويل ApiException إلى نتيجة خطأ.
 */
fun ApiException.asError(): NetworkResult.Error = NetworkResult.Error(this)