package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ClipViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ClipViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isPro by viewModel.isProUnlocked.collectAsState()
    val securePin by viewModel.securePin.collectAsState()
    val isWatermarkEnabled by viewModel.isWatermarkEnabled.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinValueInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes & Licencias", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .testTag("settings_column")
        ) {
            // Simulated premium card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleProState() }
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isPro) "ClipLuz PRO • Activado" else "Consigue ClipLuz PRO",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPro) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    if (isPro) "¡Gracias por apoyar el software ético y open source!" else "Formato de exportación sin firmas, bloqueo biométrico y temas exclusivos.",
                                    fontSize = 12.sp,
                                    color = if (isPro) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = if (isPro) Icons.Default.Verified else Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.toggleProState() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPro) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                contentColor = if (isPro) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isPro) "Simular Desctivación" else "Activar Pro de Prueba (Offline)")
                        }
                    }
                }
            }

            // General Settings Category
            item {
                Text(
                    "Preferencias Básicas",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Dark Mode switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkMode == true) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Tema de Aspecto", fontWeight = FontWeight.Bold)
                                Text(
                                    if (isDarkMode == true) "Fijado en Modo Oscuro" else if (isDarkMode == false) "Fijado en Modo Claro" else "Siguiendo el Sistema",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Cycle through: null (System), true (Dark), false (Light)
                        Button(
                            onClick = {
                                when (isDarkMode) {
                                    null -> viewModel.setDarkMode(true)
                                    true -> viewModel.setDarkMode(false)
                                    false -> viewModel.setDarkMode(null)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                when (isDarkMode) {
                                    null -> "Sistema"
                                    true -> "Oscuro"
                                    false -> "Claro"
                                }
                            )
                        }
                    }
                }
            }

            // Image Export watermark
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.BrandingWatermark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Firma de Visualización", fontWeight = FontWeight.Bold)
                                Text("Añade 'Creado con ClipLuz' al final de la imagen para visibilidad viral.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Switch(
                            checked = isWatermarkEnabled,
                            onCheckedChange = { viewModel.setWatermarkEnabled(it) }
                        )
                    }
                }
            }

            // Lock Security Category
            item {
                Text(
                    "Seguridad y Privacidad",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // Secure PIN row card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (securePin != null) Color(0xFF27AE60) else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Bloqueo de PIN de Entrada", fontWeight = FontWeight.Bold)
                                Text(
                                    if (securePin != null) "Cerrado con PIN seguro" else "Desactivado • Libre acceso",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (securePin != null) {
                                    viewModel.setSecurePin(null)
                                } else {
                                    pinValueInput = ""
                                    showPinDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (securePin != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (securePin != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(if (securePin != null) "Desactivar" else "Configurar")
                        }
                    }
                }
            }

            // Legal open source section
            item {
                Text(
                    "Licencia & Transparencia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Balance, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Licencia Apache 2.0", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "ClipLuz es software de código libre distribuido bajado la licencia Apache-2.0. " +
                                    "Todo lo procesado, extraído o editado en esta app vive estrictamente en la base de datos " +
                                    "Room cifrada localmente. No enviamos datos al exterior, no poseemos trackers ni dependencias opacas.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Versión de App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.outline)
                            Text("1.0.0 (Open-Source)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }

    // PIN configuration dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Configurar PIN local", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Ingresa un código numérico para bloquear la entrada a la aplicación. No lo olvides, ya que no se sincroniza con ningún servidor.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = pinValueInput,
                        onValueChange = { if (it.length <= 6) pinValueInput = it },
                        placeholder = { Text("PIN de seguridad...") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_setup_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValueInput.isNotBlank()) {
                            viewModel.setSecurePin(pinValueInput)
                            showPinDialog = false
                        } else {
                            Toast.makeText(context, "Por favor escribe un número", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Configurar código")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
