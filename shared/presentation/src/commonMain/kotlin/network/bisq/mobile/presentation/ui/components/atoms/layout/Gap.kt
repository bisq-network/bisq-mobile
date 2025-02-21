package network.bisq.mobile.presentation.ui.components.atoms.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

object BisqGap {

    @Composable()
    fun VQuarter() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPaddingQuarter))
    }

    @Composable()
    fun VHalf() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPaddingHalf))
    }

    @Composable()
    fun VHalfQuarter() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPaddingHalfQuarter))
    }

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
    fun V5() {
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding5X))
    }

    @Composable()
    fun HQuarter() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPaddingQuarter))
    }

    @Composable()
    fun HHalf() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPaddingHalf))
    }

    @Composable()
    fun HHalfQuarter() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPaddingHalfQuarter))
    }

    @Composable()
    fun H1() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPadding))
    }

    @Composable()
    fun H2() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPadding2X))
    }

    @Composable()
    fun H3() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPadding3X))
    }

    @Composable()
    fun H4() {
        Spacer(modifier = Modifier.width(BisqUIConstants.ScreenPadding4X))
    }

}

