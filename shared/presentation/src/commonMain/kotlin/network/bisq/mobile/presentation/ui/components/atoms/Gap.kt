package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

object BisqGap {

    @Composable()
    fun V1() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
    }

    @Composable()
    fun V2() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding2X))
    }

    @Composable()
    fun V3() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding3X))
    }

    @Composable()
    fun V4() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding4X))
    }

    @Composable()
    fun H1() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPadding))
    }

}

