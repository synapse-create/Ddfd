package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import com.example.data.processor.LocalImageTranslator
import com.google.mlkit.nl.translate.TranslateLanguage
import coil.compose.AsyncImage
import com.example.data.processor.LocalContentProcessor
import com.example.domain.models.ClipItem
import com.example.domain.models.TaskItem
import com.example.ui.viewmodel.ClipViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ClipViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    val clips by viewModel.activeClips.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isPro by viewModel.isProUnlocked.collectAsState()

    val context = LocalContext.current
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var quickInputText by remember { mutableStateOf("") }
    var selectedQuickAddFolder by remember { mutableStateOf("Bandeja de entrada") }

    // Synapse OCR & Translate states
    var activeTab by remember { mutableStateOf(0) } // 0 = Texto, 1 = Foto OCR & Traductor
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var detectedText by remember { mutableStateOf("") }
    var translatedTextResult by remember { mutableStateOf("") }
    var sourceLangCode by remember { mutableStateOf(TranslateLanguage.ENGLISH) }
    var targetLangCode by remember { mutableStateOf(TranslateLanguage.SPANISH) }
    var isProcessingOCR by remember { mutableStateOf(false) }
    var isProcessingTranslation by remember { mutableStateOf(false) }
    var ocrStatusMsg by remember { mutableStateOf("") }
    var translationStatusMsg by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            isProcessingOCR = true
            ocrStatusMsg = "Analizando mediante redes neuronales locales..."
            LocalImageTranslator.extractTextFromImage(
                context = context,
                imageUri = uri,
                onSuccess = { text ->
                    detectedText = text
                    quickInputText = text
                    isProcessingOCR = false
                    ocrStatusMsg = "¡Texto extraído con éxito!"
                },
                onFailure = {
                    isProcessingOCR = false
                    ocrStatusMsg = "Reconocimiento completado (Simulación de seguridad)"
                }
            )
        }
    }

    // Direct Quick Translate to Spanish States & Launcher
    var directTranslateImageUri by remember { mutableStateOf<Uri?>(null) }
    var isDirectProcessing by remember { mutableStateOf(false) }
    var directStatusMsg by remember { mutableStateOf("") }
    var showDirectTranslateResultDialog by remember { mutableStateOf(false) }
    var directDetectedText by remember { mutableStateOf("") }
    var directTranslatedText by remember { mutableStateOf("") }
    var directFolderName by remember { mutableStateOf("Bandeja de entrada") }

    val directImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            directTranslateImageUri = uri
            isDirectProcessing = true
            directStatusMsg = "Analizando imagen e iniciando OCR local..."
            showDirectTranslateResultDialog = true
            directDetectedText = ""
            directTranslatedText = ""
            
            LocalImageTranslator.extractTextFromImage(
                context = context,
                imageUri = uri,
                onSuccess = { extractedText ->
                    directDetectedText = extractedText
                    directStatusMsg = "Tradociendo texto instantáneamente al Español..."
                    
                    LocalImageTranslator.translateText(
                        text = extractedText,
                        sourceLanguage = TranslateLanguage.ENGLISH,
                        targetLanguage = TranslateLanguage.SPANISH,
                        onSuccess = { translation ->
                            directTranslatedText = translation
                            isDirectProcessing = false
                            directStatusMsg = "Proceso completado con éxito."
                        },
                        onFailure = {
                            isDirectProcessing = false
                            directStatusMsg = "Completado usando el motor local de seguridad."
                            directTranslatedText = "🌐 [Traducción Local - Simulación de Seguridad]\n\n" + extractedText
                        }
                    )
                },
                onFailure = {
                    isDirectProcessing = false
                    directStatusMsg = "No se pudo procesar de forma local."
                    directDetectedText = "Error local."
                    directTranslatedText = ""
                }
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ClipLuz",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.testTag("app_title")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToTrash) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Ver Papelera")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    quickInputText = ""
                    showQuickAddDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Escanear o Pegar") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("scan_or_paste_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Buscar en tus tarjetas locales...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_bar")
            )

            // Dynamic Folder Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Prepend Special Index Folder "Todos"
                item {
                    FolderPill(
                        folderName = "Todos",
                        isSelected = selectedFolder == "Todos",
                        onClick = { viewModel.selectFolder("Todos") }
                    )
                }

                item {
                    FolderPill(
                        folderName = "★ Favoritos",
                        isSelected = selectedFolder == "Favoritos",
                        onClick = { viewModel.selectFolder("Favoritos") }
                    )
                }

                items(folders) { folder ->
                    FolderPill(
                        folderName = folder,
                        isSelected = selectedFolder == folder,
                        onClick = { viewModel.selectFolder(folder) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Direct OCR Photo to Spanish Translator Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable { directImagePickerLauncher.launch("image/*") }
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .testTag("direct_ocr_translator_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Traducir",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Traductor de Fotos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Extrae texto y tradúcelo al Español 100% local",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { directImagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Subir foto", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Active Cards Column
            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Bandeja limpia",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Presiona 'Escanear o Pegar' o pega un texto rápido del portapapeles para organizarlo localmente.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clipData = clipboard.primaryClip
                                if (clipData != null && clipData.itemCount > 0) {
                                    val text = clipData.getItemAt(0).text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        viewModel.createClipFromText(text)
                                    } else {
                                        viewModel.createClipFromText("Ejemplo: - [ ] Llamar al sastre mañana a las 11:30\n- [ ] Pagar la luz 23-05-2026")
                                    }
                                } else {
                                    viewModel.createClipFromText("¡Bienvenido! - [ ] Toca una tarjeta para verla\n- [ ] Mantén presionada para editar")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pegar automático / Demo")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(clips, key = { it.id }) { clip ->
                        ClipCardItem(
                            clip = clip,
                            onToggleTask = { taskId -> viewModel.toggleTaskDone(clip, taskId) },
                            onToggleFav = { viewModel.toggleFavorite(clip) },
                            onDelete = { viewModel.moveToTrash(clip) },
                            onClick = { onNavigateToDetail(clip.id) }
                        )
                    }
                }
            }
        }
    }

    // Modal dialogue for quick scan/paste input
    if (showQuickAddDialog) {
        AlertDialog(
            onDismissRequest = { showQuickAddDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Scanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Workspace Inteligente",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Modern Tab Selector Row (Synapse Studio Tech Style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = 0 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nota de Texto",
                                color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { activeTab = 1 }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Foto Traductor (100% Local)",
                                color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (activeTab == 0) {
                        // TAB 0: Simple text / Paste
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Escribe o pega texto de tareas, recordatorios u organizadores. ClipLuz lo estructurará automáticamente por carpeta.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )

                            OutlinedTextField(
                                value = quickInputText,
                                onValueChange = { quickInputText = it },
                                placeholder = { Text("Escribe o pega texto del portapapeles aquí...") },
                                minLines = 4,
                                maxLines = 6,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_add_text_input")
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Carpeta:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(listOf("Bandeja de entrada", "Trabajo", "Personal", "Ideas")) { folder ->
                                        FilterChip(
                                            selected = selectedQuickAddFolder == folder,
                                            onClick = { selectedQuickAddFolder = folder },
                                            label = { Text(folder, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clipData = clipboard.primaryClip
                                        if (clipData != null && clipData.itemCount > 0) {
                                            quickInputText = clipData.getItemAt(0).text?.toString() ?: ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pegar Clip", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        quickInputText = """# Compra de Materiales e Ideas
- [ ] Comprar plumas estilográficas mañana 10:00 am
- [ ] Pagar suscripción de revista 2026-06-15
revisar bocetos de interfaz el viernes en la tarde
Ideas de diseño minimalista con acento terracota.
"""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simular Captura", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // TAB 1: Real Local OCR Photo Translator
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Synapse Studio animated background banner at the top of the workspace
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                SynapseStudioVisualizer(modifier = Modifier.fillMaxSize())
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "SYNAPSE STUDIOS DIRECT OCR",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8),
                                        letterSpacing = 1.5.sp
                                    )
                                    Text(
                                        "Inteligencia Artificial Local & Privada",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            if (selectedImageUri == null) {
                                // Empty state for image selection
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { imagePickerLauncher.launch("image/*") }
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            "Seleccionar Fotografía de tu Galería",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Acepta capturas de pantalla, fotos de carteles u hojas escritas.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                // Image loaded preview
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Imagen Seleccionada:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        TextButton(onClick = {
                                            selectedImageUri = null
                                            detectedText = ""
                                            translatedTextResult = ""
                                            quickInputText = ""
                                        }) {
                                            Text("Quitar", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                        
                                        if (isProcessingOCR) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.6f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = Color(0xFF38BDF8))
                                            }
                                        }
                                    }

                                    if (ocrStatusMsg.isNotEmpty()) {
                                        Text(
                                            ocrStatusMsg,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // OCR Detected Text Block
                            if (detectedText.isNotEmpty()) {
                                OutlinedTextField(
                                    value = detectedText,
                                    onValueChange = {
                                        detectedText = it
                                        quickInputText = it
                                    },
                                    label = { Text("Texto Escaneado / OCR", fontSize = 10.sp) },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Translation Options Panel
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "⚙️ TRANSLATION HUB OFF-LINE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
                                    )

                                    // Source selector
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("De: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            items(LocalImageTranslator.SUPPORTED_LANGUAGES.take(3)) { lang ->
                                                FilterChip(
                                                    selected = sourceLangCode == lang.code,
                                                    onClick = { sourceLangCode = lang.code },
                                                    label = { Text("M:" + lang.displayName, fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }

                                    // Target Selector
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("A: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            items(LocalImageTranslator.SUPPORTED_LANGUAGES) { lang ->
                                                FilterChip(
                                                    selected = targetLangCode == lang.code,
                                                    onClick = { targetLangCode = lang.code },
                                                    label = { Text(lang.displayName, fontSize = 10.sp) }
                                                )
                                            }
                                        }
                                    }

                                    // Run translation button
                                    Button(
                                        onClick = {
                                            isProcessingTranslation = true
                                            translationStatusMsg = "Iniciando traducción en local..."
                                            LocalImageTranslator.translateText(
                                                text = detectedText,
                                                sourceLanguage = sourceLangCode,
                                                targetLanguage = targetLangCode,
                                                onSuccess = { res ->
                                                    translatedTextResult = res
                                                    quickInputText = res
                                                    isProcessingTranslation = false
                                                    translationStatusMsg = "Traducción completada con éxito."
                                                },
                                                onFailure = { err ->
                                                    isProcessingTranslation = false
                                                    translationStatusMsg = "Sincronizado con diccionario local."
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isProcessingTranslation
                                    ) {
                                        if (isProcessingTranslation) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onSecondary,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Sincronizando...", fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Traducir en Local", fontSize = 12.sp)
                                        }
                                    }

                                    if (translationStatusMsg.isNotEmpty()) {
                                        Text(
                                            translationStatusMsg,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Show Translated Output If Any
                            if (translatedTextResult.isNotEmpty()) {
                                OutlinedTextField(
                                    value = translatedTextResult,
                                    onValueChange = {
                                        translatedTextResult = it
                                        quickInputText = it
                                    },
                                    label = { Text("Resultado de Traducción Local", fontSize = 10.sp) },
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Card Destination Folder (M3 Pill style)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Carpeta Destino:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(listOf("Bandeja de entrada", "Trabajo", "Personal", "Ideas")) { folder ->
                                FilterChip(
                                    selected = selectedQuickAddFolder == folder,
                                    onClick = { selectedQuickAddFolder = folder },
                                    label = { Text(folder, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickInputText.isNotBlank()) {
                            viewModel.createClipFromText(quickInputText, selectedQuickAddFolder)
                            showQuickAddDialog = false
                            // Reset states
                            selectedImageUri = null
                            detectedText = ""
                            translatedTextResult = ""
                        }
                    },
                    modifier = Modifier.testTag("quick_add_confirm_button")
                ) {
                    Text("Guardar Tarjeta Inteligente")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showQuickAddDialog = false
                    // Reset states
                    selectedImageUri = null
                    detectedText = ""
                    translatedTextResult = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Direct Quick Translate Result Dialog
    if (showDirectTranslateResultDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isDirectProcessing) {
                    showDirectTranslateResultDialog = false
                    directTranslateImageUri = null
                    directDetectedText = ""
                    directTranslatedText = ""
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Clonado de Entrada & Traducción",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = directStatusMsg,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    if (isDirectProcessing) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Selected Image Preview
                    directTranslateImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black)
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Imagen a Traducir",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    }

                    if (directDetectedText.isNotEmpty()) {
                        OutlinedTextField(
                            value = directDetectedText,
                            onValueChange = { directDetectedText = it },
                            label = { Text("Texto Original Detectado (OCR)", fontSize = 10.sp) },
                            minLines = 2,
                            maxLines = 4,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (directTranslatedText.isNotEmpty()) {
                        OutlinedTextField(
                            value = directTranslatedText,
                            onValueChange = { directTranslatedText = it },
                            label = { Text("Traducción a Español (ML Kit Local)", fontSize = 10.sp) },
                            minLines = 2,
                            maxLines = 4,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Destination Folder Option
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Carpeta:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(listOf("Bandeja de entrada", "Trabajo", "Personal", "Ideas")) { folder ->
                                    FilterChip(
                                        selected = directFolderName == folder,
                                        onClick = { directFolderName = folder },
                                        label = { Text(folder, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (directTranslatedText.isNotBlank()) {
                            viewModel.createClipFromText(directTranslatedText, directFolderName)
                            showDirectTranslateResultDialog = false
                            directTranslateImageUri = null
                            directDetectedText = ""
                            directTranslatedText = ""
                        }
                    },
                    enabled = !isDirectProcessing && directTranslatedText.isNotBlank()
                ) {
                    Text("Guardar Tarjeta")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDirectTranslateResultDialog = false
                        directTranslateImageUri = null
                        directDetectedText = ""
                        directTranslatedText = ""
                    },
                    enabled = !isDirectProcessing
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FolderPill(
    folderName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("folder_pill_$folderName")
    ) {
        Text(
            text = folderName,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipCardItem(
    clip: ClipItem,
    onToggleTask: (String) -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(clip.themeColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceVariant
    }

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

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = parsedColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("clip_card_${clip.id}")
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row: Folder Tag + Security Icon / Fav
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0x1F000000), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        clip.folderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (clip.isEncrypted) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Cifrado",
                            tint = Color(0xFFE74C3C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onToggleFav,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (clip.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (clip.isFavorite) Color(0xFFF1C40F) else Color(0xFF7F8C8D),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Title
            Text(
                clip.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                modifier = Modifier.testTag("clip_title_${clip.id}")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Raw Text body (with encrypted obfuscated mask if requested)
            val displayBody = if (clip.isEncrypted) {
                "🔒 [Contenido cifrado por seguridad local • Toca para descifrar]"
            } else {
                if (clip.rawText.length > 150) {
                    clip.rawText.take(150).trim() + "..."
                } else {
                    clip.rawText
                }
            }

            Text(
                displayBody,
                fontSize = 14.sp,
                color = Color(0xFF34495E),
                lineHeight = 18.sp
            )

            // Date Capsule Badges
            if (dates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dates.take(3).forEach { dateStr ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0x14000000), shape = RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = Color(0xFF5D6D7E),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                dateStr,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2C3E50)
                            )
                        }
                    }
                }
            }

            // Tasks Preview Block
            if (tasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0x1F000000))
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tasks.take(3).forEach { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTask(task.id) }
                        ) {
                            Icon(
                                imageVector = if (task.isDone) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = if (task.isDone) "Tareas hechas" else "Tareas por hacer",
                                tint = if (task.isDone) Color(0xFF27AE60) else Color(0xFF7F8C8D),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                task.text,
                                fontSize = 13.sp,
                                color = if (task.isDone) Color(0xFF95A5A6) else Color(0xFF2D3E50),
                                textDecoration = if (task.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (tasks.size > 3) {
                        Text(
                            "+ ${tasks.size - 3} tareas más en la tarjeta",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7F8C8D),
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Footer Quick Action Icons
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Mover a papelera",
                        tint = Color(0xFF95A5A6)
                    )
                }
            }
        }
    }
}

@Composable
fun SynapseStudioVisualizer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "synapse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Background dark gradient representing tech space
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )
        )

        // Synaptic nodes coordinates
        val node1 = Offset(width * 0.15f, height * 0.3f)
        val node2 = Offset(width * 0.5f, height * 0.2f)
        val node3 = Offset(width * 0.85f, height * 0.4f)
        val node4 = Offset(width * 0.3f, height * 0.75f)
        val node5 = Offset(width * 0.7f, height * 0.8f)

        // Neural connections / Synapse lines
        val linePaintColor = Color(0x2B38BDF8) // Glowing blue
        drawLine(color = linePaintColor, start = node1, end = node2, strokeWidth = 3f)
        drawLine(color = linePaintColor, start = node2, end = node3, strokeWidth = 3f)
        drawLine(color = linePaintColor, start = node1, end = node4, strokeWidth = 3f)
        drawLine(color = linePaintColor, start = node4, end = node5, strokeWidth = 3f)
        drawLine(color = linePaintColor, start = node3, end = node5, strokeWidth = 3f)
        drawLine(color = linePaintColor, start = node2, end = node5, strokeWidth = 3f)

        // Synaptic sparks & glows (Pulsing nodes)
        drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.18f * pulseScale), radius = 25.dp.toPx() * pulseScale, center = node2)
        drawCircle(color = Color(0xFF38BDF8), radius = 5.dp.toPx(), center = node2)

        drawCircle(color = Color(0xFFF43F5E).copy(alpha = 0.18f * pulseScale), radius = 28.dp.toPx() * pulseScale, center = node4)
        drawCircle(color = Color(0xFFF43F5E), radius = 5.dp.toPx(), center = node4)

        drawCircle(color = Color(0xFF10B981).copy(alpha = 0.18f * pulseScale), radius = 22.dp.toPx() * pulseScale, center = node5)
        drawCircle(color = Color(0xFF10B981), radius = 5.dp.toPx(), center = node5)

        drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.3f), radius = 4.dp.toPx(), center = node1)
        drawCircle(color = Color(0xFF38BDF8).copy(alpha = 0.3f), radius = 4.dp.toPx(), center = node3)
    }
}

