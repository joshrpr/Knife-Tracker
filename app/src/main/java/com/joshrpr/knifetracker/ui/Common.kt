package com.joshrpr.knifetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import coil.compose.AsyncImage
import com.joshrpr.knifetracker.KnifeTrackerApp
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun CreationExtras.app(): KnifeTrackerApp =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as KnifeTrackerApp

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat)

/** "17°" or "17.5°" — drops a trailing ".0". */
fun formatAngle(angle: Float): String =
    if (angle % 1f == 0f) "${angle.toInt()}°" else String.format(Locale.getDefault(), "%.1f°", angle)

/** Parses a user-typed angle, accepting a comma decimal separator. Valid range is 1–45°. */
fun parseAngle(text: String): Float? =
    text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it in 1f..45f }

@Composable
fun KnifePhoto(path: String?, modifier: Modifier = Modifier) {
    if (path != null) {
        AsyncImage(
            model = File(path),
            contentDescription = "Knife photo",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
