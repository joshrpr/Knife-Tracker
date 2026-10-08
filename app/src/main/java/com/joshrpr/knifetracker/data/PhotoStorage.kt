package com.joshrpr.knifetracker.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Keeps knife photos in app-private storage so they survive the gallery or camera app. */
class PhotoStorage(private val context: Context) {
    private val dir: File
        get() = File(context.filesDir, "photos").apply { mkdirs() }

    fun newPhotoFile(): File = File(dir, "knife_${UUID.randomUUID()}.jpg")

    /** A content:// URI the camera app can write into. */
    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies a picked image into private storage and returns the new file's path. */
    suspend fun importFrom(uri: Uri): String? = withContext(Dispatchers.IO) {
        val target = newPhotoFile()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { input.copyTo(it) }
            } ?: return@withContext null
            target.absolutePath
        }.getOrElse {
            target.delete()
            null
        }
    }

    fun delete(path: String?) {
        if (path != null && path.startsWith(dir.absolutePath)) File(path).delete()
    }
}
