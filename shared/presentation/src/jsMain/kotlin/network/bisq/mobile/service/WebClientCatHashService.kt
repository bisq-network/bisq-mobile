package network.bisq.mobile.service

import kotlinx.browser.document
import network.bisq.mobile.client.cathash.BaseClientCatHashService
import network.bisq.mobile.domain.PlatformImage
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.Image
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.js.Promise

class WebClientCatHashService : BaseClientCatHashService("${getFilesDir()}/Bisq2_mobile") {

    override fun composeImage(paths: Array<String>, size: Int): PlatformImage? {
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.width = size
        canvas.height = size
        
        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        
        // Load and draw all images
        val imagePromises = paths.map { path ->
            loadImage("$CAT_HASH_PATH/$path").then { img ->
                ctx.drawImage(img, 0.0, 0.0, size.toDouble(), size.toDouble())
            }
        }
        
        // Wait for all images to be drawn
        Promise.all(imagePromises.toTypedArray()).then {
            // Return the composed image
            val dataUrl = canvas.toDataURL()
            return PlatformImage(dataUrl)
        }
        
        return null
    }

    override fun writeRawImage(image: PlatformImage, iconFilePath: String) {
        // Convert data URL to Blob and store in IndexedDB
        val dataUrl = image.dataUrl
        val byteString = window.atob(dataUrl.split(",")[1])
        val mimeType = dataUrl.split(",")[0].split(":")[1].split(";")[0]
        
        val ab = ArrayBuffer(byteString.length)
        val ia = Uint8Array(ab)
        
        for (i in byteString.indices) {
            ia[i] = byteString[i].toInt().toByte()
        }
        
        val blob = Blob(arrayOf(ab), BlobPropertyBag(mimeType))
        storeInIndexedDB(iconFilePath, blob)
    }

    override fun readRawImage(iconFilePath: String): PlatformImage? {
        // Read from IndexedDB and convert to data URL
        val blob = retrieveFromIndexedDB(iconFilePath) ?: return null
        val reader = FileReader()
        reader.readAsDataURL(blob)
        reader.onload = { event ->
            val dataUrl = event.target?.result as String
            return PlatformImage(dataUrl)
        }
        return null
    }
    
    private fun loadImage(src: String): Promise<HTMLImageElement> {
        return Promise { resolve, reject ->
            val img = Image()
            img.onload = { resolve(img) }
            img.onerror = { reject(Throwable("Failed to load image: $src")) }
            img.src = src
        }
    }
    
    private fun storeInIndexedDB(key: String, blob: Blob) {
        // TODO Implementation for storing in IndexedDB
        // Important: needs to have fallback impl as tor browser wouldn't support indexed db
    }
    
    private fun retrieveFromIndexedDB(key: String): Blob? {
        // TODO Implementation for retrieving in IndexedDB
        // **IMPORTANT**: needs to have fallback impl as tor browser wouldn't support indexed db
        return null
    }
}

private fun getFilesDir(): String {
    return "/bisq-storage"
}