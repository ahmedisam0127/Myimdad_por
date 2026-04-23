package com.myimdad_por.core.network

import okhttp3.CertificatePinner

/**
 * 🔐 Certificate Pinning Configuration (Bank-Level Security)
 *
 * هذا الكلاس مسؤول عن:
 * - تعريف الـ Pins بشكل آمن
 * - منع الأخطاء البشرية (Misconfiguration)
 * - دعم rotation و backup pins
 *
 * ⚠️ مهم جداً:
 * - يجب إضافة Backup Pin واحد على الأقل لكل Host
 * - لا تستخدم هذا الكلاس مع Third-party APIs
 */
object CertificatePinningConfig {

    /**
     * تعريف Pins لكل Host
     *
     * القاعدة:
     * - على الأقل 2 Pins لكل Host (Primary + Backup)
     * - استخدم SHA-256 فقط
     */
    private val pinConfig: Map<String, Set<String>> = mapOf(

        // مثال:
        // "api.imdad.com" to setOf(
        //     "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        //     "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        // )

    )

    /**
     * إنشاء CertificatePinner جاهز للاستخدام
     */
    fun create(): CertificatePinner {
        if (pinConfig.isEmpty()) {
            throw IllegalStateException(
                "Certificate pinning is enabled but no pins are configured."
            )
        }

        val builder = CertificatePinner.Builder()

        pinConfig.forEach { (host, pins) ->
            validateHost(host)
            validatePins(host, pins)

            pins.forEach { pin ->
                builder.add(host, pin)
            }
        }

        return builder.build()
    }

    /**
     * هل التفعيل موجود؟
     */
    fun isEnabled(): Boolean = pinConfig.isNotEmpty()

    /**
     * التحقق من صحة الـ Host
     */
    private fun validateHost(host: String) {
        require(host.isNotBlank()) {
            "Host must not be blank."
        }

        require(!host.startsWith("http")) {
            "Host must not contain scheme (http/https): $host"
        }
    }

    /**
     * التحقق من الـ Pins
     */
    private fun validatePins(host: String, pins: Set<String>) {
        require(pins.isNotEmpty()) {
            "No pins provided for host: $host"
        }

        require(pins.size >= 2) {
            """
            Security Warning:
            Host [$host] must have at least TWO pins (primary + backup).
            Otherwise the app may break on certificate rotation.
            """.trimIndent()
        }

        pins.forEach { pin ->
            require(pin.startsWith("sha256/")) {
                "Invalid pin format for host [$host]: $pin"
            }

            require(pin.length > 10) {
                "Pin too short for host [$host]"
            }
        }
    }
}