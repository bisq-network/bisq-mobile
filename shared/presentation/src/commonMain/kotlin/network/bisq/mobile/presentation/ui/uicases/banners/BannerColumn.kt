package network.bisq.mobile.presentation.ui.uicases.banners

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject


@Composable
fun BannerColumn() {
    val presenter: BannerColumnPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val showConnectionsBanner by presenter.showConnectionsBanner.collectAsState()
    val allConnectionsLost by presenter.allConnectionsLost.collectAsState()
    val connectionState by presenter.connectionState.collectAsState()

    val showInventoryRequestBanner by presenter.showInventoryRequestBanner.collectAsState()
    val inventoryRequestState by presenter.inventoryRequestState.collectAsState()
    val allDataReceived by presenter.allDataReceived.collectAsState()

    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)
    ) {
        AnimatedVisibility(
            visible = showConnectionsBanner,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Banner(
                text = connectionState,
                backgroundColor = if (allConnectionsLost) BisqTheme.colors.danger else BisqTheme.colors.yellow10
            )
        }

        AnimatedVisibility(
            visible = showInventoryRequestBanner,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Banner(
                text = inventoryRequestState,
                backgroundColor = BisqTheme.colors.yellow10,
            )
        }
    }
}


@Composable
fun Banner(
    text: String,
    backgroundColor: Color,
    textColor: Color = BisqTheme.colors.white,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = BisqUIConstants.ScreenPaddingQuarter, horizontal = BisqUIConstants.ScreenPadding)
            .clip(RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
            .background(backgroundColor)
    ) {
        BisqText.baseRegular(
            text,
            textColor,
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding)
        )
    }
}