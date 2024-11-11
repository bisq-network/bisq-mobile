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

import android.graphics.Bitmap
import bisq.user.cathash.CatHashService
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * TODO hashing implementation for image generation - used by bisq2 jars.
 */
class AndroidCatHashService(baseDir: Path?) : CatHashService<Bitmap>(baseDir) {
    override fun composeImage(paths: Array<String>, size: Double): Bitmap {
        throw RuntimeException("Not impl. yet")
    }

    @Throws(IOException::class)
    override fun writeRawImage(image: Bitmap, iconFile: File) {
        throw RuntimeException("Not impl. yet")
    }

    @Throws(IOException::class)
    override fun readRawImage(iconFile: File): Bitmap {
        throw RuntimeException("Not impl. yet")
    }
}
