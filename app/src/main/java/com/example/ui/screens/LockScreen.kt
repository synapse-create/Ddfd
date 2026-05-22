package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ClipViewModel

@Composable
fun LockScreen(
    viewModel: ClipViewModel
) {
    val context = LocalContext.current
    var pinEntered by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "ClipLuz Privado",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                "Ingresa tu PIN de seguridad local",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Display of entered dots representation
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val active = pinEntered.length >= i
                    val color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Simple styled on-screen numpad
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val numRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                numRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { num ->
                            NumpadButton(text = num, onClick = {
                                if (pinEntered.length < 4) {
                                    pinEntered += num
                                    if (pinEntered.length == 4) {
                                        val success = viewModel.attemptUnlock(pinEntered)
                                        if (!success) {
                                            Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                            pinEntered = ""
                                        }
                                    }
                                }
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Empty spacer
                    Box(modifier = Modifier.size(72.dp))

                    NumpadButton(text = "0", onClick = {
                        if (pinEntered.length < 4) {
                            pinEntered += "0"
                            if (pinEntered.length == 4) {
                                val success = viewModel.attemptUnlock(pinEntered)
                                if (!success) {
                                    Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                    pinEntered = ""
                                }
                            }
                        }
                    })

                    // Clear button
                    IconButton(
                        onClick = {
                            if (pinEntered.isNotEmpty()) {
                                pinEntered = pinEntered.dropLast(1)
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Retroceso")
                    }
                }
            }
        }
    }
}

@Composable
fun NumpadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .testTag("numpad_$text")
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
