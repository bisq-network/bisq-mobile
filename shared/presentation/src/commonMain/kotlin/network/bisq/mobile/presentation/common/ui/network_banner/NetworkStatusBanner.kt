package network.bisq.mobile.presentation.common.ui.network_banner

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.network_banner.NetworkStatusBannerConstants.ANIMATION_DURATION_MS
import network.bisq.mobile.presentation.common.ui.network_banner.NetworkStatusBannerConstants.HIDE_DELAY_MS
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun NetworkStatusBanner() {
    val presenter: NetworkStatusBannerPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val inventoryRequestInfo by presenter.inventoryRequestInfo.collectAsState()
    val allDataReceived by presenter.allDataReceived.collectAsState()
    val numConnections by presenter.numConnections.collectAsState()
    val isMainContentVisible by presenter.isMainContentVisible.collectAsState()

    NetworkStatusBannerContent(
        allDataReceived = allDataReceived,
        numConnections = numConnections,
        isMainContentVisible = isMainContentVisible,
        inventoryRequestInfo = inventoryRequestInfo
    )
}

@Composable
private fun NetworkStatusBannerView(
    allDataReceived: Boolean,
    inventoryRequestInfo: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (allDataReceived) BisqTheme.colors.primaryDim else BisqTheme.colors.yellow,
        animationSpec = tween(durationMillis = ANIMATION_DURATION_MS),
        label = "bannerBgAnim"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                // outer padding
                start = BisqUIConstants.ScreenPadding,
                end = BisqUIConstants.ScreenPadding,
                top = 10.dp,
                bottom = BisqUIConstants.ScreenPaddingQuarter,
            )
            .clip(RoundedCornerShape(BisqUIConstants.ScreenPadding4X))
            .background(backgroundColor)
            .padding(
                // inner padding
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

/**
 * Controls when the network status banner is visible.
 */
@Composable
private fun NetworkStatusBannerContent(
    allDataReceived: Boolean,
    numConnections: Int,
    isMainContentVisible: Boolean,
    inventoryRequestInfo: String
) {
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
            delay(HIDE_DELAY_MS)
            shouldBeVisible = false
        }
    }

    AnimatedVisibility(
        visible = shouldBeVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)) +
                expandVertically(animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)),
        exit = fadeOut(
            animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
        )
    ) {
        NetworkStatusBannerView(
            allDataReceived = allDataReceived,
            inventoryRequestInfo = inventoryRequestInfo
        )
    }
}

/**
 * ⚠️ PREVIEW ONLY - DO NOT USE IN PRODUCTION CODE
 *
 * This composable is specifically designed for @Preview functions only.
 * It bypasses all business logic and animations to show static banner states.
 *
 * For production use, always use NetworkStatusBanner() instead.
 */
@Composable
private fun NetworkStatusBannerContentPreview(
    allDataReceived: Boolean,
    inventoryRequestInfo: String
) {
    NetworkStatusBannerView(
        allDataReceived = allDataReceived,
        inventoryRequestInfo = inventoryRequestInfo
    )
}

@Preview
@Composable
private fun NetworkStatusBannerPreview_Loading() {
    BisqTheme.Preview {
        NetworkStatusBannerContentPreview(
            allDataReceived = false,
            inventoryRequestInfo = "Requesting initial network data"
        )
    }
}

@Preview
@Composable
private fun NetworkStatusBannerPreview_Completed() {
    BisqTheme.Preview {
        NetworkStatusBannerContentPreview(
            allDataReceived = true,
            inventoryRequestInfo = "Network data received"
        )
    }
}

private object NetworkStatusBannerConstants {
    const val HIDE_DELAY_MS = 4000L
    const val ANIMATION_DURATION_MS = 600
}
