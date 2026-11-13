package com.beichenxing.aurora.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

/**
 * Manages local storage of character avatar images.
 * Avatars are stored in the app's internal files directory under "avatars/".
 */
object AvatarStorage {

    private const val AVATAR_DIR = "avatars"
    private const val MAX_AVATAR_SIZE = 512 // px, max width/height

    /**
     * Save avatar from a URI (user-selected image).
     * Returns the local file path, or null if saving failed.
     */
    fun saveFromUri(context: Context, uri: Uri, characterId: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            saveAvatar(context, bitmap, characterId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save avatar from URI")
            null
        }
    }

    /**
     * Save avatar from raw bytes (extracted from PNG card or CharX).
     * Returns the local file path, or null if saving failed.
     */
    fun saveFromBytes(context: Context, bytes: ByteArray, characterId: String): String? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            saveAvatar(context, bitmap, characterId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save avatar from bytes")
            null
        }
    }

    /**
     * Delete avatar file for a character.
     */
    fun delete(context: Context, characterId: String) {
        getAvatarFile(context, characterId).delete()
    }

    /**
     * Get the avatar file for a character (may not exist).
     */
    fun getAvatarFile(context: Context, characterId: String): File {
        val dir = File(context.filesDir, AVATAR_DIR)
        return File(dir, "$characterId.jpg")
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun saveAvatar(context: Context, bitmap: Bitmap, characterId: String): String? {
        return try {
            val dir = File(context.filesDir, AVATAR_DIR).also { it.mkdirs() }
            val file = File(dir, "$characterId.jpg")

            // Resize if needed
            val scaled = scaleBitmap(bitmap, MAX_AVATAR_SIZE)

            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            file.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save avatar bitmap")
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}