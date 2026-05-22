package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.ClipItem
import com.example.ui.viewmodel.ClipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: ClipViewModel,
    onNavigateBack: () -> Unit
) {
    val trashedClips by viewModel.trashedClips.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Papelera de Reciclaje", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (trashedClips.isNotEmpty()) {
                        IconButton(onClick = { showConfirmDialog = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Vaciar todo")
                        }
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
            if (trashedClips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Papelera Vacía",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Las tarjetas eliminadas de la bandeja principal aparecerán aquí por seguridad local.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Los elementos eliminados se guardan localmente. Puedes restaurarlos o borrarlos por completo.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    items(trashedClips, key = { it.id }) { clip ->
                        TrashedItemRow(
                            clip = clip,
                            onRestore = { viewModel.restoreFromTrash(clip) },
                            onDeletePermanently = { viewModel.deletePermanently(clip) }
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Vaciar Papelera?", fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción eliminará de forma irreversible todas las tarjetas guardadas en este dispositivo. No hay servidores de respaldo.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrashAll()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Vaciar Permanente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TrashedItemRow(
    clip: ClipItem,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val bgCardColor = try {
        Color(android.graphics.Color.parseColor(clip.themeColorHex)).copy(alpha = 0.6f)
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("trashed_item_${clip.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    clip.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (clip.rawText.length > 80) clip.rawText.take(80) + "..." else clip.rawText,
                    fontSize = 13.sp,
                    color = Color(0xFF5D6D7E)
                )
            }

            Row {
                IconButton(onClick = onRestore) {
                    Icon(
                        Icons.Default.RestoreFromTrash,
                        contentDescription = "Restaurar",
                        tint = Color(0xFF27AE60)
                    )
                }
                IconButton(onClick = onDeletePermanently) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Borrar Permanente",
                        tint = Color(0xFFC0392B)
                    )
                }
            }
        }
    }
}
