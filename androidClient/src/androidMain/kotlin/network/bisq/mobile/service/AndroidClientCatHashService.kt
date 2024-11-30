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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import network.bisq.mobile.client.cathash.ClientCatHashService
import network.bisq.mobile.utils.ImageUtil
import network.bisq.mobile.utils.ImageUtil.PATH_TO_DRAWABLE
import java.io.File

const val CAT_HASH_PATH = PATH_TO_DRAWABLE + "cathash/"

class AndroidClientCatHashService(private val context: Context, filesDir: String) :
    ClientCatHashService<ImageBitmap>("$filesDir/Bisq2_mobile") {
    override fun composeImage(paths: Array<String>, size: Int): ImageBitmap {
        return ImageUtil.composeImage(
            context,
            CAT_HASH_PATH,
            paths,
            size,
            size
        ).asImageBitmap()
    }

    override fun writeRawImage(image: ImageBitmap, iconFilePath: String) {
        ImageUtil.writeRawImage(image.asAndroidBitmap(), File(iconFilePath))
    }

    override fun readRawImage(iconFilePath: String): ImageBitmap? {
        return ImageUtil.readRawImage(File((iconFilePath)))?.asImageBitmap()
    }
}
