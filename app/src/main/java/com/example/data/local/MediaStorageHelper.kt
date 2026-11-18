package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaStorageHelper(private val context: Context) {

    private val shootsRootDir: File
        get() = File(context.filesDir, "shoots").apply { mkdirs() }

    private val refsDir: File
        get() = File(context.filesDir, "refs").apply { mkdirs() }

    suspend fun decodeScaledBitmapFromUri(uri: Uri, maxDimension: Int = 1280): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, boundsOptions)
                }

                val sampleSize = calculateInSampleSize(
                    boundsOptions.outWidth,
                    boundsOptions.outHeight,
                    maxDimension
                )

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input, null, decodeOptions)
                } ?: return@withContext null

                val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL

                val rotated = applyExifOrientation(decoded, orientation)
                scaleDownIfNeeded(rotated, maxDimension)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun decodeScaledBitmapFromFile(filePath: String, maxDimension: Int = 1280): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (!file.exists()) return@withContext null
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath, boundsOptions)
                val sampleSize = calculateInSampleSize(
                    boundsOptions.outWidth,
                    boundsOptions.outHeight,
                    maxDimension
                )
                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return@withContext null
                scaleDownIfNeeded(decoded, maxDimension)
            } catch (_: Exception) {
                null
            }
        }

    suspend fun loadDefaultModelReferenceBitmap(maxDimension: Int = 1152): Bitmap =
        loadDrawableScaled(R.drawable.default_model_bedroom_ref, maxDimension)

    suspend fun loadDefaultBedroomReferenceBitmap(maxDimension: Int = 1152): Bitmap =
        loadDrawableScaled(R.drawable.default_bedroom_ref, maxDimension)

    private suspend fun loadDrawableScaled(drawableResId: Int, maxDimension: Int): Bitmap =
        withContext(Dispatchers.IO) {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, drawableResId, boundsOptions)
            val sampleSize = calculateInSampleSize(
                boundsOptions.outWidth,
                boundsOptions.outHeight,
                maxDimension
            )
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeResource(
                context.resources,
                drawableResId,
                decodeOptions
            )
            scaleDownIfNeeded(decoded, maxDimension)
        }

    suspend fun saveCustomReferenceFromUri(uri: Uri, fileName: String): String? =
        withContext(Dispatchers.IO) {
            val bitmap = decodeScaledBitmapFromUri(uri, maxDimension = 1280) ?: return@withContext null
            val outFile = File(refsDir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            outFile.absolutePath
        }

    suspend fun saveGarmentOriginalFromUri(uri: Uri, shootId: String, index: Int): String? =
        withContext(Dispatchers.IO) {
            val bitmap = decodeScaledBitmapFromUri(uri, maxDimension = 1400) ?: return@withContext null
            val dir = File(shootsRootDir, shootId).apply { mkdirs() }
            val outFile = File(dir, "original_$index.jpg")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            outFile.absolutePath
        }

    suspend fun saveGeneratedPoseBitmap(bitmap: Bitmap, shootId: String, fileName: String): String =
        withContext(Dispatchers.IO) {
            val dir = File(shootsRootDir, shootId).apply { mkdirs() }
            val outFile = File(dir, fileName)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outFile.absolutePath
        }

    suspend fun deleteShootDirectory(shootId: String) = withContext(Dispatchers.IO) {
        val dir = File(shootsRootDir, shootId)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    suspend fun saveImageFileToGallery(filePath: String, displayName: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val srcFile = File(filePath)
                if (!srcFile.exists()) return@withContext false

                val isPng = filePath.endsWith(".png", ignoreCase = true)
                val mimeType = if (isPng) "image/png" else "image/jpeg"
                val ext = if (isPng) ".png" else ".jpg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName$ext")
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/VintedStudio"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext false

                resolver.openOutputStream(imageUri)?.use { out ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                } ?: return@withContext false

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    fun buildShareImageIntent(filePath: String, title: String): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = if (filePath.endsWith(".png", ignoreCase = true)) "image/png" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        if (height > maxDim || width > maxDim) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= maxDim && halfWidth / inSampleSize >= maxDim) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            else -> return bitmap
        }
        val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (transformed != bitmap) bitmap.recycle()
        return transformed
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / largest.toFloat()
        val newW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }
}
