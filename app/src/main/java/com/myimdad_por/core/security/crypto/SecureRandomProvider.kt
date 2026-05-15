package com.myimdad_por.core.security.crypto

import java.security.SecureRandom
import kotlin.math.abs

/**
 * Centralized source for cryptographically secure random values.
 *
 * Use this provider anywhere the app needs:
 * - tokens
 * - nonces
 * - salts
 * - unpredictable identifiers
 */
object SecureRandomProvider {

    private const val DEFAULT_TOKEN_SIZE = 32

    private const val DEFAULT_ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    private val random: SecureRandom by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SecureRandom()
    }

    /**
     * Returns a new byte array filled with secure random data.
     */
    fun nextBytes(size: Int): ByteArray {
        require(size > 0) { "size must be greater than 0." }
        return ByteArray(size).also { random.nextBytes(it) }
    }

    /**
     * Fills the given array with secure random data and returns it.
     */
    fun fill(target: ByteArray): ByteArray {
        require(target.isNotEmpty()) { "target must not be empty." }
        random.nextBytes(target)
        return target
    }

    /**
     * Returns a secure random Int.
     */
    fun nextInt(): Int = random.nextInt()

    /**
     * Returns a secure random Int in the range [0, bound).
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be greater than 0." }
        return random.nextInt(bound)
    }

    /**
     * Returns a secure random Long.
     */
    fun nextLong(): Long = random.nextLong()

    /**
     * Returns a secure random Boolean.
     */
    fun nextBoolean(): Boolean = random.nextBoolean()

    /**
     * Generates a hexadecimal token from secure random bytes.
     */
    fun token(byteCount: Int = DEFAULT_TOKEN_SIZE): String {
        require(byteCount > 0) { "byteCount must be greater than 0." }
        return nextBytes(byteCount).toHexString()
    }

    /**
     * Generates a random string using the provided alphabet.
     * This is suitable for nonces, short codes, and identifiers.
     */
    fun randomString(
        length: Int,
        alphabet: String = DEFAULT_ALPHABET
    ): String {
        require(length > 0) { "length must be greater than 0." }
        require(alphabet.isNotEmpty()) { "alphabet must not be empty." }

        val builder = StringBuilder(length)
        repeat(length) {
            builder.append(alphabet[nextInt(alphabet.length)])
        }
        return builder.toString()
    }

    private fun ByteArray.toHexString(): String {
        val hexChars = CharArray(size * 2)
        var index = 0

        for (byte in this) {
            val value = byte.toInt() and 0xFF
            hexChars[index++] = HEX_CHARS[value ushr 4]
            hexChars[index++] = HEX_CHARS[value and 0x0F]
        }

        return String(hexChars)
    }

    private val HEX_CHARS = "0123456789abcdef".toCharArray()
}