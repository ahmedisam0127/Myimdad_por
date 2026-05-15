package com.myimdad_por.core.network

import java.io.IOException

/**
 * استثناء موحّد لطبقة الشبكة.
 *
 * الهدف منه:
 * - توحيد التعامل مع أخطاء API
 * - تمييز الأخطاء حسب النوع أو الكود
 * - تمرير رسالة مناسبة لواجهة المستخدم
 */
open class ApiException(
    val code: Int? = null,
    override val message: String? = null,
    val userMessage: String? = message,
    cause: Throwable? = null
) : IOException(message, cause) {

    override fun toString(): String {
        return buildString {
            append("ApiException(")
            append("code=").append(code)
            append(", message=").append(message)
            append(", userMessage=").append(userMessage)
            append(")")
        }
    }

    companion object {

        /**
         * خطأ غير متوقع من السيرفر أو طبقة الشبكة.
         */
        fun unexpected(
            message: String = "حدث خطأ غير متوقع",
            cause: Throwable? = null
        ): ApiException {
            return ApiException(
                code = null,
                message = message,
                userMessage = message,
                cause = cause
            )
        }

        /**
         * خطأ اتصال.
         */
        fun network(
            message: String = "تحقق من اتصال الإنترنت",
            cause: Throwable? = null
        ): ApiException {
            return ApiException(
                code = null,
                message = message,
                userMessage = message,
                cause = cause
            )
        }

        /**
         * خطأ مهلة زمنية.
         */
        fun timeout(
            message: String = "انتهت مهلة الاتصال",
            cause: Throwable? = null
        ): ApiException {
            return ApiException(
                code = null,
                message = message,
                userMessage = message,
                cause = cause
            )
        }

        /**
         * خطأ مصادقة.
         */
        fun unauthorized(
            message: String = "غير مصرح بالدخول",
            cause: Throwable? = null
        ): ApiException {
            return ApiException(
                code = 401,
                message = message,
                userMessage = message,
                cause = cause
            )
        }

        /**
         * خطأ حظر الوصول.
         */
        fun forbidden(
            message: String = "لا تملك صلاحية الوصول",
            cause: Throwable? = null
        ): ApiException {
            return ApiException(
                code = 403,
                message = message,
                userMessage = message,
                cause = cause
            )
        }

        /**
         * خطأ منطق الطلب أو البيانات.
         */
        fun badRequest(
            message: String = "الطلب غير صحيح",
            cause: Throwable? = null
        ): ApiException {
            return ApiException(
                code = 400,
                message = message,
                userMessage = message,
                cause = cause
            )
        }

        /**
         * خطأ من السيرفر.
         */
        fun serverError(
            message: String = "حدث خطأ في الخادم",
            cause: Throwable? = null
        ): ApiException {
            return ApiException(
                code = 500,
                message = message,
                userMessage = message,
                cause = cause
            )
        }
    }
}

/**
 * استثناء مخصص عندما يكون الاتصال بالإنترنت غير متاح.
 */
class NetworkUnavailableException(
    message: String = "لا يوجد اتصال بالإنترنت",
    cause: Throwable? = null
) : ApiException(
    code = null,
    message = message,
    userMessage = message,
    cause = cause
)

/**
 * استثناء مخصص لانتهاء المهلة.
 */
class RequestTimeoutException(
    message: String = "انتهت مهلة الطلب",
    cause: Throwable? = null
) : ApiException(
    code = null,
    message = message,
    userMessage = message,
    cause = cause
)

/**
 * استثناء مخصص لرمز HTTP غير المتوقع.
 */
class HttpApiException(
    code: Int,
    message: String,
    userMessage: String? = message,
    cause: Throwable? = null
) : ApiException(
    code = code,
    message = message,
    userMessage = userMessage,
    cause = cause
)