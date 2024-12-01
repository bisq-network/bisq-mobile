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

import com.ionspin.kotlin.bignum.integer.BigInteger
import kotlin.math.abs

object BucketEncoder {
    /**
     * @param input A BigInteger input that is to be split up deterministically in buckets according to the bucketSizes array.
     * @return buckets
     */
    fun encode(input: BigInteger, bucketSizes: IntArray): IntArray {
        var value: BigInteger = input
        var currentBucket = 0
        val result = IntArray(bucketSizes.size)
        while (currentBucket < bucketSizes.size) {
            val bucketSize = bucketSizes[currentBucket]
            val divisorReminder: Pair<BigInteger, BigInteger> =
                value.divideAndRemainder(BigInteger(bucketSize.toLong()))

            value = divisorReminder.first
            val reminder: Long = divisorReminder.second.longValue()
            val res = abs(reminder % bucketSize).toInt()
            result[currentBucket] = res
            currentBucket++
        }
        return result
    }

    fun toPaths(buckets: IntArray, pathTemplates: Array<BucketConfig.PathDetails>): Array<String?> {
        val paths = arrayOfNulls<String>(pathTemplates.size)
        for (i in paths.indices) {
            val path = pathTemplates[i].path
            val shapeIdx: Int? = pathTemplates[i].shapeIdx
            val itemIdx = pathTemplates[i].itemIdx

            if (shapeIdx == null) {
                paths[i] = generatePath(path, buckets[itemIdx])
            } else {
                paths[i] = generatePath(path, buckets[shapeIdx], buckets[itemIdx])
            }
        }
        return paths
    }

    private fun generatePath(pathTemplate: String, shapeNumber: Int, index: Int): String {
        return pathTemplate
            .replace(BucketConfig.SHAPE_NUMBER.toRegex(), shapeNumber.toString())
            .replace(BucketConfig.DIGIT.toRegex(), index.toString().padStart(2, '0'))
    }

    private fun generatePath(pathTemplate: String, index: Int): String {
        return pathTemplate.replace(BucketConfig.DIGIT.toRegex(), index.toString().padStart(2, '0'))
    }
}
