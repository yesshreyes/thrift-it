package com.example.thriftit.data.local

import android.net.Uri
import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {
    // ── String list ──────────────────────────────────────────────────────────

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value == null) return "[]"
        val jsonArray = JSONArray()
        value.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty() || value == "[]") return emptyList()
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(value)
            for (i in 0 until jsonArray.length()) list.add(jsonArray.getString(i))
        } catch (_: Exception) {}
        return list
    }

    // ── Uri list ─────────────────────────────────────────────────────────────

    private val DELIMITER = "|"

    @TypeConverter
    fun fromUriList(uris: List<Uri>?): String =
        uris?.joinToString(DELIMITER) { it.toString() } ?: ""

    @TypeConverter
    fun toUriList(value: String?): List<Uri> =
        if (value.isNullOrBlank()) emptyList()
        else value.split(DELIMITER).map { Uri.parse(it) }
}
