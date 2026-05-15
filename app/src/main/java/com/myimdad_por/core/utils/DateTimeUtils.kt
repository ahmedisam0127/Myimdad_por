package com.myimdad_por.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * أدوات التاريخ والوقت الخاصة بالتطبيق.
 *
 * تعتمد على java.time وتناسب Android الحديث مع تفعيل desugaring.
 */
object DateTimeUtils {

    private const val PATTERN_DATE_TIME = "yyyy-MM-dd HH:mm:ss"
    private const val PATTERN_DATE = "yyyy-MM-dd"
    private const val PATTERN_DISPLAY_DATE = "dd/MM/yyyy"
    private const val PATTERN_DISPLAY_DATE_TIME = "dd/MM/yyyy HH:mm"
    private const val PATTERN_TIME = "HH:mm"

    private val defaultLocale: Locale = Locale.getDefault()
    private val defaultZoneId: ZoneId = ZoneId.systemDefault()

    private val defaultDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_DATE_TIME, defaultLocale)

    private val defaultDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_DATE, defaultLocale)

    private val displayDateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_DISPLAY_DATE, defaultLocale)

    private val displayDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_DISPLAY_DATE_TIME, defaultLocale)

    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern(PATTERN_TIME, defaultLocale)

    /**
     * الوقت الحالي كـ [LocalDateTime].
     */
    fun now(): LocalDateTime = LocalDateTime.now(defaultZoneId)

    /**
     * اليوم الحالي كـ [LocalDate].
     */
    fun today(): LocalDate = LocalDate.now(defaultZoneId)

    /**
     * الوقت الحالي كـ [Instant].
     */
    fun nowInstant(): Instant = Instant.now()

    /**
     * بداية اليوم الحالي.
     */
    fun startOfToday(): LocalDateTime = today().atStartOfDay()

    /**
     * نهاية اليوم الحالي بدقة عالية.
     */
    fun endOfToday(): LocalDateTime = today().atTime(LocalTime.MAX)

    /**
     * تنسيق [LocalDateTime] إلى نص.
     */
    fun formatDateTime(
        dateTime: LocalDateTime,
        pattern: String = PATTERN_DATE_TIME,
        locale: Locale = defaultLocale
    ): String {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    /**
     * تنسيق [LocalDate] إلى نص.
     */
    fun formatDate(
        date: LocalDate,
        pattern: String = PATTERN_DATE,
        locale: Locale = defaultLocale
    ): String {
        return date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    /**
     * تنسيق التاريخ والوقت بشكل مناسب للعرض.
     */
    fun formatForDisplay(dateTime: LocalDateTime): String {
        return dateTime.format(displayDateTimeFormatter)
    }

    /**
     * تنسيق التاريخ فقط بشكل مناسب للعرض.
     */
    fun formatDateForDisplay(date: LocalDate): String {
        return date.format(displayDateFormatter)
    }

    /**
     * تنسيق الوقت فقط.
     */
    fun formatTime(dateTime: LocalDateTime): String {
        return dateTime.format(timeFormatter)
    }

    /**
     * تحويل نص إلى [LocalDateTime].
     * يحاول أولاً النمط المخصص، ثم ISO-8601 كخيار احتياطي.
     */
    fun parseDateTime(
        value: String,
        pattern: String = PATTERN_DATE_TIME,
        locale: Locale = defaultLocale
    ): LocalDateTime? {
        if (value.isBlank()) return null

        return try {
            LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern, locale))
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /**
     * تحويل نص إلى [LocalDate].
     * يحاول أولاً النمط المخصص، ثم ISO-8601 كخيار احتياطي.
     */
    fun parseDate(
        value: String,
        pattern: String = PATTERN_DATE,
        locale: Locale = defaultLocale
    ): LocalDate? {
        if (value.isBlank()) return null

        return try {
            LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern, locale))
        } catch (_: DateTimeParseException) {
            try {
                LocalDate.parse(value, DateTimeFormatter.ISO_DATE)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /**
     * تحويل [Instant] إلى [LocalDateTime] حسب المنطقة الزمنية الحالية.
     */
    fun instantToLocalDateTime(instant: Instant): LocalDateTime {
        return LocalDateTime.ofInstant(instant, defaultZoneId)
    }

    /**
     * تحويل [LocalDateTime] إلى [Instant].
     */
    fun localDateTimeToInstant(dateTime: LocalDateTime): Instant {
        return dateTime.atZone(defaultZoneId).toInstant()
    }

    /**
     * تحويل [LocalDateTime] إلى نص ISO-8601.
     */
    fun toIsoString(dateTime: LocalDateTime): String {
        return dateTime.atZone(defaultZoneId).format(DateTimeFormatter.ISO_DATE_TIME)
    }

    /**
     * تحويل [LocalDate] إلى نص ISO-8601.
     */
    fun toIsoString(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_DATE)
    }

    /**
     * التحقق من صحة التاريخ النصي.
     */
    fun isValidDate(
        value: String,
        pattern: String = PATTERN_DATE,
        locale: Locale = defaultLocale
    ): Boolean {
        return parseDate(value, pattern, locale) != null
    }

    /**
     * التحقق من صحة التاريخ والوقت النصي.
     */
    fun isValidDateTime(
        value: String,
        pattern: String = PATTERN_DATE_TIME,
        locale: Locale = defaultLocale
    ): Boolean {
        return parseDateTime(value, pattern, locale) != null
    }

    /**
     * إضافة أيام إلى تاريخ/وقت.
     */
    fun plusDays(dateTime: LocalDateTime, days: Long): LocalDateTime {
        return dateTime.plusDays(days)
    }

    /**
     * طرح أيام من تاريخ/وقت.
     */
    fun minusDays(dateTime: LocalDateTime, days: Long): LocalDateTime {
        return dateTime.minusDays(days)
    }

    /**
     * الحصول على الطابع الزمني الحالي بالميلي ثانية.
     */
    fun currentTimeMillis(): Long = System.currentTimeMillis()
}