package com.joshrpr.knifetracker.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.joshrpr.knifetracker.data.Knife
import com.joshrpr.knifetracker.data.KnifeDao
import com.joshrpr.knifetracker.data.PhotoStorage
import kotlinx.coroutines.launch
import java.io.File

class KnifeEditViewModel(
    private val savedState: SavedStateHandle,
    private val dao: KnifeDao,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val knifeId: Long = checkNotNull(savedState["knifeId"])
    val isNew = knifeId == 0L
    private var original: Knife? = null

    var name by mutableStateOf("")
    var maker by mutableStateOf("")
    var steel by mutableStateOf("")
    var angleText by mutableStateOf("")
    var notes by mutableStateOf("")
    var photoPath by mutableStateOf<String?>(null)
        private set
    var loaded by mutableStateOf(isNew)
        private set

    /** Photos taken during this edit that haven't been saved to the database yet. */
    private val unsavedPhotos = mutableSetOf<String>()
    private var saved = false

    /** Where the camera app is writing; kept in SavedStateHandle so it survives process death. */
    private var pendingCapture: String?
        get() = savedState["pendingCapture"]
        set(value) { savedState["pendingCapture"] = value }

    init {
        if (!isNew) viewModelScope.launch {
            dao.getKnife(knifeId)?.let { k ->
                original = k
                name = k.name
                maker = k.maker
                steel = k.steel
                angleText = k.targetAngle?.let { formatAngle(it).trimEnd('°') } ?: ""
                notes = k.notes
                photoPath = k.photoPath
            }
            loaded = true
        }
    }

    val angleError: Boolean get() = angleText.isNotBlank() && parseAngle(angleText) == null
    val canSave: Boolean get() = loaded && name.isNotBlank() && !angleError

    fun prepareCapture(): Uri {
        val file = photos.newPhotoFile()
        pendingCapture = file.absolutePath
        return photos.uriFor(file)
    }

    fun onCaptureResult(success: Boolean) {
        val path = pendingCapture ?: return
        pendingCapture = null
        if (success && File(path).length() > 0) setPhoto(path) else File(path).delete()
    }

    fun onPicked(uri: Uri, onError: () -> Unit) = viewModelScope.launch {
        val path = photos.importFrom(uri)
        if (path != null) setPhoto(path) else onError()
    }

    fun removePhoto() = setPhoto(null)

    private fun setPhoto(path: String?) {
        photoPath?.let { if (it in unsavedPhotos) { photos.delete(it); unsavedPhotos.remove(it) } }
        path?.let { unsavedPhotos += it }
        photoPath = path
    }

    fun save(onSaved: (Long) -> Unit) {
        if (!canSave) return
        val base = original ?: Knife(name = "")
        val knife = base.copy(
            name = name.trim(),
            maker = maker.trim(),
            steel = steel.trim(),
            targetAngle = parseAngle(angleText),
            notes = notes.trim(),
            photoPath = photoPath,
        )
        viewModelScope.launch {
            val result = dao.upsertKnife(knife)
            saved = true
            unsavedPhotos.clear()
            // Replaced or removed the old photo: clean up the file it pointed to.
            original?.photoPath?.takeIf { it != photoPath }?.let(photos::delete)
            onSaved(if (isNew) result else knife.id)
        }
    }

    override fun onCleared() {
        if (!saved) unsavedPhotos.forEach(photos::delete)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = app()
                KnifeEditViewModel(createSavedStateHandle(), app.database.knifeDao(), app.photos)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnifeEditScreen(
    onDone: (knifeId: Long, wasNew: Boolean) -> Unit,
    onCancel: () -> Unit,
    viewModel: KnifeEditViewModel = viewModel(factory = KnifeEditViewModel.Factory),
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun toast(message: String) { scope.launch { snackbar.showSnackbar(message) } }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        viewModel.onCaptureResult(it)
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onPicked(uri) { toast("Couldn't load that image") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Add knife" else "Edit knife") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Cancel") }
                },
                actions = {
                    TextButton(
                        enabled = viewModel.canSave,
                        onClick = { viewModel.save { id -> onDone(id, viewModel.isNew) } },
                    ) { Text("Save") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KnifePhoto(
                viewModel.photoPath,
                Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    try {
                        takePicture.launch(viewModel.prepareCapture())
                    } catch (e: ActivityNotFoundException) {
                        viewModel.onCaptureResult(false)
                        toast("No camera app found")
                    }
                }) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Text("  Camera")
                }
                OutlinedButton(onClick = {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Text("  Gallery")
                }
                if (viewModel.photoPath != null) {
                    TextButton(onClick = viewModel::removePhoto) { Text("Remove") }
                }
            }
            Text("Photo is optional.")

            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("Name *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.maker,
                onValueChange = { viewModel.maker = it },
                label = { Text("Maker") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.steel,
                onValueChange = { viewModel.steel = it },
                label = { Text("Steel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.angleText,
                onValueChange = { viewModel.angleText = it },
                label = { Text("Target angle per side (°)") },
                isError = viewModel.angleError,
                supportingText = { Text(if (viewModel.angleError) "Enter 1–45" else "Optional, e.g. 15 or 17.5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.notes = it },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
