package com.myimdad_por.data.local.converters

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.json.JSONArray
import org.json.JSONObject

class AppConverters {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.setScale(SCALE, RoundingMode.HALF_UP)?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            BigDecimal(value.trim()).setScale(SCALE, RoundingMode.HALF_UP)
        }.getOrNull()
    }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): Long? {
        return value?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun toLocalDateTime(value: Long?): LocalDateTime? {
        return value?.let {
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it),
                ZoneId.systemDefault()
            )
        }
    }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): Long? {
        return value?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? {
        return value?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.let { JSONObject(it).toString() }
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String> {
        if (value.isNullOrBlank()) return emptyMap()
        return runCatching {
            val json = JSONObject(value)
            buildMap {
                json.keys().forEach { key ->
                    put(key, json.optString(key))
                }
            }
        }.getOrDefault(emptyMap())
    }

    @TypeConverter
    fun fromStringSet(value: Set<String>?): String? {
        return value?.let {
            JSONArray().apply { it.forEach(::put) }.toString()
        }
    }

    @TypeConverter
    fun toStringSet(value: String?): Set<String> {
        if (value.isNullOrBlank()) return emptySet()
        return runCatching {
            val array = JSONArray(value)
            buildSet {
                for (i in 0 until array.length()) {
                    val item = array.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
        }.getOrDefault(emptySet())
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let {
            JSONArray().apply { it.forEach(::put) }.toString()
        }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optString(i)
                    if (item.isNotBlank()) add(item)
                }
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val SCALE = 2
    }
}