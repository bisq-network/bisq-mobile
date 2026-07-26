package network.bisq.mobile.presentation.common.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AndroidImageUtilTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test getImageByPath valid path`() {
        val assetPath = "bisq_logo.png"
        val bitmap =
            AndroidImageUtil.getImageByPath(
                context,
                AndroidImageUtil.PATH_TO_DRAWABLE,
                assetPath,
                reqWidth = 0,
                reqHeight = 0,
            )
        assertNotNull("Image should be loaded", bitmap)
    }

    @Test
    fun `test getImageByPath invalid path`() {
        val bitmap =
            AndroidImageUtil.getImageByPath(
                context,
                AndroidImageUtil.PATH_TO_DRAWABLE,
                "non_existing_image.png",
                reqWidth = 100,
                reqHeight = 100,
            )
        assertNull("Image should be null for an invalid path", bitmap)
    }

    @Test
    fun `getImageByPath downsamples 300px cathash layer to 150px when requesting 120px`() {
        val bitmap =
            AndroidImageUtil.getImageByPath(
                context,
                AndroidImageUtil.PATH_TO_FILES,
                "cathash/nose/01.png",
                reqWidth = 120,
                reqHeight = 120,
            )

        assertNotNull("Cathash layer should load", bitmap)
        assertEquals(150, bitmap!!.width)
        assertEquals(150, bitmap.height)
    }

    @Test
    fun `getImageByPath decodes full 300px cathash layer when requesting 300px`() {
        val bitmap =
            AndroidImageUtil.getImageByPath(
                context,
                AndroidImageUtil.PATH_TO_FILES,
                "cathash/nose/01.png",
                reqWidth = 300,
                reqHeight = 300,
            )

        assertNotNull("Cathash layer should load", bitmap)
        assertEquals(300, bitmap!!.width)
        assertEquals(300, bitmap.height)
    }

    @Test
    fun `test composeImage from multiple cathash layers`() {
        val paths = arrayOf("cathash/nose/01.png", "cathash/eyes/12.png")
        val composedBitmap =
            AndroidImageUtil.composeImage(
                context,
                AndroidImageUtil.PATH_TO_FILES,
                paths,
                120,
                120,
            )
        assertEquals(120, composedBitmap.width)
        assertEquals(120, composedBitmap.height)
        assertTrue("Composed image should contain drawn pixels", composedBitmap.hasNonTransparentPixels())
    }

    @Test
    fun `test composeImage cathash layer at 120px`() {
        val paths = arrayOf("cathash/bg/bg_0/11.png")
        val composedBitmap =
            AndroidImageUtil.composeImage(
                context,
                AndroidImageUtil.PATH_TO_FILES,
                paths,
                120,
                120,
            )
        assertEquals(120, composedBitmap.width)
        assertEquals(120, composedBitmap.height)
        assertTrue("Composed image should contain drawn pixels", composedBitmap.hasNonTransparentPixels())
    }

    @Test
    fun `calculateInSampleSize returns 2 when downsampling 300 to 120`() {
        val options =
            BitmapFactory.Options().apply {
                outWidth = 300
                outHeight = 300
            }
        assertEquals(2, AndroidImageUtil.calculateInSampleSize(options, 120, 120))
    }

    @Test
    fun `calculateInSampleSize returns 1 when target matches source size`() {
        val options =
            BitmapFactory.Options().apply {
                outWidth = 300
                outHeight = 300
            }
        assertEquals(1, AndroidImageUtil.calculateInSampleSize(options, 300, 300))
    }

    @Test
    fun `calculateInSampleSize returns 4 when downsampling 300 to 50`() {
        val options =
            BitmapFactory.Options().apply {
                outWidth = 300
                outHeight = 300
            }
        assertEquals(4, AndroidImageUtil.calculateInSampleSize(options, 50, 50))
    }

    @Test
    fun `calculateInSampleSize returns 1 for invalid or zero request size`() {
        val options =
            BitmapFactory.Options().apply {
                outWidth = 300
                outHeight = 300
            }
        assertEquals(1, AndroidImageUtil.calculateInSampleSize(options, 0, 120))
        assertEquals(1, AndroidImageUtil.calculateInSampleSize(options, 120, 0))
        assertEquals(1, AndroidImageUtil.calculateInSampleSize(options, -1, -1))
    }

    @Test
    fun `test bitmapToByteArray and byteArrayToBitmap`() {
        val originalBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val byteArray = AndroidImageUtil.bitmapToByteArray(originalBitmap)
        assertTrue("Byte array should not be empty", byteArray.isNotEmpty())

        val decodedBitmap = AndroidImageUtil.byteArrayToBitmap(byteArray)
        assertNotNull("Decoded bitmap should not be null", decodedBitmap)
        assertEquals("Width should match", originalBitmap.width, decodedBitmap!!.width)
        assertEquals("Height should match", originalBitmap.height, decodedBitmap.height)
    }

    @Test
    fun `test writeRawImage and readRawImage`() {
        val testFile = File(context.cacheDir, "test_image.raw")
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)

        // Write bitmap to file
        AndroidImageUtil.writeBitmapAsByteArray(bitmap, testFile)
        assertTrue("File should exist", testFile.exists())

        // Read bitmap from file
        val loadedBitmap = AndroidImageUtil.readByteArrayAsBitmap(testFile)
        assertNotNull("Loaded bitmap should not be null", loadedBitmap)
        assertEquals("Bitmap width should match", 50, loadedBitmap!!.width)
        assertEquals("Bitmap height should match", 50, loadedBitmap.height)

        // Cleanup
        testFile.delete()
    }

    private fun ImageBitmap.hasNonTransparentPixels(): Boolean {
        val bitmap = asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.any { pixel -> pixel ushr 24 != 0 }
    }
}
