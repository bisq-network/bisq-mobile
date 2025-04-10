@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.browser.window
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ui.helpers.TimeProvider
import network.bisq.mobile.presentation.ui.helpers.WebCurrentTimeProvider
import org.jetbrains.skia.Image
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.url.URL

actual fun getPlatformPainter(platformImage: PlatformImage): Painter {
    // For web, we convert the data URL to a Skia image and then to a Compose ImageBitmap
    val dataUrl = platformImage.dataUrl
    val image = createImageBitmapFromDataUrl(dataUrl)
    return BitmapPainter(image)
}

private fun createImageBitmapFromDataUrl(dataUrl: String): ImageBitmap {
    // This is a simplified approach - in a real implementation, you might want to use
    // a more robust method to convert data URLs to ImageBitmap
    val imageElement = document.createElement("img") as HTMLImageElement
    imageElement.src = dataUrl

    // Create a canvas to draw the image
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

    // Set canvas dimensions
    canvas.width = imageElement.width
    canvas.height = imageElement.height

    // Draw image to canvas
    ctx.drawImage(imageElement, 0.0, 0.0)

    // Get image data
    val imageData = ctx.getImageData(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())

    // Convert to Skia Image and then to Compose ImageBitmap
    val skiaImage = Image.makeFromEncoded(imageElement.src.encodeToByteArray())
    return skiaImage.toComposeImageBitmap()
}

private fun createCanvasContext(width: Int, height: Int): Pair<HTMLCanvasElement, CanvasRenderingContext2D> {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = width
    canvas.height = height
    val context = canvas.getContext("2d") as CanvasRenderingContext2D
    return Pair(canvas, context)
}

actual fun getPlatformCurrentTimeProvider(): TimeProvider = WebCurrentTimeProvider()

actual fun exitApp() {
    // For web, we can't truly exit the app, but we can close the tab/window
    // or navigate to a "goodbye" page
    if (window.confirm("Are you sure you want to exit the application?")) {
        window.close()
        // If window.close() is blocked by the browser (common in modern browsers),
        // redirect to a goodbye page or show a message
        window.location.href = "#/goodbye"
    }
}

actual fun getScreenWidthDp(): Int {
    // Get the window width in pixels
    val widthPx = window.innerWidth

    // Convert to DP using a standard conversion factor
    // Typical desktop displays have a device pixel ratio of 1-3
    val pixelRatio = window.devicePixelRatio

    // Convert pixels to DP
    return (widthPx / pixelRatio).toInt()
}