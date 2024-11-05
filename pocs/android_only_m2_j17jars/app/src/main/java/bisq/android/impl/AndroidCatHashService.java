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

package bisq.android.impl;

import android.graphics.Bitmap;

import bisq.user.cathash.CatHashService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AndroidCatHashService extends CatHashService<Bitmap> {
    public AndroidCatHashService(Path baseDir) {
        super(baseDir);
    }

    @Override
    protected Bitmap composeImage(String[] paths, double size) {
       throw new RuntimeException("Not impl. yet");
    }

    @Override
    protected void writeRawImage(Bitmap image, File iconFile) throws IOException {
        throw new RuntimeException("Not impl. yet");
    }

    @Override
    protected Bitmap readRawImage(File iconFile) throws IOException {
        throw new RuntimeException("Not impl. yet");
    }
}
