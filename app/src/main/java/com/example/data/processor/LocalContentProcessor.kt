package com.example.data.processor

import com.example.domain.models.TaskItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.regex.Pattern

object LocalContentProcessor {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val taskListType = Types.newParameterizedType(List::class.java, TaskItem::class.java)
    private val stringListType = Types.newParameterizedType(List::class.java, String::class.java)

    private val taskAdapter = moshi.adapter<List<TaskItem>>(taskListType)
    private val stringAdapter = moshi.adapter<List<String>>(stringListType)

    // A collection of gorgeous pastel minimal theme colors
    val BEAUTIFUL_CARD_COLORS = listOf(
        "#FFFDFC", // Sand
        "#F3F7F4", // Sage Soft
        "#F0F4F8", // Ocean Fog
        "#F5F3F7", // Pastel Lavender
        "#FDF8F5", // Warm Clay
        "#FFFDF0", // Solar Cream
        "#F1F6F9"  // Slate Light
    )

    fun processContent(rawText: String): ProcessedResult {
        if (rawText.isBlank()) {
            return ProcessedResult(
                title = "Nota Vacía",
                tasks = emptyList(),
                dates = emptyList(),
                themeColor = BEAUTIFUL_CARD_COLORS.first()
            )
        }

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        // 1. Title Extraction
        var title = ""
        for (line in lines) {
            val cleaned = line.replace(Regex("^#+\\s*"), "").replace(Regex("^[-*+]\\s*"), "")
            if (cleaned.isNotEmpty()) {
                title = if (cleaned.length > 40) {
                    cleaned.take(37) + "..."
                } else {
                    cleaned
                }
                break
            }
        }
        if (title.isEmpty()) title = "Nota Rápida"

        // 2. Task checklist extraction
        val tasks = mutableListOf<TaskItem>()
        // Match markers like "- [ ]", "[ ]", "- [x]", "[x]" or lines containing specific task verbs
        val taskMarkerPattern = Pattern.compile("^([\\-*+]\\s*)?\\[([ xX]?)\\]\\s*(.+)$")
        val verbPattern = Pattern.compile(
            "^(llevar|llamar|enviar|comprar|pagar|hacer|revisar|estudiar|llamar a|recordar|reunión|cita)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
        )

        for (line in lines) {
            val matcher = taskMarkerPattern.matcher(line)
            if (matcher.find()) {
                val isDone = matcher.group(2)?.trim()?.lowercase() == "x"
                val text = matcher.group(3) ?: ""
                if (text.isNotBlank()) {
                    tasks.add(TaskItem(text = text, isDone = isDone))
                }
            } else {
                // If the line starts with traditional bullet points or action verbs
                val bulletMatches = line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ")
                val cleanLine = line.replace(Regex("^[-*+]\\s*"), "")
                val verbMatcher = verbPattern.matcher(cleanLine)
                
                if (bulletMatches && cleanLine.length in 3..100) {
                    tasks.add(TaskItem(text = cleanLine, isDone = false))
                } else if (verbMatcher.find() && cleanLine.length in 5..100) {
                    tasks.add(TaskItem(text = cleanLine, isDone = false))
                }
            }
        }

        // 3. Date / Time extraction
        val dates = mutableListOf<String>()
        // Match DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD
        val datePattern = Pattern.compile("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{4})|(\\d{4}-\\d{2}-\\d{2})\\b")
        val timePattern = Pattern.compile("\\b(2[0-3]|[01]?[0-9]):([0-5][0-9])\\s*(am|pm|AM|PM)?\\b")
        
        val dateMatcher = datePattern.matcher(rawText)
        while (dateMatcher.find()) {
            val match = dateMatcher.group()
            if (!dates.contains(match)) {
                dates.add(match)
            }
        }

        val timeMatcher = timePattern.matcher(rawText)
        while (timeMatcher.find()) {
            val match = timeMatcher.group()
            if (!dates.contains(match)) {
                dates.add(match)
            }
        }

        // Keywords detection
        val lowercaseRaw = rawText.lowercase()
        val keywords = listOf("hoy", "mañana", "esta tarde", "este fin de semana", "lunes", "viernes")
        for (kw in keywords) {
            if (lowercaseRaw.contains(kw) && !dates.contains(kw)) {
                dates.add(kw.replaceFirstChar { it.uppercase() })
            }
        }

        // 4. Color picking is based on title length hash or random
        val colorIndex = title.hashCode().coerceAtLeast(0) % BEAUTIFUL_CARD_COLORS.size
        val themeColor = BEAUTIFUL_CARD_COLORS[colorIndex]

        return ProcessedResult(
            title = title,
            tasks = tasks,
            dates = dates,
            themeColor = themeColor
        )
    }

    fun serializeTasks(tasks: List<TaskItem>): String {
        return taskAdapter.toJson(tasks)
    }

    fun serializeDates(dates: List<String>): String {
        return stringAdapter.toJson(dates)
    }

    data class ProcessedResult(
        val title: String,
        val tasks: List<TaskItem>,
        val dates: List<String>,
        val themeColor: String
    )
}
