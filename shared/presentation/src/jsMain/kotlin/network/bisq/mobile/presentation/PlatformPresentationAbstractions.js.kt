package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.document
import kotlinx.browser.window
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ui.helpers.TimeProvider
import network.bisq.mobile.presentation.ui.helpers.WebCurrentTimeProvider
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLImageElement

actual fun getPlatformPainter(platformImage: PlatformImage): Painter {
    val dataUrl = platformImage.dataUrl

    return object : Painter() {
        override val intrinsicSize = androidx.compose.ui.geometry.Size.Unspecified

        override fun DrawScope.onDraw() {
            val canvas = drawContext.canvas
            val paint = androidx.compose.ui.graphics.Paint()

            // Use DOM API to load and draw the image onto a canvas manually
            val image = document.createElement("img") as HTMLImageElement
            image.src = dataUrl

            image.onload = {
                val (htmlCanvas, context) = createCanvasContext(image.width, image.height)
                context.drawImage(image, 0.0, 0.0)

                // Compose Web doesn't currently support reading canvas as ImageBitmap directly,
                // so this is mostly useful for mocking or basic layout sizing
            }

            // For now: You can draw a placeholder or background to represent it
            drawRect(color = androidx.compose.ui.graphics.Color.Gray)
        }
    }
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
    if (window.confirm("Are you sure you want to exit the application?")) {
        try {
            // Try to close the window
            window.close()
            
            // If we get here, the window.close() call didn't throw an exception,
            // but it might still have been blocked. After a short delay, check if 
            // we're still here and redirect if necessary
            window.setTimeout({
                // If we're still here, the close was probably blocked
                window.location.href = "#/goodbye"
            }, 300)
        } catch (e: Exception) {
            console.error("Error closing window:", e)
            // If close throws an exception, redirect
            window.location.href = "#/goodbye"
        }
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