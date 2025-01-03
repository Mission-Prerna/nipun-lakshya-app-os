package com.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.data.db.models.Insight
import com.data.models.submissions.SubmitResultsModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

@ProvidedTypeConverter
class Convertors {

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }

    @TypeConverter
    fun toList(value: String?): MutableList<String> {
        val listType = object : TypeToken<MutableList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: MutableList<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromSubmitResultsModel(value: SubmitResultsModel): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toSubmitResultsModel(value: String): SubmitResultsModel {
        return Gson().fromJson(value, object : TypeToken<SubmitResultsModel>() {}.type)
    }

    @TypeConverter
    fun fromIntList(grades: List<Int>): String {
        return grades.joinToString(",")
    }

    @TypeConverter
    fun toIntList(gradesString: String): List<Int> {
        return gradesString.split(",").map { it.toInt() }
    }

    @TypeConverter
    fun fromInsightsList(insightsList: List<Insight>): String {
        return Gson().toJson(insightsList)
    }

    @TypeConverter
    fun toInsightsList(insightsListJson: String): List<Insight> {
        val type = object : TypeToken<List<Insight>>() {}.type
        return Gson().fromJson(insightsListJson, type)
    }

}
