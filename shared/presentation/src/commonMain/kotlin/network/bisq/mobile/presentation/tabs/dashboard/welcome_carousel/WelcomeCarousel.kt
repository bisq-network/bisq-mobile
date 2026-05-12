package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.koin.compose.koinInject

/**
 * Overlay shown on top of the dashboard. Bundles all pending opt-in cards
 * (notifications, battery optimization, ...) into a single sequential carousel
 * instead of stacking modal dialogs.
 *
 * The carousel is a live view of [WelcomeCarouselPresenter.uiState] — when the
 * underlying state shifts (the user grants the OS prompt, taps "Don't ask
 * again", or new cards become pending), the visible page transitions
 * automatically. There is no local session snapshot.
 *
 * Primary action handling is split:
 * - "Don't ask again" is presenter-owned (pure state persistence) and goes
 *   through [WelcomeCarouselPresenter.onAction].
 * - "Enable / Open settings" needs a Composable-side launcher
 *   (rememberNotificationPermissionLauncher, rememberBatteryOptimizationsLauncher),
 *   which the screen owns and bridges via [onRequestNotificationPermission] and
 *   [onRequestBatteryOptimization].
 */
@Composable
fun WelcomeCarousel(
    onRequestNotificationPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    presenter: WelcomeCarouselPresenter = koinInject(),
) {
    val uiState by presenter.uiState.collectAsState()
    if (uiState.pages.isEmpty()) return

    WelcomeCarouselContent(
        uiState = uiState,
        onPrimaryAction = { type ->
            when (type) {
                CarouselPageType.NOTIFICATIONS -> onRequestNotificationPermission()
                CarouselPageType.BATTERY -> onRequestBatteryOptimization()
            }
        },
        onAction = presenter::onAction,
    )
}

@Composable
internal fun WelcomeCarouselContent(
    uiState: WelcomeCarouselUiState,
    onPrimaryAction: (CarouselPageType) -> Unit,
    onAction: (WelcomeCarouselUiAction) -> Unit,
) {
    if (uiState.pages.isEmpty()) return

    // Wizard-stepper progress: track the peak number of cards that have been
    // pending at once during this session so the indicator dot count stays
    // stable as cards resolve. Reset when the carousel leaves composition
    // (i.e. all cards are resolved and the outer WelcomeCarousel hides us).
    var maxPagesInSession by remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.pages.size) {
        if (uiState.pages.size > maxPagesInSession) {
            maxPagesInSession = uiState.pages.size
        }
    }
    val resolvedSoFar = (maxPagesInSession - uiState.pages.size).coerceAtLeast(0)

    val currentPageType = uiState.pages.first()
    val currentPage = currentPageType.toDisplay()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BisqUIConstants.ScreenPadding2X)
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey30)
                    .padding(BisqUIConstants.ScreenPadding2X),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (maxPagesInSession > 1) {
                PageIndicatorDots(
                    totalPages = maxPagesInSession,
                    currentPage = resolvedSoFar,
                )
                BisqGap.V2()
            }

            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(currentPage.iconBackgroundColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                BisqText.H3Regular(
                    text = currentPage.iconText,
                    color = currentPage.iconBackgroundColor,
                )
            }

            BisqGap.V2()

            AnimatedContent(
                targetState = currentPageType,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "welcome_carousel_page",
            ) { type ->
                val page = type.toDisplay()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BisqText.H5Light(
                        text = page.title,
                        textAlign = TextAlign.Center,
                    )
                    BisqGap.V1()
                    BisqText.BaseLightGrey(
                        text = page.description,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            BisqGap.V3()

            BisqButton(
                text = currentPage.actionText,
                onClick = { onPrimaryAction(currentPageType) },
                fullWidth = true,
            )

            BisqGap.V1()

            BisqButton(
                text = "mobile.welcomeCarousel.dontAskAgain".i18n(),
                onClick = { onAction(WelcomeCarouselUiAction.OnDontAskAgain(currentPageType)) },
                type = BisqButtonType.Clear,
            )
        }
    }
}

@Composable
private fun PageIndicatorDots(
    totalPages: Int,
    currentPage: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier =
                    Modifier
                        .size(if (index == currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index == currentPage -> BisqTheme.colors.primary
                                index < currentPage -> BisqTheme.colors.primary.copy(alpha = 0.4f)
                                else -> BisqTheme.colors.mid_grey30
                            },
                        ),
            )
        }
    }
}

private data class CarouselPageDisplay(
    val iconText: String,
    val iconBackgroundColor: Color,
    val title: String,
    val description: String,
    val actionText: String,
)

@Composable
private fun CarouselPageType.toDisplay(): CarouselPageDisplay =
    when (this) {
        CarouselPageType.NOTIFICATIONS ->
            CarouselPageDisplay(
                iconText = "🔔",
                iconBackgroundColor = BisqTheme.colors.primary,
                title = "mobile.welcomeCarousel.notifications.title".i18n(),
                description = "mobile.welcomeCarousel.notifications.description".i18n(),
                actionText = "mobile.welcomeCarousel.notifications.action".i18n(),
            )

        CarouselPageType.BATTERY ->
            CarouselPageDisplay(
                iconText = "🔋",
                iconBackgroundColor = BisqTheme.colors.warning,
                title = "mobile.welcomeCarousel.battery.title".i18n(),
                description = "mobile.welcomeCarousel.battery.description".i18n(),
                actionText = "mobile.welcomeCarousel.battery.action".i18n(),
            )
    }

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_NotificationsAndBattery_Preview() {
    BisqTheme.Preview {
        WelcomeCarouselContent(
            uiState =
                WelcomeCarouselUiState(
                    pages = listOf(CarouselPageType.NOTIFICATIONS, CarouselPageType.BATTERY),
                ),
            onPrimaryAction = {},
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_BatteryOnly_Preview() {
    BisqTheme.Preview {
        WelcomeCarouselContent(
            uiState = WelcomeCarouselUiState(pages = listOf(CarouselPageType.BATTERY)),
            onPrimaryAction = {},
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_NotificationsOnly_Preview() {
    BisqTheme.Preview {
        WelcomeCarouselContent(
            uiState = WelcomeCarouselUiState(pages = listOf(CarouselPageType.NOTIFICATIONS)),
            onPrimaryAction = {},
            onAction = {},
        )
    }
}
