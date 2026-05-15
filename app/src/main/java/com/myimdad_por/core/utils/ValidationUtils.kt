package com.myimdad_por.core.utils

import android.util.Patterns
import java.math.BigDecimal
import java.util.Locale

/**
 * أدوات التحقق من المدخلات (Validation).
 *
 * تحسينات:
 * - دوال صغيرة وواضحة
 * - دعم الأرقام العربية والهندية
 * - تقليل التكرار
 * - فحص أكثر دقة للمدخلات
 * - قابلية صيانة أعلى
 */
object ValidationUtils {

    private const val SUDANESE_LOCAL_PREFIX = "09"
    private const val SUDANESE_INTERNATIONAL_PREFIX = "+2499"
    private const val MIN_GENERIC_PHONE_LENGTH = 9
    private const val MAX_GENERIC_PHONE_LENGTH = 15

    private val whitespaceRegex = "\\s+".toRegex()

    private val arabicDigits = mapOf(
        '٠' to '0',
        '١' to '1',
        '٢' to '2',
        '٣' to '3',
        '٤' to '4',
        '٥' to '5',
        '٦' to '6',
        '٧' to '7',
        '٨' to '8',
        '٩' to '9',
        '۰' to '0',
        '۱' to '1',
        '۲' to '2',
        '۳' to '3',
        '۴' to '4',
        '۵' to '5',
        '۶' to '6',
        '۷' to '7',
        '۸' to '8',
        '۹' to '9'
    )

    /**
     * التحقق من أن النص غير فارغ.
     */
    fun isNotEmpty(value: String?): Boolean = !value.isNullOrBlank()

    /**
     * التحقق من البريد الإلكتروني.
     */
    fun isValidEmail(email: String?): Boolean {
        val normalized = email?.trim().orEmpty()
        return normalized.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(normalized).matches()
    }

    /**
     * التحقق من رقم الهاتف بشكل عام.
     * يسمح بـ + في البداية فقط.
     */
    fun isValidPhone(phone: String?): Boolean {
        val normalized = normalizePhone(phone)
        if (normalized.isEmpty()) return false

        val startsWithPlus = normalized.startsWith('+')
        val digitsOnly = if (startsWithPlus) normalized.drop(1) else normalized

        return digitsOnly.length in MIN_GENERIC_PHONE_LENGTH..MAX_GENERIC_PHONE_LENGTH &&
                digitsOnly.all(Char::isDigit) &&
                (!startsWithPlus || normalized.indexOf('+') == 0)
    }

    /**
     * التحقق من رقم سوداني.
     * أمثلة:
     * 0912345678
     * +249912345678
     */
    fun isSudanesePhone(phone: String?): Boolean {
        val normalized = normalizePhone(phone)
        if (normalized.isEmpty()) return false

        return normalized.matches(Regex("^09\\d{8}$")) ||
                normalized.matches(Regex("^\\+2499\\d{8}$"))
    }

    /**
     * التحقق من كلمة المرور.
     */
    fun isValidPassword(password: String?): Boolean {
        val value = password?.trim().orEmpty()
        return value.length >= Constants.Validation.MIN_PASSWORD_LENGTH
    }

    /**
     * التحقق من الاسم.
     */
    fun isValidName(name: String?): Boolean {
        val value = name?.trim().orEmpty()
        if (value.isEmpty()) return false
        if (value.length > Constants.Validation.MAX_NAME_LENGTH) return false

        return value.any { it.isLetter() }
    }

    /**
     * التحقق من المبلغ المالي.
     */
    fun isValidAmount(amount: BigDecimal?): Boolean {
        return amount != null && amount >= BigDecimal.ZERO
    }

    /**
     * التحقق من أن النص يحتوي على أرقام فقط.
     */
    fun isDigitsOnly(value: String?): Boolean {
        val normalized = normalizeNumbers(value).trim()
        return normalized.isNotEmpty() && normalized.all(Char::isDigit)
    }

    /**
     * تحويل الأرقام العربية والهندية إلى إنجليزية، مع إزالة المسافات.
     */
    fun normalizeNumbers(input: String?): String {
        if (input.isNullOrEmpty()) return ""

        return buildString(input.length) {
            input.forEach { char ->
                when {
                    char.isWhitespace() -> Unit
                    char in arabicDigits -> append(arabicDigits.getValue(char))
                    else -> append(char)
                }
            }
        }
    }

    /**
     * تنظيف رقم الهاتف وإزالة المسافات.
     */
    fun normalizePhone(input: String?): String {
        return normalizeNumbers(input)
            .replace(whitespaceRegex, "")
            .replace(" ", "")
            .trim()
    }

    /**
     * التحقق من الطول.
     */
    fun isInRange(
        value: String?,
        min: Int,
        max: Int
    ): Boolean {
        val text = value?.trim().orEmpty()
        return text.length in min..max
    }

    /**
     * مقارنة كلمتي المرور.
     */
    fun isSamePassword(
        password: String?,
        confirmPassword: String?
    ): Boolean {
        val first = password?.trim().orEmpty()
        val second = confirmPassword?.trim().orEmpty()

        return first.isNotEmpty() && first == second
    }

    /**
     * التحقق من URL.
     */
    fun isValidUrl(url: String?): Boolean {
        val normalized = url?.trim().orEmpty()
        return normalized.isNotEmpty() && Patterns.WEB_URL.matcher(normalized).matches()
    }

    /**
     * التحقق من ID.
     */
    fun isValidId(id: String?): Boolean {
        return id?.trim()?.length ?: 0 >= 3
    }

    /**
     * تحويل النص إلى صيغة آمنة للمقارنة.
     */
    fun normalizeText(value: String?): String {
        return value?.trim()?.lowercase(Locale.ROOT).orEmpty()
    }
}