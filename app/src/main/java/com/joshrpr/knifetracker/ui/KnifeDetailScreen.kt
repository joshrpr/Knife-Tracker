package com.joshrpr.knifetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.joshrpr.knifetracker.data.Knife
import com.joshrpr.knifetracker.data.KnifeDao
import com.joshrpr.knifetracker.data.PhotoStorage
import com.joshrpr.knifetracker.data.Sharpening
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KnifeDetailViewModel(
    savedState: SavedStateHandle,
    private val dao: KnifeDao,
    private val photos: PhotoStorage,
) : ViewModel() {
    private val knifeId: Long = checkNotNull(savedState["knifeId"])

    val knife: StateFlow<Knife?> = dao.observeKnife(knifeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sharpenings: StateFlow<List<Sharpening>> = dao.observeSharpenings(knifeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun logSharpening(angle: Float, method: String, notes: String) = viewModelScope.launch {
        dao.insertSharpening(
            Sharpening(knifeId = knifeId, angle = angle, method = method.trim(), notes = notes.trim()),
        )
    }

    fun deleteSharpening(sharpening: Sharpening) = viewModelScope.launch {
        dao.deleteSharpening(sharpening)
    }

    fun deleteKnife(onDeleted: () -> Unit) = viewModelScope.launch {
        val current = dao.getKnife(knifeId) ?: return@launch
        dao.deleteKnife(current)
        photos.delete(current.photoPath)
        onDeleted()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = app()
                KnifeDetailViewModel(createSavedStateHandle(), app.database.knifeDao(), app.photos)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnifeDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: KnifeDetailViewModel = viewModel(factory = KnifeDetailViewModel.Factory),
) {
    val knife by viewModel.knife.collectAsStateWithLifecycle()
    val sharpenings by viewModel.sharpenings.collectAsStateWithLifecycle()
    var showLogDialog by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteKnife by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Sharpening?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(knife?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    knife?.let { k ->
                        IconButton(onClick = { onEdit(k.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit knife")
                        }
                        IconButton(onClick = { confirmDeleteKnife = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete knife")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showLogDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Log sharpening") },
            )
        },
    ) { padding ->
        val k = knife ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (k.photoPath != null) {
                item {
                    KnifePhoto(
                        k.photoPath,
                        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                    )
                }
            }
            item { KnifeInfo(k) }
            item {
                Text(
                    "Sharpening history",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (sharpenings.isEmpty()) {
                item { Text("Nothing logged yet.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(sharpenings, key = { it.id }) { entry ->
                SharpeningRow(entry, onDelete = { pendingDelete = entry })
            }
        }
    }

    if (showLogDialog) {
        LogSharpeningDialog(
            defaultAngle = sharpenings.firstOrNull()?.angle ?: knife?.targetAngle,
            defaultMethod = sharpenings.firstOrNull()?.method.orEmpty(),
            onDismiss = { showLogDialog = false },
            onSave = { angle, method, notes ->
                viewModel.logSharpening(angle, method, notes)
                showLogDialog = false
            },
        )
    }

    if (confirmDeleteKnife) {
        AlertDialog(
            onDismissRequest = { confirmDeleteKnife = false },
            title = { Text("Delete knife?") },
            text = { Text("This removes the knife, its photo, and all of its sharpening history.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteKnife = false
                    viewModel.deleteKnife(onDeleted = onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteKnife = false }) { Text("Cancel") } },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove the ${formatAngle(entry.angle)} sharpening from ${formatDate(entry.sharpenedAt)}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSharpening(entry)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun KnifeInfo(knife: Knife) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (knife.maker.isNotBlank()) LabeledValue("Maker", knife.maker)
        if (knife.steel.isNotBlank()) LabeledValue("Steel", knife.steel)
        knife.targetAngle?.let { LabeledValue("Target angle", "${formatAngle(it)} per side") }
        if (knife.notes.isNotBlank()) LabeledValue("Notes", knife.notes)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SharpeningRow(entry: Sharpening, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${formatAngle(entry.angle)} per side",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(formatDate(entry.sharpenedAt), style = MaterialTheme.typography.bodySmall)
                if (entry.method.isNotBlank()) Text(entry.method, style = MaterialTheme.typography.bodyMedium)
                if (entry.notes.isNotBlank()) Text(entry.notes, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete entry")
            }
        }
    }
}

private val commonAngles = listOf(12f, 15f, 17f, 20f, 25f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogSharpeningDialog(
    defaultAngle: Float?,
    defaultMethod: String,
    onDismiss: () -> Unit,
    onSave: (angle: Float, method: String, notes: String) -> Unit,
) {
    var angleText by rememberSaveable { mutableStateOf(defaultAngle?.let { formatAngle(it).trimEnd('°') } ?: "") }
    var method by rememberSaveable { mutableStateOf(defaultMethod) }
    var notes by rememberSaveable { mutableStateOf("") }
    val angle = parseAngle(angleText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log sharpening") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = angleText,
                    onValueChange = { angleText = it },
                    label = { Text("Angle per side (°)") },
                    isError = angleText.isNotBlank() && angle == null,
                    supportingText = { if (angleText.isNotBlank() && angle == null) Text("Enter 1–45") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    commonAngles.forEach { preset ->
                        FilterChip(
                            selected = angle == preset,
                            onClick = { angleText = formatAngle(preset).trimEnd('°') },
                            label = { Text(formatAngle(preset)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Stones / method (optional)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                )
            }
        },
        confirmButton = {
            TextButton(enabled = angle != null, onClick = { angle?.let { onSave(it, method, notes) } }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
