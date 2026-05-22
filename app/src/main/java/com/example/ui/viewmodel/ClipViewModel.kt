package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.export.LocalExporter
import com.example.data.local.ClipDatabase
import com.example.data.processor.LocalContentProcessor
import com.example.data.repository.ClipRepository
import com.example.data.security.LocalSecurityManager
import com.example.data.settings.DataStoreManager
import com.example.domain.models.ClipItem
import com.example.domain.models.TaskItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ClipViewModel(
    application: Application,
    private val repository: ClipRepository,
    private val dataStoreManager: DataStoreManager
) : AndroidViewModel(application) {

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Theme & Settings State
    val isDarkMode: StateFlow<Boolean?> = dataStoreManager.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isProUnlocked: StateFlow<Boolean> = dataStoreManager.isProUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isWatermarkEnabled: StateFlow<Boolean> = dataStoreManager.isWatermarkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val securePin: StateFlow<String?> = dataStoreManager.securePin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isPinLocked: StateFlow<Boolean> = dataStoreManager.isPinLocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Secure unlocking session state (memory only)
    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Query & Filtering parameters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>("Todos") // "Todos" as master index filter
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    // Base Database Streams
    val activeClips: StateFlow<List<ClipItem>> = combine(
        repository.getActiveClips(),
        _searchQuery,
        _selectedFolder
    ) { list, query, folder ->
        var filteredList = list

        // Apply folder filter
        if (folder != null && folder != "Todos" && folder != "Favoritos") {
            filteredList = filteredList.filter { it.folderName == folder }
        } else if (folder == "Favoritos") {
            filteredList = filteredList.filter { it.isFavorite }
        }

        // Apply search search
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            filteredList = filteredList.filter {
                it.title.lowercase().contains(q) || it.rawText.lowercase().contains(q)
            }
        }
        filteredList
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashedClips: StateFlow<List<ClipItem>> = repository.getTrashedClips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<String>> = repository.getFolders()
        .map { list ->
            // Prepend standard folders in Spanish
            listOf("Bandeja de entrada", "Trabajo", "Personal", "Ideas") + list.filter {
                it !in listOf("Bandeja de entrada", "Trabajo", "Personal", "Ideas")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Bandeja de entrada", "Trabajo", "Personal", "Ideas"))

    // UI Feedback state (one shot events or status messages)
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    init {
        // Initial app lock state evaluation
        viewModelScope.launch {
            securePin.collect { pin ->
                if (pin.isNullOrEmpty()) {
                    _isAppUnlocked.value = true
                }
            }
        }
    }

    // Attempt PIN validation
    fun attemptUnlock(pin: String): Boolean {
        val currentPin = securePin.value
        return if (pin == currentPin) {
            _isAppUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockAppManually() {
        if (!securePin.value.isNullOrEmpty()) {
            _isAppUnlocked.value = false
        }
    }

    // Toggle styling settings
    fun setDarkMode(enabled: Boolean?) {
        viewModelScope.launch {
            dataStoreManager.setDarkMode(enabled)
        }
    }

    fun toggleProState() {
        viewModelScope.launch {
            val nextState = !isProUnlocked.value
            dataStoreManager.setProUnlocked(nextState)
            _uiMessage.emit(if (nextState) "Premium Activado • Plantillas Desbloqueadas" else "Suscripción Desactivada")
        }
    }

    fun setSecurePin(pin: String?) {
        viewModelScope.launch {
            dataStoreManager.setSecurePin(pin)
            if (pin.isNullOrEmpty()) {
                _isAppUnlocked.value = true
                _uiMessage.emit("Bloqueo de PIN desactivado")
            } else {
                _uiMessage.emit("PIN de seguridad configurado con éxito")
            }
        }
    }

    fun setWatermarkEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.setWatermarkEnabled(enabled)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectFolder(folder: String?) {
        _selectedFolder.value = folder
    }

    // --- Clip CRUD actions ---

    fun createClipFromText(text: String, folderName: String = "Bandeja de entrada") {
        viewModelScope.launch {
            if (text.isBlank()) return@launch

            // Parse text locally using regex engine
            val result = LocalContentProcessor.processContent(text)
            
            val newClip = ClipItem(
                rawText = text,
                title = result.title,
                tasksJson = LocalContentProcessor.serializeTasks(result.tasks),
                datesJson = LocalContentProcessor.serializeDates(result.dates),
                themeColorHex = result.themeColor,
                folderName = folderName,
                isProTemplate = false
            )
            repository.insert(newClip)
            _uiMessage.emit("¡Tarjeta creada con claridad!")
        }
    }

    fun updateClip(clip: ClipItem) {
        viewModelScope.launch {
            // Re-evaluate checklist or metadata if edited raw
            repository.update(clip)
        }
    }

    fun editClipContent(clipId: Int, newTitle: String, newRawText: String, folderName: String, colorHex: String) {
        viewModelScope.launch {
            val existing = repository.getClipOnce(clipId) ?: return@launch
            
            // Re-process raw text to extract any modified/added tasks & dates
            val processResult = LocalContentProcessor.processContent(newRawText)

            var finalTasks = processResult.tasks
            val decodedExistingTasks: List<TaskItem> = try {
                val listType = Types.newParameterizedType(List::class.java, TaskItem::class.java)
                val adapter: com.squareup.moshi.JsonAdapter<List<TaskItem>> = moshi.adapter(listType)
                adapter.fromJson(existing.tasksJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            // Keep status of previous tasks if text still matches to avoid manual wiping
            finalTasks = finalTasks.map { newTask ->
                val matchingOld = decodedExistingTasks.find { it.text.trim().lowercase() == newTask.text.trim().lowercase() }
                if (matchingOld != null) {
                    newTask.copy(isDone = matchingOld.isDone)
                } else {
                    newTask
                }
            }

            val updated = existing.copy(
                title = newTitle.ifBlank { processResult.title },
                rawText = newRawText,
                tasksJson = LocalContentProcessor.serializeTasks(finalTasks),
                datesJson = LocalContentProcessor.serializeDates(processResult.dates),
                folderName = folderName,
                themeColorHex = colorHex
            )
            repository.update(updated)
            _uiMessage.emit("Cambios guardados localmente")
        }
    }

    fun toggleTaskDone(clip: ClipItem, taskId: String) {
        viewModelScope.launch {
            val listType = Types.newParameterizedType(List::class.java, TaskItem::class.java)
            val adapter: com.squareup.moshi.JsonAdapter<List<TaskItem>> = moshi.adapter(listType)
            val tasks = try {
                adapter.fromJson(clip.tasksJson)?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            val index = tasks.indexOfFirst { it.id == taskId }
            if (index != -1) {
                val target = tasks[index]
                tasks[index] = target.copy(isDone = !target.isDone)
                
                val updatedClip = clip.copy(
                    tasksJson = adapter.toJson(tasks)
                )
                repository.update(updatedClip)
            }
        }
    }

    fun addNewTaskManually(clip: ClipItem, taskText: String) {
        viewModelScope.launch {
            if (taskText.isBlank()) return@launch
            val listType = Types.newParameterizedType(List::class.java, TaskItem::class.java)
            val adapter: com.squareup.moshi.JsonAdapter<List<TaskItem>> = moshi.adapter(listType)
            val tasks = try {
                adapter.fromJson(clip.tasksJson)?.toMutableList() ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }

            tasks.add(TaskItem(text = taskText, isDone = false))
            val updatedClip = clip.copy(
                tasksJson = adapter.toJson(tasks)
            )
            repository.update(updatedClip)
        }
    }

    fun toggleFavorite(clip: ClipItem) {
        viewModelScope.launch {
            val nextState = !clip.isFavorite
            repository.update(clip.copy(isFavorite = nextState))
            _uiMessage.emit(if (nextState) "Añadido a favoritos" else "Quitado de favoritos")
        }
    }

    fun toggleEncryptState(clip: ClipItem) {
        viewModelScope.launch {
            val nextState = !clip.isEncrypted
            val updatedText = if (nextState) {
                LocalSecurityManager.encrypt(clip.rawText)
            } else {
                LocalSecurityManager.decrypt(clip.rawText)
            }
            repository.update(
                clip.copy(
                    isEncrypted = nextState,
                    rawText = updatedText
                )
            )
            _uiMessage.emit(if (nextState) "Tarjeta cifrada con llave local" else "Contenido descifrado")
        }
    }

    fun moveToTrash(clip: ClipItem) {
        viewModelScope.launch {
            repository.update(clip.copy(isTrash = true))
            _uiMessage.emit("Tarjeta movida a Papelera")
        }
    }

    fun restoreFromTrash(clip: ClipItem) {
        viewModelScope.launch {
            repository.update(clip.copy(isTrash = false))
            _uiMessage.emit("Tarjeta restaurada con éxito")
        }
    }

    fun deletePermanently(clip: ClipItem) {
        viewModelScope.launch {
            repository.delete(clip)
            _uiMessage.emit("Tarjeta borrada para siempre")
        }
    }

    fun emptyTrashAll() {
        viewModelScope.launch {
            repository.emptyTrash()
            _uiMessage.emit("Papelera vaciada por completo")
        }
    }
}

// ViewModel Factory definition
class ClipViewModelFactory(
    private val application: Application,
    private val repository: ClipRepository,
    private val dataStoreManager: DataStoreManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClipViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClipViewModel(application, repository, dataStoreManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
