package com.lexapps.sblgntreader.data.db

import androidx.room.TypeConverter
import org.json.JSONObject

class VocabularyLevelsConverter {
    @TypeConverter
    fun decode(value: String): Map<String, Int> {
        val json = JSONObject(value)
        return json.keys().asSequence().associateWith { json.getInt(it) }
    }

    @TypeConverter
    fun encode(value: Map<String, Int>): String = JSONObject(value.toSortedMap()).toString()
}
