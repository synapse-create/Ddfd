package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.models.TaskItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val taskListType = Types.newParameterizedType(List::class.java, TaskItem::class.java)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)
    
    private val taskAdapter = moshi.adapter<List<TaskItem>>(taskListType)
    private val stringAdapter = moshi.adapter<List<String>>(stringListType)

    @TypeConverter
    fun fromTaskList(tasks: List<TaskItem>?): String {
        return taskAdapter.toJson(tasks ?: emptyList())
    }

    @TypeConverter
    fun toTaskList(json: String?): List<TaskItem> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            taskAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromStringList(strings: List<String>?): String {
        return stringAdapter.toJson(strings ?: emptyList())
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            stringAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
