package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage

// TODO analyse using for better pwa performance
//fun PlatformImageView(platformImage: PlatformImage) {
//    Img(src = platformImage.dataUrl, attrs = {
//        attr("alt", "image")
//        attr("style", "max-width: 100%; height: auto;")
//    })
//}

actual fun rememberPlatformImagePainter(platformImage: PlatformImage): Painter {
    val dataUrl = platformImage.dataUrl

    return object : Painter() {
        override val intrinsicSize = androidx.compose.ui.geometry.Size.Unspecified

        override fun DrawScope.onDraw() {
            // Compose Web does not currently support drawing images from the DOM onto the canvas directly,
            // so we draw a placeholder box or background here.
            drawRect(color = androidx.compose.ui.graphics.Color.LightGray)
        }
    }
}