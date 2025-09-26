package network.bisq.mobile.presentation.ui.uicases.banners

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.check_circle
import kotlinx.coroutines.delay
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject


@Composable
fun Banner() {
    val presenter: BannerPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val inventoryRequestInfo by presenter.inventoryRequestInfo.collectAsState()
    val allDataReceived by presenter.allDataReceived.collectAsState()
    val numConnections by presenter.numConnections.collectAsState()
    val isMainContentVisible by presenter.isMainContentVisible.collectAsState()

    val duration = 600
    val backgroundColor by animateColorAsState(
        targetValue = if (allDataReceived) BisqTheme.colors.primaryDim else BisqTheme.colors.yellow,
        animationSpec = tween(durationMillis = duration),
        label = "bannerBgAnim"
    )

    var shouldBeVisible by remember { mutableStateOf(false) }

    // Show/hide immediately (no delay) while fetching
    LaunchedEffect(isMainContentVisible, allDataReceived, numConnections) {
        if (isMainContentVisible && !allDataReceived && numConnections > 0) {
            shouldBeVisible = true
        } else if (!allDataReceived) {
            // Only hide immediately when still fetching
            shouldBeVisible = false
        }
    }
    // Hide with delay once all data has been received
    LaunchedEffect(allDataReceived, shouldBeVisible) {
        if (allDataReceived && shouldBeVisible) {
            delay(4000)
            shouldBeVisible = false
        }
    }

    AnimatedVisibility(
        visible = shouldBeVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = duration)) +
                expandVertically(animationSpec = tween(durationMillis = duration)),
        exit = fadeOut(
            animationSpec = tween(durationMillis = duration)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = duration)
        )
    )
    {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth()
                .padding(
                    // outer padding
                    start = BisqUIConstants.ScreenPadding,
                    end = BisqUIConstants.ScreenPadding,
                    top = 10.dp,
                    bottom = 0.dp,
                )
                .clip(RoundedCornerShape(BisqUIConstants.ScreenPadding4X))
                .background(backgroundColor)
                .padding(  // inner padding
                    vertical = BisqUIConstants.ScreenPadding,
                    horizontal = BisqUIConstants.ScreenPadding
                )
        ) {
            if (allDataReceived) {
                Image(
                    painter = painterResource(Res.drawable.check_circle),
                    colorFilter = ColorFilter.tint(BisqTheme.colors.white),
                    contentDescription = "All data received",
                    modifier = Modifier.size(20.dp),
                )
            } else {
                CircularProgressIndicator(
                    color = BisqTheme.colors.white,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 1.dp,
                )
            }

            BisqGap.HHalfQuarter()
            BisqText.baseRegular(
                text = inventoryRequestInfo,
                color = BisqTheme.colors.white,
            )
        }
    }
}