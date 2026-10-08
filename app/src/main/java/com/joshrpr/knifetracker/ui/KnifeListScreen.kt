package com.joshrpr.knifetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.joshrpr.knifetracker.data.KnifeDao
import com.joshrpr.knifetracker.data.KnifeSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class KnifeListViewModel(dao: KnifeDao) : ViewModel() {
    val knives: StateFlow<List<KnifeSummary>?> = dao.observeKnifeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory = viewModelFactory {
            initializer { KnifeListViewModel(app().database.knifeDao()) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnifeListScreen(
    onAddKnife: () -> Unit,
    onOpenKnife: (Long) -> Unit,
    viewModel: KnifeListViewModel = viewModel(factory = KnifeListViewModel.Factory),
) {
    val knives by viewModel.knives.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Knife Tracker") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddKnife,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add knife") },
            )
        },
    ) { padding ->
        val list = knives
        when {
            list == null -> Unit
            list.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No knives yet. Add one to start logging sharpening angles.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.knife.id }) { summary ->
                    KnifeRow(summary, onClick = { onOpenKnife(summary.knife.id) })
                }
            }
        }
    }
}

@Composable
private fun KnifeRow(summary: KnifeSummary, onClick: () -> Unit) {
    val knife = summary.knife
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KnifePhoto(knife.photoPath, Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(knife.name, style = MaterialTheme.typography.titleMedium)
                val subtitle = listOf(knife.maker, knife.steel).filter { it.isNotBlank() }.joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
                val last = if (summary.lastAngle != null && summary.lastSharpenedAt != null) {
                    "Last: ${formatAngle(summary.lastAngle)} per side on ${formatDate(summary.lastSharpenedAt)}"
                } else {
                    "Not sharpened yet"
                }
                Text(
                    last,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
