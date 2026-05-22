package com.example.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

@Entity(tableName = "clips")
data class ClipItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rawText: String,
    val title: String,
    val tasksJson: String = "[]", // Serialized List<TaskItem>
    val datesJson: String = "[]", // Serialized List<String> index of dates
    val imageUri: String? = null,
    val pdfPath: String? = null,
    val folderName: String = "Bandeja de entrada", // Default "Inbox" in Spanish
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val themeColorHex: String = "#FFFDFC", // Off-white modern starting color
    val isProTemplate: Boolean = false
)
