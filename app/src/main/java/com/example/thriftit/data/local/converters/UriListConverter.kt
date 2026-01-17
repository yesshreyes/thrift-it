package com.example.thriftit.data.local.converters

import android.net.Uri
import androidx.room.TypeConverter

class UriListConverter {
    private val DELIMITER = "|"

    @TypeConverter
    fun fromUriList(uris: List<Uri>?): String = uris?.joinToString(DELIMITER) { it.toString() } ?: ""

    @TypeConverter
    fun toUriList(value: String?): List<Uri> =
        if (value.isNullOrBlank()) {
            emptyList()
        } else {
            value.split(DELIMITER).map { Uri.parse(it) }
        }
}
