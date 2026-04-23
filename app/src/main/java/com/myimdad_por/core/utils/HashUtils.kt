package com.myimdad_por.core.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * أدوات التجزئة والتوقيع بشكل احترافي وآمن.
 *
 * ملاحظات مهمة:
 * - SHA-256 / SHA-512 مناسبة للبصمات والتأكد من سلامة البيانات.
 * - لا يُنصح باستخدامها لتخزين كلمات المرور مباشرة.
 * - للتحقق من كلمات المرور استخدم خوارزميات مخصصة مثل bcrypt أو PBKDF2 أو Argon2.
 */
object HashUtils {

    private const val SHA_256 = "SHA-256"
    private const val SHA_512 = "SHA-512"
    private const val SHA_1 = "SHA-1"
    private const val MD5 = "MD5"
    private const val HMAC_SHA_256 = "HmacSHA256"
    private const val HMAC_SHA_512 = "HmacSHA512"

    private const val DEFAULT_SALT_LENGTH = 16
    private const val HASH_SEPARATOR = ":"

    private val secureRandom = SecureRandom()

    /**
     * إنشاء SHA-256 من النص.
     */
    fun sha256(input: String): String {
        return digest(input, SHA_256)
    }

    /**
     * إنشاء SHA-512 من النص.
     */
    fun sha512(input: String): String {
        return digest(input, SHA_512)
    }

    /**
     * إنشاء SHA-1 من النص.
     *
     * لا يُستخدم للأمان الحساس، وإنما فقط عند الحاجة للتوافق مع أنظمة قديمة.
     */
    fun sha1(input: String): String {
        return digest(input, SHA_1)
    }

    /**
     * إنشاء MD5 من النص.
     *
     * غير مناسب للأمان، ويُستخدم فقط في الحالات غير الحساسة أو للتوافق القديم.
     */
    fun md5(input: String): String {
        return digest(input, MD5)
    }

    /**
     * إنشاء HMAC-SHA256 باستخدام مفتاح سري.
     */
    fun hmacSha256(input: String, secretKey: String): String {
        return hmac(input, secretKey, HMAC_SHA_256)
    }

    /**
     * إنشاء HMAC-SHA512 باستخدام مفتاح سري.
     */
    fun hmacSha512(input: String, secretKey: String): String {
        return hmac(input, secretKey, HMAC_SHA_512)
    }

    /**
     * إنشاء Salt عشوائي بصيغة hexadecimal.
     */
    fun generateSalt(length: Int = DEFAULT_SALT_LENGTH): String {
        require(length > 0) { "Salt length must be greater than zero." }

        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes.toHexString()
    }

    /**
     * إنشاء hash مملح باستخدام SHA-256.
     *
     * الصيغة الناتجة:
     * salt:hash
     */
    fun sha256WithSalt(input: String, salt: String = generateSalt()): String {
        val normalizedSalt = salt.trim()
        require(normalizedSalt.isNotEmpty()) { "Salt must not be blank." }

        val hash = sha256("$normalizedSalt$HASH_SEPARATOR$input")
        return "$normalizedSalt$HASH_SEPARATOR$hash"
    }

    /**
     * التحقق من قيمة مطابقة لـ SHA-256 مع Salt.
     */
    fun verifySha256WithSalt(input: String, storedValue: String): Boolean {
        val parts = storedValue.split(HASH_SEPARATOR, limit = 2)
        if (parts.size != 2) return false

        val salt = parts[0]
        val expectedHash = parts[1]

        if (salt.isBlank() || expectedHash.isBlank()) return false

        val actualHash = sha256("$salt$HASH_SEPARATOR$input")
        return constantTimeEquals(expectedHash, actualHash)
    }

    /**
     * إنشاء digest عام حسب الخوارزمية المطلوبة.
     */
    fun digest(input: String, algorithm: String): String {
        val messageDigest = MessageDigest.getInstance(algorithm)
        val bytes = messageDigest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.toHexString()
    }

    /**
     * مقارنة ثابتة الزمن قدر الإمكان لتقليل مخاطر timing attacks.
     */
    fun constantTimeEquals(first: String, second: String): Boolean {
        val a = first.toByteArray(StandardCharsets.UTF_8)
        val b = second.toByteArray(StandardCharsets.UTF_8)

        var result = a.size xor b.size
        val minLength = minOf(a.size, b.size)

        for (i in 0 until minLength) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }

        return result == 0
    }

    /**
     * تحويل ByteArray إلى hexadecimal.
     */
    fun ByteArray.toHexString(): String {
        val hexChars = CharArray(size * 2)
        var index = 0

        for (byte in this) {
            val value = byte.toInt() and 0xFF
            hexChars[index++] = HEX_ARRAY[value ushr 4]
            hexChars[index++] = HEX_ARRAY[value and 0x0F]
        }

        return String(hexChars)
    }

    /**
     * إنشاء HMAC بالخوارزمية المطلوبة.
     */
    private fun hmac(input: String, secretKey: String, algorithm: String): String {
        require(secretKey.isNotBlank()) { "Secret key must not be blank." }

        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), algorithm)
        mac.init(keySpec)

        val bytes = mac.doFinal(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.toHexString()
    }

    private val HEX_ARRAY = "0123456789abcdef".toCharArray()
}