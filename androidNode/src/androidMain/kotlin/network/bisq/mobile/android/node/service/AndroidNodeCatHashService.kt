/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */
package network.bisq.mobile.android.node.service

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import bisq.user.cathash.CatHashService
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.utils.AndroidImageUtil
import network.bisq.mobile.utils.AndroidImageUtil.PATH_TO_DRAWABLE
import java.io.File
import java.nio.file.Path

/**
 * Cat Hash implementation for Android node
 */
const val CAT_HASH_PATH = PATH_TO_DRAWABLE + "cathash/"

class AndroidNodeCatHashService(private val context: Context, baseDir: Path?) :
    CatHashService<PlatformImage>(baseDir) {
    override fun composeImage(paths: Array<String>, size: Double): PlatformImage {
        return PlatformImage(AndroidImageUtil.composeImage(
            context,
            CAT_HASH_PATH,
            paths,
            size.toInt(),
            size.toInt()
        ))
    }

    override fun writeRawImage(image: PlatformImage, file: File) {
        val bitmap: Bitmap = image.bitmap.asAndroidBitmap()
        AndroidImageUtil.writeBitmapAsByteArray(bitmap, file)
    }

    override fun readRawImage(file: File): PlatformImage? {
        val bitmap: Bitmap? = AndroidImageUtil.readByteArrayAsBitmap(file)
        return if (bitmap == null) null else PlatformImage(bitmap.asImageBitmap())
    }
}
