package com.example.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.domain.models.ClipItem
import com.example.domain.models.TaskItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalExporter {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val taskListType = Types.newParameterizedType(List::class.java, TaskItem::class.java)
    private val taskAdapter = moshi.adapter<List<TaskItem>>(taskListType)

    fun exportAsCleanText(clip: ClipItem): String {
        val sb = StringBuilder()
        sb.append("✨ ").append(clip.title.uppercase(Locale.getDefault())).append(" ✨\n")
        sb.append("===================================\n\n")
        sb.append(clip.rawText).append("\n\n")

        val tasks = getTasks(clip.tasksJson)
        if (tasks.isNotEmpty()) {
            sb.append("TASKS:\n")
            tasks.forEach { task ->
                val box = if (task.isDone) "[✓]" else "[ ]"
                sb.append("$box ${task.text}\n")
            }
            sb.append("\n")
        }

        sb.append("-----------------------------------\n")
        sb.append("Creado con ClipLuz")
        return sb.toString()
    }

    fun exportAsMarkdownChecklist(clip: ClipItem): String {
        val sb = StringBuilder()
        sb.append("# ").append(clip.title).append("\n\n")
        sb.append("> ").append(clip.rawText.replace("\n", "\n> ")).append("\n\n")

        val tasks = getTasks(clip.tasksJson)
        if (tasks.isNotEmpty()) {
            sb.append("## Lista de Tareas\n\n")
            tasks.forEach { task ->
                val box = if (task.isDone) "- [x]" else "- [ ]"
                sb.append("$box ${task.text}\n")
            }
            sb.append("\n")
        }

        sb.append("---\n")
        sb.append("*Creado con ClipLuz*")
        return sb.toString()
    }

    fun exportAsBeautifulImage(context: Context, clip: ClipItem, showWatermark: Boolean = true): Uri? {
        val width = 1080
        val height = 1350 // Story/Instagram/Portrait ratio
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Decode the background color of the clip
        val bgColor = try {
            Color.parseColor(clip.themeColorHex)
        } catch (e: Exception) {
            Color.parseColor("#FFFDFC")
        }

        // 1. Draw solid background
        val bgPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw modern border frame
        val borderPaint = Paint().apply {
            color = Color.parseColor("#2C3E50")
            style = Paint.Style.STROKE
            strokeWidth = 14f
            isAntiAlias = true
        }
        canvas.drawRect(30f, 30f, width - 30f, height - 30f, borderPaint)

        // 3. Setup text paint for Brand title (top)
        val brandPaint = Paint().apply {
            color = Color.parseColor("#7F8C8D")
            textSize = 28f
            style = Paint.Style.FILL
            isAntiAlias = true
            letterSpacing = 0.2f
        }
        canvas.drawText("CLIPLUZ   •   CLARIDAD LOCAL", 80f, 100f, brandPaint)

        // 4. Draw Date of creation
        val dateFormat = SimpleDateFormat("dd MMMM, yyyy - HH:mm", Locale.forLanguageTag("es-ES"))
        val dateStr = dateFormat.format(Date(clip.createdAt))
        val dateTextPaint = Paint().apply {
            color = Color.parseColor("#95A5A6")
            textSize = 24f
            isAntiAlias = true
        }
        canvas.drawText(dateStr, width - 380f, 100f, dateTextPaint)

        // 5. Title
        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#2C3E50")
            textSize = 52f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val titleLayout = StaticLayout.Builder.obtain(clip.title, 0, clip.title.length, titlePaint, width - 160)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .build()
        
        canvas.save()
        canvas.translate(80f, 160f)
        titleLayout.draw(canvas)
        canvas.restore()

        var currentY = 160f + titleLayout.height + 60f

        // 6. Raw note body (cropped gracefully if too long)
        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#34495E")
            textSize = 32f
            isAntiAlias = true
        }
        val cleanBody = if (clip.rawText.length > 350) {
            clip.rawText.take(350).trim() + "..."
        } else {
            clip.rawText
        }
        val bodyLayout = StaticLayout.Builder.obtain(cleanBody, 0, cleanBody.length, bodyPaint, width - 160)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .build()

        canvas.save()
        canvas.translate(80f, currentY)
        bodyLayout.draw(canvas)
        canvas.restore()

        currentY += bodyLayout.height + 80f

        // 7. Tasks checklist
        val tasks = getTasks(clip.tasksJson)
        if (tasks.isNotEmpty()) {
            val taskHeaderPaint = Paint().apply {
                color = Color.parseColor("#2C3E50")
                textSize = 34f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("✓ Tareas Pendientes", 80f, currentY, taskHeaderPaint)
            currentY += 50f

            val taskTextPaint = Paint().apply {
                color = Color.parseColor("#2C3E50")
                textSize = 28f
                isAntiAlias = true
            }
            val checkPaint = Paint().apply {
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            val checkFillPaint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Draw up to max 6 tasks in image to avoid layout overflow
            tasks.take(6).forEach { task ->
                val checkY = currentY - 12f
                if (task.isDone) {
                    checkFillPaint.color = Color.parseColor("#27AE60")
                    canvas.drawCircle(95f, checkY, 14f, checkFillPaint)
                    
                    // Draw mini check mark inside
                    val checkMarkPaint = Paint().apply {
                        color = Color.WHITE
                        strokeWidth = 3f
                        style = Paint.Style.STROKE
                    }
                    canvas.drawLine(89f, checkY, 93f, checkY + 4f, checkMarkPaint)
                    canvas.drawLine(93f, checkY + 4f, 101f, checkY - 5f, checkMarkPaint)
                } else {
                    checkPaint.color = Color.parseColor("#7F8C8D")
                    canvas.drawCircle(95f, checkY, 14f, checkPaint)
                }

                // Task string with single-line cut
                val taskTxt = if (task.text.length > 55) task.text.take(52) + "..." else task.text
                canvas.drawText(taskTxt, 130f, currentY, taskTextPaint)
                currentY += 56f
            }
        }

        // 8. Watermark/Signature at the very bottom
        if (showWatermark) {
            val watermarkBgPaint = Paint().apply {
                color = Color.parseColor("#2C3E50")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, height - 70f, width.toFloat(), height.toFloat(), watermarkBgPaint)

            val logoPaint = Paint().apply {
                color = Color.WHITE
                textSize = 26f
                isFakeBoldText = true
                isAntiAlias = true
                letterSpacing = 0.1f
            }
            canvas.drawText("Creado con ClipLuz", 80f, height - 26f, logoPaint)

            val localTagPaint = Paint().apply {
                color = Color.parseColor("#BDC3C7")
                textSize = 22f
                isAntiAlias = true
            }
            canvas.drawText("100% Privado • Fuera de la Red", width - 350f, height - 28f, localTagPaint)
        }

        return try {
            val cachePath = File(context.cacheDir, "exports")
            cachePath.mkdirs()
            val file = File(cachePath, "clipluz_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }

    private fun getTasks(json: String): List<TaskItem> {
        if (json.isEmpty()) return emptyList()
        return try {
            taskAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
