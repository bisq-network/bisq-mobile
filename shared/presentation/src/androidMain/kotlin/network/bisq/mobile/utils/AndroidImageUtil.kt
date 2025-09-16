package network.bisq.mobile.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import network.bisq.mobile.domain.utils.Logging
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Android images utility functions
 */
object AndroidImageUtil : Logging {
    const val PATH_TO_DRAWABLE =
        "composeResources/bisqapps.shared.presentation.generated.resources/drawable/"

    fun composeImage(
        context: Context,
        basePath: String,
        paths: Array<String>,
        width: Int,
        height: Int
    ): ImageBitmap {
        // Use more efficient bitmap configuration for better performance
        val resultBitmap = createBitmap(width, height)
        val canvas = Canvas(resultBitmap)
        val paint = Paint().apply {
            isAntiAlias = false // Disable anti-aliasing for better performance
            isFilterBitmap = false // Disable bitmap filtering for better performance
        }

        paths.forEach { path ->
            val bitmap = getImageByPath(context, basePath, path)
            if (bitmap != null) {
                // Only scale if necessary to avoid unnecessary operations
                val scaledBitmap = if (bitmap.width != width || bitmap.height != height) {
                    bitmap.scale(width, height, false) // Use faster scaling
                } else {
                    bitmap
                }
                canvas.drawBitmap(scaledBitmap, 0f, 0f, paint)

                // Recycle scaled bitmap if it's different from original to free memory
                if (scaledBitmap !== bitmap) {
                    scaledBitmap.recycle()
                    bitmap.recycle()
                } else {
                    bitmap.recycle()
                }
            }
        }

        return resultBitmap.asImageBitmap()
    }

    fun readByteArrayAsBitmap(file: File): Bitmap? {
        return try {
            val byteArray = file.readBytes()
            byteArrayToBitmap(byteArray)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    fun writeBitmapAsByteArray(image: Bitmap, file: File) {
        try {
            val byteArray = bitmapToByteArray(image)
            FileOutputStream(file).use { it.write(byteArray) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    internal fun getImageByPath(context: Context, basePath: String, path: String): Bitmap? {
        return try {
            val fullPath = basePath + path
            val inputStream = context.assets.open(fullPath)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    internal fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    fun byteArrayToBitmap(data: ByteArray): Bitmap? {
        val inputStream = ByteArrayInputStream(data)
        return BitmapFactory.decodeStream(inputStream)
    }

    fun bitmapToPngByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    fun decodePngToImageBitmap(pngByteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(pngByteArray, 0, pngByteArray.size)
    }

    fun saveByteArrayAsPng(data: ByteArray, file: File) {
        val bitmap = byteArrayToBitmap(data)
        if (bitmap != null) {
            saveBitmapAsPng(bitmap, file)
        }
    }

    fun saveBitmapAsPng(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
    }

    fun readPngByteArray(file: File): ByteArray? {
        return try {
            file.readBytes()
        } catch (e: IOException) {
            log.e("Reading $file failed", e)
            null
        }
    }
}

