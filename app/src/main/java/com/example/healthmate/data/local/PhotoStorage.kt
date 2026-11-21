package com.example.healthmate.data.local

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Keeps selfie images in the app's private storage (spec 4.3).
 *
 * An image picked with the Photo Picker is only lent to the app for a short time, so it is copied
 * into a file the app owns. That file's absolute path is what gets saved in `photo_path`.
 * It is an interface so ViewModel tests can use a fake instead of real files.
 */
interface PhotoStorage {
    /** Copies the picked image into private storage and returns the new file's absolute path. */
    suspend fun savePhoto(source: Uri): String

    /** Deletes a photo file. Quick, so it can also be called when a screen is closing. */
    fun deletePhoto(path: String)
}

/**
 * Stores photos in `files/photos/` inside the app's private folder. Other apps cannot read it,
 * no storage permission is needed, and Android deletes it if the app is uninstalled.
 */
class LocalPhotoStorage(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : PhotoStorage {

    private val appContext = context.applicationContext
    private val photoDir = File(appContext.filesDir, PHOTO_DIR)

    override suspend fun savePhoto(source: Uri): String = withContext(ioDispatcher) {
        photoDir.mkdirs()
        // A random name, so a new photo never overwrites another record's photo.
        val target = File(photoDir, "selfie_${UUID.randomUUID()}.${extensionFor(source)}")
        try {
            val input = appContext.contentResolver.openInputStream(source)
                ?: throw IOException("Could not open the selected image")
            input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
        } catch (e: Exception) {
            target.delete() // don't leave half-copied files behind
            throw e
        }
        target.absolutePath
    }

    override fun deletePhoto(path: String) {
        File(path).delete()
    }

    /** "image/png" → "png". Falls back to "jpg"; image loading reads the real format from the file. */
    private fun extensionFor(source: Uri): String {
        val mimeType = appContext.contentResolver.getType(source)
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
    }

    private companion object {
        const val PHOTO_DIR = "photos"
    }
}
