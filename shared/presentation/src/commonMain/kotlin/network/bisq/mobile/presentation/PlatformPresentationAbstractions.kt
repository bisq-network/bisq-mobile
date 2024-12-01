package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage

expect fun getPlatformPainter(platformImage: PlatformImage): Painter