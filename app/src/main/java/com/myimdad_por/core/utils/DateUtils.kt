package com.myimdad_por.core.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * DateUtils
 *
 * كلاس مساعد مركزي لتنسيق التواريخ وتحويلها.
 * يُستخدم في كامل التطبيق لضمان الاتساق بين الشاشات.
 *
 * السبب: نقل [expiryDateFormatter] من InventoryViewModel
 * لتجنب تكرار التعريف في كل ViewModel يحتاج التعامل مع التواريخ.
 */
object DateUtils {

    /** الصيغة الرسمية لتاريخ انتهاء الصلاحية المستخدمة في كامل التطبيق */
    val expiryDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * تحويل نص إلى [LocalDate].
     * @return [LocalDate] إن كان النص صالحاً، أو null إن كان فارغاً أو غير صالح.
     */
    fun parseExpiryDate(value: String): LocalDate? {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return null
        return try {
            LocalDate.parse(trimmed, expiryDateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /**
     * تحويل [LocalDate] إلى نص بصيغة yyyy-MM-dd.
     * @return نص التاريخ، أو نص فارغ إن كانت القيمة null.
     */
    fun formatExpiryDate(date: LocalDate?): String {
        return date?.format(expiryDateFormatter) ?: ""
    }

    /**
     * التحقق من صلاحية نص التاريخ دون تحويله.
     * مفيد لإظهار رسالة خطأ فورية في الـ UI أثناء الكتابة.
     */
    fun isValidDateText(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return true // فارغ = مقبول (اختياري)
        return parseExpiryDate(trimmed) != null
    }
}
