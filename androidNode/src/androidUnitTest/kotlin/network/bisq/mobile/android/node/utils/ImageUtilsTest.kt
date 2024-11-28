package network.bisq.mobile.android.node.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.BeforeTest

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [31])
class ImageUtilTest {

    private lateinit var context: Context

    @BeforeTest
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test getImageByPath valid path`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // TODO couldn't get it to test with sample test asset so had to use a path for a real one
        val assetPath = "composeResources/bisqapps.shared.presentation.generated.resources/drawable/bisq_logo.png"
        val bitmap = ImageUtil.getImageByPath(context, assetPath)
        assertNotNull("Image should be loaded", bitmap)
    }

    @Test
    fun `test getImageByPath invalid path`() {
        val bitmap = ImageUtil.getImageByPath(context, "non_existing_image.png")
        assertNull("Image should be null for an invalid path", bitmap)
    }

    @Test
    fun `test composeImage from multiple paths`() {
        val paths = arrayOf("images/sample_image.png", "images/sample_overlay.png")
        val composedBitmap = ImageUtil.composeImage(context, paths, 200, 200)
        assertEquals("Composed image width should be 200", 200, composedBitmap.width)
        assertEquals("Composed image height should be 200", 200, composedBitmap.height)
    }

    @Test
    fun `test bitmapToByteArray and byteArrayToBitmap`() {
        val originalBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val byteArray = ImageUtil.bitmapToByteArray(originalBitmap)
        assertTrue("Byte array should not be empty", byteArray.isNotEmpty())

        val decodedBitmap = ImageUtil.byteArrayToBitmap(byteArray)
        assertNotNull("Decoded bitmap should not be null", decodedBitmap)
        assertEquals("Width should match", originalBitmap.width, decodedBitmap!!.width)
        assertEquals("Height should match", originalBitmap.height, decodedBitmap.height)
    }

    @Test
    fun `test writeRawImage and readRawImage`() {
        val testFile = File(context.cacheDir, "test_image.raw")
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)

        // Write bitmap to file
        ImageUtil.writeRawImage(bitmap, testFile)
        assertTrue("File should exist", testFile.exists())

        // Read bitmap from file
        val loadedBitmap = ImageUtil.readRawImage(testFile)
        assertNotNull("Loaded bitmap should not be null", loadedBitmap)
        assertEquals("Bitmap width should match", 50, loadedBitmap!!.width)
        assertEquals("Bitmap height should match", 50, loadedBitmap.height)

        // Cleanup
        testFile.delete()
    }
}
