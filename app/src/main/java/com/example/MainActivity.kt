package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.local.ClipDatabase
import com.example.data.repository.ClipRepository
import com.example.data.settings.DataStoreManager
import com.example.ui.navigation.ClipNavigation
import com.example.ui.screens.LockScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ClipViewModel
import com.example.ui.viewmodel.ClipViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local offline dependencies
        val database = ClipDatabase.getInstance(this)
        val repository = ClipRepository(database.clipDao)
        val dataStoreManager = DataStoreManager(this)

        // 2. Build the stateful ClipViewModel with manual constructor injection
        val viewModelFactory = ClipViewModelFactory(application, repository, dataStoreManager)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ClipViewModel::class.java]

        // 3. Listen to toast messages from SharedFlow on lifecycle Scope
        lifecycleScope.launch {
            viewModel.uiMessage.collectLatest { message ->
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            // Observe configuration streams reactively
            val isDarkModeConfig by viewModel.isDarkMode.collectAsState()
            val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
            
            // Re-evaluate system baseline preferences
            val systemTheme = isSystemInDarkTheme()
            val useDarkTheme = isDarkModeConfig ?: systemTheme

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = isAppUnlocked,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "AppLockTransition"
                    ) { unlocked ->
                        if (unlocked) {
                            ClipNavigation(viewModel = viewModel)
                        } else {
                            LockScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
