package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.export.LocalExporter
import com.example.data.processor.LocalContentProcessor
import com.example.domain.models.ClipItem
import com.example.domain.models.TaskItem
import com.example.ui.viewmodel.ClipViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    clipId: Int,
    viewModel: ClipViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clips by viewModel.activeClips.collectAsState()
    val isWatermarkEnabled by viewModel.isWatermarkEnabled.collectAsState()
    val isPro by viewModel.isProUnlocked.collectAsState()

    // Find our specific clip
    val clip = clips.find { it.id == clipId }

    if (clip == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cargando o tarjeta no encontrada...")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateBack) {
                    Text("Volver al inicio")
                }
            }
        }
        return
    }

    // Local mutable state fields initialized from the Clip
    var editTitle by remember(clipId) { mutableStateOf(clip.title) }
    var editRawText by remember(clipId) { mutableStateOf(clip.rawText) }
    var selectedFolder by remember(clipId) { mutableStateOf(clip.folderName) }
    var selectedCardColor by remember(clipId) { mutableStateOf(clip.themeColorHex) }
    var newTaskText by remember { mutableStateOf("") }
    
    var showShareMenu by remember { mutableStateOf(false) }

    // Re-evaluate checklist tasks lists to render reactively inside the editor
    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }
    val listType = remember { Types.newParameterizedType(List::class.java, TaskItem::class.java) }
    val stringListType = remember { Types.newParameterizedType(List::class.java, String::class.java) }
    
    val tasks = remember(clip.tasksJson) {
        try {
            val adapter = moshi.adapter<List<TaskItem>>(listType)
            adapter.fromJson(clip.tasksJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val dates = remember(clip.datesJson) {
        try {
            val adapter = moshi.adapter<List<String>>(stringListType)
            adapter.fromJson(clip.datesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    val parsedBgColor = remember(selectedCardColor) {
        try {
            Color(android.graphics.Color.parseColor(selectedCardColor))
        } catch (e: Exception) {
            Color(0xFFFFFDFC)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Claridad", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        // Auto-save before returning
                        viewModel.editClipContent(clipId, editTitle, editRawText, selectedFolder, selectedCardColor)
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Encryption locker toggle
                    IconButton(onClick = {
                        viewModel.toggleEncryptState(clip)
                    }) {
                        Icon(
                            imageVector = if (clip.isEncrypted) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Cifrado",
                            tint = if (clip.isEncrypted) Color(0xFFE74C3C) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Star favorite
                    IconButton(onClick = {
                        viewModel.toggleFavorite(clip)
                    }) {
                        Icon(
                            imageVector = if (clip.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favoritos",
                            tint = if (clip.isFavorite) Color(0xFFF1C40F) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share popup visual trigger
                    IconButton(onClick = { showShareMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir o Exportar")
                    }

                    // Delete to trash bin
                    IconButton(onClick = {
                        viewModel.moveToTrash(clip)
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Card Canvas Wrap (matches colors selection!)
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = parsedBgColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_card_canvas")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            // Title input field inside the card
                            TextField(
                                value = editTitle,
                                onValueChange = { editTitle = it },
                                placeholder = { Text("Título de Claridad", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2C3E50)
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_title_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Raw Text Content input field
                            TextField(
                                value = editRawText,
                                onValueChange = { editRawText = it },
                                placeholder = { Text("Escribe notas, tareas rápidas, recordatorios, etc...") },
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 15.sp,
                                    color = Color(0xFF34495E)
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                minLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("detail_body_input")
                            )

                            // Render Dates Capsule badges inline
                            if (dates.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Event, contentDescription = null, tint = Color(0xFF7F8C8D), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Fechas y horas locales detectadas:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    dates.forEach { dateStr ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0x1F000000), shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(dateStr, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2C3E50))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Task Checklist Editor
                item {
                    Text(
                        "Lista de Tareas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                if (tasks.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No se detectaron tareas. ¡Usa el campo inferior para añadir tareas manualmente a la tarjeta!",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(tasks) { task ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTaskDone(clip, task.id) }
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = "Tareas",
                                    tint = if (task.isDone) Color(0xFF27AE60) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    task.text,
                                    fontSize = 14.sp,
                                    color = if (task.isDone) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (task.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Append manual checklist task field
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newTaskText,
                            onValueChange = { newTaskText = it },
                            placeholder = { Text("Añadir tarea manual...", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                if (newTaskText.isNotBlank()) {
                                    viewModel.addNewTaskManually(clip, newTaskText)
                                    newTaskText = ""
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir tarea")
                        }
                    }
                }

                // Category folders picker
                item {
                    Text(
                        "Selecciona Carpeta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(listOf("Bandeja de entrada", "Trabajo", "Personal", "Ideas")) { folder ->
                            FilterChip(
                                selected = selectedFolder == folder,
                                onClick = { selectedFolder = folder },
                                label = { Text(folder) },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                // Card dynamic background color pick row!
                item {
                    Text(
                        "Color Visual de Tarjeta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(LocalContentProcessor.BEAUTIFUL_CARD_COLORS) { colorHex ->
                            val colorValue = Color(android.graphics.Color.parseColor(colorHex))
                            val isSelected = selectedCardColor == colorHex

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colorValue)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0x33000000),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCardColor = colorHex }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Seleccionado",
                                        tint = Color(0xFF2C3E50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Save actions at Bottom Row (floating styling)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            viewModel.editClipContent(clipId, editTitle, editRawText, selectedFolder, selectedCardColor)
                            Toast.makeText(context, "Listo • Guardado en local", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_card_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Cambios Locales", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal dialogue bottom sheet menu for choosing sharing options
    if (showShareMenu) {
        AlertDialog(
            onDismissRequest = { showShareMenu = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.IosShare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar y Compartir", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "Elige el formato de salida. Las exportaciones se procesan íntegramente en tu dispositivo móvil.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    // Export option 1: Clean text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareMenu = false
                                val text = LocalExporter.exportAsCleanText(clip)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir Texto Limpio"))
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Texto Plano Limpio", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Ideal para copiar en mensajería clásica", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Export option 2: Markdown checklist
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareMenu = false
                                val text = LocalExporter.exportAsMarkdownChecklist(clip)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Compartir Markdown"))
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Checklist, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Formato Markdown checklist", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Para Notion, Obsidian o GitHub con firma", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    // Export option 3: Beautiful premium image card!
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showShareMenu = false
                                // Save on device and share Uri
                                val uri = LocalExporter.exportAsBeautifulImage(context, clip, showWatermark = isWatermarkEnabled)
                                if (uri != null) {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Compartir Tarjeta de Visibilidad"))
                                } else {
                                    Toast.makeText(context, "Error procesando el render de imagen", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Imagen de Claridad Premium", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                if (!isPro) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("PRO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                            Text("Genera una hermosa postal cuadrada de 1080px", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showShareMenu = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
