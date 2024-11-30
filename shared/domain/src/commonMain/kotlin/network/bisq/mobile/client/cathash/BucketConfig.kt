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
package network.bisq.mobile.client.cathash

import kotlin.jvm.JvmOverloads

abstract class BucketConfig {
    abstract val bucketSizes: IntArray

    abstract val pathTemplates: Array<PathDetails>

    internal class Bucket(val count: Int, val idx: Int)

    class PathDetails @JvmOverloads constructor(
        val path: String,
        val itemIdx: Int,
        val shapeIdx: Int? = null
    )

    companion object {
        const val CURRENT_VERSION: Int = 0
        const val DIGIT: String = "#"
        const val SHAPE_NUMBER: String = "#SHAPE_NUMBER#"
    }
}
