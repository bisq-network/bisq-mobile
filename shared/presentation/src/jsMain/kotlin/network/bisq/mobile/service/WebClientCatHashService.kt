package network.bisq.mobile.service

import kotlinx.browser.document
import kotlinx.browser.window
import network.bisq.mobile.client.cathash.BaseClientCatHashService
import network.bisq.mobile.domain.PlatformImage
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.Image
import kotlin.js.Promise

// Define the path constant at the top level
private const val CAT_HASH_PATH = "cathash/"

class WebClientCatHashService : BaseClientCatHashService("/bisq-storage") {

    override fun composeImage(paths: Array<String>, size: Int): PlatformImage? {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = size
        canvas.height = size

        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D

        // In a real implementation, we would load and draw all images
        // For now, just create a simple placeholder
        ctx.fillStyle = "#f0f0f0"
        ctx.fillRect(0.0, 0.0, size.toDouble(), size.toDouble())

        val dataUrl = canvas.toDataURL()
        return PlatformImage(dataUrl)
    }

    override fun writeRawImage(image: PlatformImage, iconFilePath: String) {
        try {
            // Store in localStorage as a simple implementation
            // We need to access the dataUrl property of the PlatformImage
            val imageDataUrl = (image as? PlatformImage)?.dataUrl ?: ""
            window.localStorage.setItem(iconFilePath, imageDataUrl)
        } catch (e: Exception) {
            console.error("Failed to write image: ${e.message}")
        }
    }

    override fun readRawImage(iconFilePath: String): PlatformImage? {
        try {
            val dataUrl = window.localStorage.getItem(iconFilePath)
            return if (dataUrl != null) {
                PlatformImage(dataUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            console.error("Failed to read image: ${e.message}")
            return null
        }
    }

    // Helper function to load an image
    private fun loadImage(src: String): Promise<HTMLImageElement> {
        return Promise { resolve, reject ->
            val img = Image()
            // Fix the onload handler to match the expected type
            img.onload = { _ -> resolve(img) }
            img.onerror = { _, _, _, _, _ -> reject(Throwable("Failed to load image: $src")) }
            img.src = src
        }
    }
}