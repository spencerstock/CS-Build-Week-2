package com.spencerstock.lambdahunt.Room

import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson


class ListConverter {


    @TypeConverter
    fun fromStringList(someList: List<String>?): String? {
        if (someList == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {

        }.type
        return gson.toJson(someList, type)
    }

    @TypeConverter
    fun toStringList(string: String?): List<String>? {
        if (string == null) {
            return null
        }
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {

        }.type
        return gson.fromJson(string, type)
    }

}