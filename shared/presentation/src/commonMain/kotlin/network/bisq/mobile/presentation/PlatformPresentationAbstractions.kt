package network.bisq.mobile.presentation

import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ui.helpers.TimeProvider

expect fun getPlatformPainter(platformImage: PlatformImage): Painter

expect fun getPlatformCurrentTimeProvider(): TimeProvider

expect fun exitApp()
