package com.myimdad_por.data.remote.interceptor

import com.myimdad_por.core.network.CertificatePinningConfig
import com.myimdad_por.core.security.CertificatePinning
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.Locale

/**
 * طبقة حراسة إضافية فوق CertificatePinner.
 *
 * مبدأها:
 * - لا تسمح بالاتصال غير الآمن إلا إذا كان ذلك مصرحًا صراحةً.
 * - لا تسمح بالوصول إلى hosts غير مسموح بها عند تفعيل allow-list.
 * - يمكن استخدامها مع OkHttpClient.Builder بسهولة.
 *
 * ملاحظة مهمة:
 * شهادة pinning الحقيقية تتم عبر OkHttp CertificatePinner.
 * هذا الـ interceptor هنا يضيف سياسة تشغيلية واضحة ويمنع أي
 * طلب غير متوقع قبل الوصول إلى طبقة النقل.
 */
class CertificatePinningInterceptor(
    private val allowedCleartextHosts: Set<String> = emptySet(),
    private val allowedPinnedHosts: Set<String> = emptySet(),
    private val enforceHttps: Boolean = true
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val host = url.host.lowercase(Locale.ROOT)

        if (enforceHttps && !isHttps(url.scheme) && !isCleartextAllowed(host)) {
            throw IOException(
                "Cleartext traffic is not allowed for host: $host"
            )
        }

        if (allowedPinnedHosts.isNotEmpty() && host !in allowedPinnedHosts) {
            throw IOException(
                "Host is not allowed by pinning policy: $host"
            )
        }

        return chain.proceed(request)
    }

    private fun isHttps(scheme: String?): Boolean {
        return scheme?.equals("https", ignoreCase = true) == true
    }

    private fun isCleartextAllowed(host: String): Boolean {
        return allowedCleartextHosts.any { it.equals(host, ignoreCase = true) }
    }

    companion object {

        /**
         * يضيف سياسة الحراسة + شهادة pinning إلى الـ builder.
         */
        fun applyTo(
            builder: OkHttpClient.Builder,
            hostPins: List<CertificatePinning.HostPins> = emptyList(),
            allowedCleartextHosts: Set<String> = emptySet(),
            enforceHttps: Boolean = true
        ): OkHttpClient.Builder {
            val normalizedHosts = hostPins.map { it.host.lowercase(Locale.ROOT) }.toSet()

            if (hostPins.isNotEmpty()) {
                builder.certificatePinner(CertificatePinning.build(hostPins, enabled = true))
            } else if (CertificatePinningConfig.isEnabled()) {
                builder.certificatePinner(CertificatePinningConfig.create())
            }

            builder.addInterceptor(
                CertificatePinningInterceptor(
                    allowedCleartextHosts = allowedCleartextHosts,
                    allowedPinnedHosts = normalizedHosts,
                    enforceHttps = enforceHttps
                )
            )

            return builder
        }
    }
}