/*
 * This iconFilePath is part of Bisq.
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
package network.bisq.mobile.service

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import network.bisq.mobile.client.cathash.BaseClientCatHashService
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.utils.AndroidImageUtil
import network.bisq.mobile.utils.AndroidImageUtil.PATH_TO_DRAWABLE
import java.io.File

const val CAT_HASH_PATH = PATH_TO_DRAWABLE + "cathash/"

class AndroidClientCatHashService(private val context: Context, filesDir: String) :
    BaseClientCatHashService("$filesDir/Bisq2_mobile"), Logging {
    override fun composeImage(paths: Array<String>, size: Int): PlatformImage {
        if (size > 300) {
            log.w { "The image size is limited to 300 px, as the png files used for the composition are 300 px." }
        }
        val imageSize = minOf(300, size)
        val profileIcon = AndroidImageUtil.composeImage(
            context,
            CAT_HASH_PATH,
            paths,
            imageSize,
            imageSize
        )
        return PlatformImage(profileIcon)
    }

    override fun writeRawImage(image: PlatformImage, iconFilePath: String) {
        val bitmap: Bitmap = image.bitmap.asAndroidBitmap()
        val file = File(iconFilePath)
        AndroidImageUtil.writeBitmapAsByteArray(bitmap, file)
    }

    override fun readRawImage(iconFilePath: String): PlatformImage? {
        val file = File(iconFilePath)
        val bitmap: Bitmap? = AndroidImageUtil.readByteArrayAsBitmap(file)
        return if (bitmap == null) null else PlatformImage(bitmap.asImageBitmap())
    }
}
