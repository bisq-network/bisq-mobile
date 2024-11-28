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
import bisq.user.cathash.CatHashService
import network.bisq.mobile.android.node.utils.ImageUtil
import java.io.File
import java.nio.file.Path

/**
 * Cat Hash implementation for Android
 */
class AndroidCatHashService(private val context: Context, baseDir: Path?) : CatHashService<Bitmap>(baseDir) {
    override fun composeImage(paths: Array<String>, size: Double): Bitmap {
        return ImageUtil.composeImage(context, paths, size.toInt(), size.toInt())
    }

    override fun writeRawImage(image: Bitmap, file: File) {
        ImageUtil.writeRawImage(image, file)
    }

    override fun readRawImage(file: File): Bitmap? {
        return ImageUtil.readRawImage(file)
    }
}
