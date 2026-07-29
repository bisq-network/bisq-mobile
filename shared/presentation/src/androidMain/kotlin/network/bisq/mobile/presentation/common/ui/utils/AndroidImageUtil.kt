package network.bisq.mobile.presentation.common.ui.utils

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
    const val PATH_TO_FILES =
        "composeResources/bisqapps.shared.presentation.generated.resources/files/"

    fun composeImage(
        context: Context,
        basePath: String,
        paths: Array<String>,
        width: Int,
        height: Int,
    ): ImageBitmap {
        // Use more efficient bitmap configuration for better performance
        val resultBitmap = createBitmap(width, height)
        val canvas = Canvas(resultBitmap)
        val paint =
            Paint().apply {
                isAntiAlias = false // Disable anti-aliasing for better performance
                isFilterBitmap = false // Disable bitmap filtering for better performance
            }

        paths.forEach { path ->
            val bitmap = getImageByPath(context, basePath, path, width, height)
            if (bitmap != null) {
                // Only scale if necessary to avoid unnecessary operations
                val scaledBitmap =
                    if (bitmap.width != width || bitmap.height != height) {
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

    fun readByteArrayAsBitmap(file: File): Bitmap? =
        try {
            val byteArray = file.readBytes()
            byteArrayToBitmap(byteArray)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

    fun writeBitmapAsByteArray(
        image: Bitmap,
        file: File,
    ) {
        try {
            val byteArray = bitmapToByteArray(image)
            FileOutputStream(file).use { it.write(byteArray) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    internal fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        // Guards the loop below: with a non-positive requested size its condition never fails, so
        // inSampleSize doubles until it overflows to 0 and the following division throws.
        if (reqWidth <= 0 || reqHeight <= 0) {
            return 1
        }

        val height = options.outHeight
        val width = options.outWidth
        if (height <= 0 || width <= 0) {
            return 1
        }

        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    internal fun getImageByPath(
        context: Context,
        basePath: String,
        path: String,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap? =
        try {
            val fullPath = basePath + path
            // Bounds-then-sampled decode per
            // https://developer.android.com/topic/performance/graphics/load-bitmap.
            // The asset is opened twice because the bounds pass consumes the stream; unlike the
            // decodeResource form in that guide, a stream cannot be replayed for the second decode.
            val boundsOptions =
                BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
            context.assets.open(fullPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            }

            val decodeOptions =
                BitmapFactory.Options().apply {
                    inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
                }
            context.assets.open(fullPath).use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    internal fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    fun byteArrayToBitmap(data: ByteArray): Bitmap? {
        val decodeOptions = BitmapFactory.Options()
        return BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
    }
}
