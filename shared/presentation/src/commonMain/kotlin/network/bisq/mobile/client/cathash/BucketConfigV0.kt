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

class BucketConfigV0 : BucketConfig() {
    companion object {
        private val BG = Bucket(16, 0)
        private val BG_OVERLAY = Bucket(32, 1)
        private val BODY_AND_FACE = Bucket(16, 2)
        private val CHEST_AND_EARS = Bucket(16, 3)
        private val CHEST_OVERLAY = Bucket(3, 4)
        private val EARS_OVERLAY = Bucket(3, 5)
        private val FACE_OVERLAY = Bucket(17, 6)
        private val EYES = Bucket(16, 7)
        private val NOSE = Bucket(6, 8)
        private val WHISKERS = Bucket(7, 9)
        private val BODY_SHAPE = Bucket(2, 10)
        private val CHEST_SHAPE = Bucket(2, 11)
        private val EARS_SHAPE = Bucket(2, 12)
        private val FACE_SHAPE = Bucket(5, 13)
        var bucketSizes: IntArray = intArrayOf(
            BG.count,
            BG_OVERLAY.count,
            BODY_AND_FACE.count,
            CHEST_AND_EARS.count,
            CHEST_OVERLAY.count,
            EARS_OVERLAY.count,
            FACE_OVERLAY.count,
            EYES.count,
            NOSE.count,
            WHISKERS.count,
            BODY_SHAPE.count,
            CHEST_SHAPE.count,
            EARS_SHAPE.count,
            FACE_SHAPE.count
        )
        var pathTemplates: Array<PathDetails>

        init {

            val postFix = ".png"
            pathTemplates = arrayOf(
                PathDetails("bg/bg_0/" + DIGIT + postFix, BG.idx),
                PathDetails("bg/bg_1/" + DIGIT + postFix, BG_OVERLAY.idx),
                PathDetails(
                    "body/body" + SHAPE_NUMBER + "/" + DIGIT + postFix,
                    BODY_AND_FACE.idx,
                    BODY_SHAPE.idx
                ),
                PathDetails(
                    "chest/chest" + SHAPE_NUMBER + "_0/" + DIGIT + postFix,
                    CHEST_AND_EARS.idx,
                    CHEST_SHAPE.idx
                ),
                PathDetails(
                    "chest/chest" + SHAPE_NUMBER + "_1/" + DIGIT + postFix,
                    CHEST_OVERLAY.idx,
                    CHEST_SHAPE.idx
                ),
                PathDetails(
                    "ears/ears" + SHAPE_NUMBER + "_0/" + DIGIT + postFix,
                    CHEST_AND_EARS.idx,
                    EARS_SHAPE.idx
                ),
                PathDetails(
                    "ears/ears" + SHAPE_NUMBER + "_1/" + DIGIT + postFix,
                    EARS_OVERLAY.idx,
                    EARS_SHAPE.idx
                ),
                PathDetails(
                    "face/face" + SHAPE_NUMBER + "_0/" + DIGIT + postFix,
                    BODY_AND_FACE.idx,
                    FACE_SHAPE.idx
                ),
                PathDetails(
                    "face/face" + SHAPE_NUMBER + "_1/" + DIGIT + postFix,
                    FACE_OVERLAY.idx,
                    FACE_SHAPE.idx
                ),
                PathDetails("eyes/" + DIGIT + postFix, EYES.idx),
                PathDetails("nose/" + DIGIT + postFix, NOSE.idx),
                PathDetails("whiskers/" + DIGIT + postFix, WHISKERS.idx)
            )
        }
    }

    override val bucketSizes: IntArray
        get() = Companion.bucketSizes
    override val pathTemplates: Array<PathDetails>
        get() = Companion.pathTemplates
}
