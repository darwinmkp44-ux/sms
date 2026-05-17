package com.disparasms.app.data.local.converter

import androidx.room.TypeConverter
import org.json.JSONArray

class Converters {

    @TypeConverter
    fun fromJsonArray(value: String): List<Long> {
        val list = mutableListOf<Long>()
        val jsonArray = JSONArray(value)
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getLong(i))
        }
        return list
    }

    @TypeConverter
    fun toJsonArray(value: List<Long>): String {
        return JSONArray(value).toString()
    }
}
