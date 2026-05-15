package com.myimdad_por.core.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.Certificate

/**
 * Certificate pinning helper for OkHttp.
 *
 * - Uses SHA-256 pins only.
 * - Supports multiple pins per host so you can keep a backup pin.
 * - Can be disabled at runtime through a kill switch.
 *
 * Keep Network Security Configuration in XML as the first line of defense,
 * and use this class as the OkHttp-specific layer.
 */
object CertificatePinning {

    data class HostPins(
        val host: String,
        val pins: List<String>
    ) {
        init {
            require(host.isNotBlank()) { "host must not be blank." }
            require(pins.isNotEmpty()) { "pins must not be empty." }
            require(pins.size >= 2) {
                "Each host should have at least one backup pin."
            }
        }
    }

    /**
     * Builds an OkHttp [CertificatePinner].
     *
     * When [enabled] is false, an empty pinner is returned.
     */
    fun build(
        hostPins: List<HostPins>,
        enabled: Boolean = true
    ): CertificatePinner {
        if (!enabled || hostPins.isEmpty()) {
            return CertificatePinner.Builder().build()
        }

        val builder = CertificatePinner.Builder()

        hostPins.forEach { entry ->
            entry.pins.forEach { pin ->
                builder.add(entry.host, normalizeSha256Pin(pin))
            }
        }

        return builder.build()
    }

    /**
     * Applies the pinning configuration to an existing OkHttp client builder.
     */
    fun applyTo(
        builder: OkHttpClient.Builder,
        hostPins: List<HostPins>,
        enabled: Boolean = true
    ): OkHttpClient.Builder {
        if (!enabled || hostPins.isEmpty()) {
            return builder
        }

        return builder.certificatePinner(build(hostPins, enabled = true))
    }

    /**
     * Returns the SHA-256 pin string for a certificate.
     * Example format: sha256/BASE64...
     */
    fun sha256Pin(certificate: Certificate): String {
        return CertificatePinner.pin(certificate)
    }

    /**
     * Converts a raw pin or a pin prefixed with sha256/ into the format expected by OkHttp.
     *
     * SHA-1 pins are intentionally rejected.
     */
    fun normalizeSha256Pin(pin: String): String {
        require(pin.isNotBlank()) { "pin must not be blank." }

        val trimmed = pin.trim()

        return when {
            trimmed.startsWith("sha256/", ignoreCase = true) -> {
                "sha256/" + trimmed.removePrefix("sha256/").removePrefix("SHA256/")
            }

            trimmed.startsWith("sha1/", ignoreCase = true) -> {
                throw IllegalArgumentException("SHA-1 pins are not allowed. Use SHA-256 pins.")
            }

            else -> "sha256/$trimmed"
        }
    }
}