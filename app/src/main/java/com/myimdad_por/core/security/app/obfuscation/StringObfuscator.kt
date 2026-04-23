package com.myimdad_por.core.security.app.obfuscation

import com.myimdad_por.core.security.crypto.SecureRandomProvider
import java.nio.charset.Charset

/**
 * تمويه بسيط للنصوص الحساسة:
 * - XOR مع Mask
 * - دعم تجميع الأجزاء
 * - دعم توليد بيانات مموهة وقت البناء أو الاختبار
 *
 * ملاحظة:
 * هذا تمويه وليس تشفيرًا فعليًا.
 */
object StringObfuscator {

    private val DEFAULT_MASK = byteArrayOf(
        0x4A, 0x1B, 0x3F, 0x72, 0x09, 0x55, 0x2C, 0x6E
    )

    private const val DEFAULT_CHARSET_NAME = "UTF-8"

    private fun charset(): Charset = Charset.forName(DEFAULT_CHARSET_NAME)

    /**
     * يفك تمويه ByteArray باستخدام XOR.
     */
    @JvmStatic
    fun decode(obfuscated: ByteArray, mask: ByteArray = DEFAULT_MASK): String {
        require(obfuscated.isNotEmpty()) { "obfuscated must not be empty." }
        require(mask.isNotEmpty()) { "mask must not be empty." }

        val result = ByteArray(obfuscated.size)
        for (i in obfuscated.indices) {
            result[i] = (obfuscated[i].toInt() xor mask[i % mask.size].toInt()).toByte()
        }
        return String(result, charset())
    }

    /**
     * يموّه String إلى ByteArray باستخدام XOR.
     * مفيد للاختبار أو لتجهيز القيم قبل لصقها في الكود.
     */
    @JvmStatic
    fun encode(plainText: String, mask: ByteArray = DEFAULT_MASK): ByteArray {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        require(mask.isNotEmpty()) { "mask must not be empty." }

        val source = plainText.toByteArray(charset())
        val result = ByteArray(source.size)

        for (i in source.indices) {
            result[i] = (source[i].toInt() xor mask[i % mask.size].toInt()).toByte()
        }
        return result
    }

    /**
     * يفك تمويه نصوص مجزأة ثم يعيد دمجها.
     */
    @JvmStatic
    fun decodeParts(
        parts: List<ByteArray>,
        mask: ByteArray = DEFAULT_MASK,
        separator: String = ""
    ): String {
        require(parts.isNotEmpty()) { "parts must not be empty." }
        require(mask.isNotEmpty()) { "mask must not be empty." }

        return parts.joinToString(separator = separator) { decode(it, mask) }
    }

    /**
     * نسخة vararg من decodeParts.
     */
    @JvmStatic
    fun decodeParts(
        vararg parts: ByteArray,
        mask: ByteArray = DEFAULT_MASK,
        separator: String = ""
    ): String {
        return decodeParts(parts.toList(), mask, separator)
    }

    /**
     * يموّه نصًا إلى أجزاء أصغر.
     * هذا لا يُستخدم غالبًا في وقت التشغيل، لكنه مفيد لتوليد بيانات مموهة للاستخدام في الكود.
     */
    @JvmStatic
    fun splitAndEncode(
        plainText: String,
        chunkSize: Int = 4,
        mask: ByteArray = DEFAULT_MASK
    ): List<ByteArray> {
        require(plainText.isNotEmpty()) { "plainText must not be empty." }
        require(chunkSize > 0) { "chunkSize must be greater than zero." }
        require(mask.isNotEmpty()) { "mask must not be empty." }

        return plainText
            .chunked(chunkSize)
            .map { encode(it, mask) }
    }

    /**
     * يولّد مفتاح Mask عشوائي لاستخدامه مع نصوص جديدة.
     */
    @JvmStatic
    fun generateMask(size: Int = 8): ByteArray {
        require(size > 0) { "size must be greater than zero." }
        return SecureRandomProvider.nextBytes(size)
    }

    /**
     * مثال عام: بناء نص من ByteArrays مموهة.
     * لا تضع أسرارًا صريحة هنا؛ استخدم ناتج encode بدلًا من ذلك.
     */
    @JvmStatic
    fun buildString(
        vararg obfuscatedParts: ByteArray,
        mask: ByteArray = DEFAULT_MASK,
        separator: String = ""
    ): String {
        return decodeParts(*obfuscatedParts, mask = mask, separator = separator)
    }

    /**
     * يفرغ محتوى ByteArray حساس بعد الاستخدام.
     */
    @JvmStatic
    fun wipe(bytes: ByteArray?) {
        if (bytes == null) return
        bytes.fill(0)
    }
}