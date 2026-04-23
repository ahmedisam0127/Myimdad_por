package com.myimdad_por.core.network

import com.myimdad_por.core.utils.Constants
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * 🔐 SecureApiClient (Enterprise / Banking Grade)
 *
 * هذا الكلاس هو:
 * - Factory لبناء OkHttpClient آمن
 * - نقطة مركزية لكل إعدادات الشبكة
 *
 * الخصائص:
 * ✔ Secure by default
 * ✔ Extensible بدون تعديل
 * ✔ Debug-safe
 * ✔ Production hardened
 */
object SecureApiClient {

    fun create(
        certificatePinner: CertificatePinner? = null,
        interceptors: List<Interceptor> = emptyList(),
        networkInterceptors: List<Interceptor> = emptyList(),
        isDebug: Boolean = false
    ): OkHttpClient {

        return OkHttpClient.Builder()
            .applyBaseConfig()
            .applySecurity(certificatePinner)
            .applyHeaders()
            .applyInterceptors(interceptors)
            .applyNetworkInterceptors(networkInterceptors)
            .applyLogging(isDebug)
            .build()
    }

    // =========================================================
    // 🔹 Base Config (Timeouts + Protocols)
    // =========================================================
    private fun OkHttpClient.Builder.applyBaseConfig() = apply {

        connectTimeout(Constants.Network.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        readTimeout(Constants.Network.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        writeTimeout(Constants.Network.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        callTimeout(Constants.Network.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        retryOnConnectionFailure(true)

        /**
         * HTTP/2 + HTTP/1.1 fallback
         * تحسين الأداء عبر multiplexing
         */
        protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))

        /**
         * استخدام TLS حديث
         */
        connectionSpecs(
            listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS
            )
        )
    }

    // =========================================================
    // 🔐 Security Layer
    // =========================================================
    private fun OkHttpClient.Builder.applySecurity(
        certificatePinner: CertificatePinner?
    ) = apply {

        if (certificatePinner != null) {
            certificatePinner(certificatePinner)
        }
    }

    // =========================================================
    // 📡 Default Headers
    // =========================================================
    private fun OkHttpClient.Builder.applyHeaders() = apply {

        addInterceptor { chain ->
            val original = chain.request()

            val request = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header(
                    "User-Agent",
                    "${Constants.App.APP_NAME}/${Constants.App.PACKAGE_NAME}"
                )
                .build()

            chain.proceed(request)
        }
    }

    // =========================================================
    // 🔁 Custom Interceptors
    // =========================================================
    private fun OkHttpClient.Builder.applyInterceptors(
        interceptors: List<Interceptor>
    ) = apply {
        interceptors.forEach { addInterceptor(it) }
    }

    private fun OkHttpClient.Builder.applyNetworkInterceptors(
        interceptors: List<Interceptor>
    ) = apply {
        interceptors.forEach { addNetworkInterceptor(it) }
    }

    // =========================================================
    // 🐞 Logging (Debug Only)
    // =========================================================
    private fun OkHttpClient.Builder.applyLogging(
        isDebug: Boolean
    ) = apply {

        if (!isDebug) return@apply

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        addInterceptor(logging)
    }
}