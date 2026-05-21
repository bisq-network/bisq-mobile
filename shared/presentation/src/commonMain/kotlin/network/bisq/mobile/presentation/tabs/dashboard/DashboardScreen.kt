package network.bisq.mobile.presentation.tabs.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_chat_circle
import bisqapps.shared.presentation.generated.resources.icon_learn
import bisqapps.shared.presentation.generated.resources.icon_markets
import bisqapps.shared.presentation.generated.resources.icon_payment
import bisqapps.shared.presentation.generated.resources.reputation
import bisqapps.shared.presentation.generated.resources.thumbs_up
import network.bisq.mobile.data.model.BatteryOptimizationState
import network.bisq.mobile.data.model.PermissionState
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.AmountWithCurrency
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.common.ui.utils.rememberNotificationPermissionLauncher
import network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel.WelcomeCarousel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun DashboardScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<DashboardPresenter>()

    val offersOnline: Number by presenter.offersOnline.collectAsState()
    val publishedProfiles: Number by presenter.publishedProfiles.collectAsState()
    val numConnections by presenter.numConnections.collectAsState()
    val marketPrice by presenter.marketPrice.collectAsState()
    val tradeRulesConfirmed by presenter.tradeRulesConfirmed.collectAsState()
    val notifPermissionState by presenter.savedNotifPermissionState.collectAsState()
    val batteryPermissionState by presenter.savedBatteryOptimizationState.collectAsState()
    val isForeground by presenter.isForeground.collectAsState()
    val showNumConnections = presenter.showNumConnections

    val notifPermLauncher =
        rememberNotificationPermissionLauncher { granted ->
            if (granted) {
                presenter.saveNotificationPermissionState(PermissionState.GRANTED)
            } else {
                // Android only lets us ask for the system dialog twice; on the second deny
                // we auto-promote to DONT_ASK_AGAIN, which mirrors the original behaviour
                // from the legacy notification dialog.
                if (notifPermissionState == PermissionState.DENIED) {
                    presenter.saveNotificationPermissionState(PermissionState.DONT_ASK_AGAIN)
                    presenter.showSnackbar(
                        "mobile.permissions.notifications.dismissed".i18n(),
                        duration = SnackbarDuration.Indefinite,
                    )
                } else {
                    presenter.saveNotificationPermissionState(PermissionState.DENIED)
                }
            }
        }

    val batteryOptimizationLauncher =
        presenter.platformSettingsManager.rememberBatteryOptimizationsLauncher { ignored ->
            if (ignored) {
                presenter.saveBatteryOptimizationState(BatteryOptimizationState.IGNORED)
            } else {
                presenter.saveBatteryOptimizationState(BatteryOptimizationState.NOT_IGNORED)
            }
        }

    // Keep persisted notification / battery state in sync with what the OS reports
    // each time the user returns to the dashboard or the underlying state changes
    // (e.g. user toggled permissions from system Settings while we were backgrounded).
    LaunchedEffect(isForeground, notifPermissionState, batteryPermissionState) {
        if (notifPermissionState != null && notifPermissionState != PermissionState.DONT_ASK_AGAIN) {
            if (presenter.hasNotificationPermission()) {
                presenter.saveNotificationPermissionState(PermissionState.GRANTED)
            } else {
                presenter.saveNotificationPermissionState(PermissionState.NOT_GRANTED)
            }
        }
        if (
            batteryPermissionState != null &&
            batteryPermissionState != BatteryOptimizationState.DONT_ASK_AGAIN
        ) {
            if (presenter.platformSettingsManager.isIgnoringBatteryOptimizations()) {
                presenter.saveBatteryOptimizationState(BatteryOptimizationState.IGNORED)
            } else {
                presenter.saveBatteryOptimizationState(BatteryOptimizationState.NOT_IGNORED)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DashboardContent(
            offersOnline = offersOnline,
            publishedProfiles = publishedProfiles,
            showNumConnections = showNumConnections,
            numConnections = numConnections,
            marketPrice = marketPrice,
            tradeRulesConfirmed = tradeRulesConfirmed,
            showIosDisclaimer = presenter.isIOS(),
            onNavigateToMarkets = presenter::onNavigateToMarkets,
            onOpenTradeGuide = presenter::onOpenTradeGuide,
        )
        WelcomeCarousel(
            onRequestNotificationPermission = { notifPermLauncher.launch() },
            onRequestBatteryOptimization = { batteryOptimizationLauncher.launch() },
        )
    }
}

@Composable
private fun DashboardContent(
    offersOnline: Number,
    publishedProfiles: Number,
    showNumConnections: Boolean,
    numConnections: Number,
    marketPrice: String,
    tradeRulesConfirmed: Boolean,
    showIosDisclaimer: Boolean,
    onNavigateToMarkets: () -> Unit,
    onOpenTradeGuide: () -> Unit,
) {
    val padding = BisqUIConstants.ScreenPadding
    BisqScrollScaffold(
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.spacedBy(padding),
    ) {
        Column {
            HomeInfoCard(
                price = marketPrice,
                text = "dashboard.marketPrice".i18n(),
            )
            BisqGap.V1()

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .semantics { contentDescription = "dashboard_content" },
                horizontalArrangement = Arrangement.spacedBy(padding),
            ) {
                HomeInfoCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    price = offersOnline.toString(),
                    text = "dashboard.offersOnline".i18n(),
                )
                if (showNumConnections) {
                    HomeInfoCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        price = numConnections.toString(),
                        text = "mobile.dashboard.numConnections".i18n(),
                    )
                } else {
                    HomeInfoCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        price = publishedProfiles.toString(),
                        text = "dashboard.activeUsers".i18n(),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.fillMaxHeight().weight(0.1f))
        if (tradeRulesConfirmed) {
            DashBoardCard(
                title = "mobile.dashboard.startTrading.headline".i18n(),
                bulletPoints =
                    listOf(
                        Pair("mobile.dashboard.main.content1".i18n(), Res.drawable.icon_markets),
                        Pair("mobile.dashboard.main.content2".i18n(), Res.drawable.icon_chat_circle),
                        Pair("mobile.dashboard.main.content3".i18n(), Res.drawable.reputation),
                    ),
                buttonText = "mobile.dashboard.startTrading.button".i18n(),
                buttonHandler = onNavigateToMarkets,
            )
        } else {
            DashBoardCard(
                title = "mobile.dashboard.tradeGuide.headline".i18n(),
                bulletPoints =
                    listOf(
                        Pair("mobile.dashboard.tradeGuide.bulletPoint1".i18n(), Res.drawable.thumbs_up),
                        Pair("bisqEasy.onboarding.top.content2".i18n(), Res.drawable.icon_payment),
                        Pair("bisqEasy.onboarding.top.content3".i18n(), Res.drawable.icon_learn),
                    ),
                buttonText = "support.resources.guides.tradeGuide".i18n(),
                buttonHandler = onOpenTradeGuide,
            )
        }
        // Apple App Review framing — only shown on iOS so Android users (where the
        // Play Store doesn't require this clarification) don't see extra disclaimer text.
        if (showIosDisclaimer) {
            BisqGap.V1()
            BisqText.SmallRegularGrey(
                text = "mobile.dashboard.disclaimer".i18n(),
                textAlign = TextAlign.Start,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = BisqUIConstants.ScreenPadding),
            )
        }
        Spacer(modifier = Modifier.fillMaxHeight().weight(0.2f))
    }
}

@Composable
fun DashBoardCard(
    title: String,
    bulletPoints: List<Pair<String, DrawableResource>>,
    buttonText: String,
    buttonHandler: () -> Unit,
) {
    BisqCard(
        padding = BisqUIConstants.ScreenPadding2X,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
    ) {
        AutoResizeText(
            text = title,
            maxLines = 1,
            textStyle = BisqTheme.typography.h1Light,
            color = BisqTheme.colors.white,
            textAlign = TextAlign.Start,
        )

        Column {
            bulletPoints.forEach { (pointKey, icon) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = BisqUIConstants.ScreenPadding),
                ) {
                    Image(
                        painterResource(icon),
                        "",
                        modifier = Modifier.size(30.dp),
                    )
                    BisqGap.H1()
                    BisqText.BaseLight(pointKey)
                }
            }
        }

        BisqButton(
            text = buttonText,
            fullWidth = true,
            onClick = buttonHandler,
        )
    }
}

@Composable
fun HomeInfoCard(
    price: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    BisqCard(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountWithCurrency(price) // TODO should be generic
        BisqGap.V1()
        BisqText.SmallRegularGrey(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DashboardContentPreview(
    language: String = "en",
    tradeRulesConfirmed: Boolean = true,
    showIosDisclaimer: Boolean = false,
) {
    BisqTheme.Preview(language = language) {
        DashboardContent(
            offersOnline = 1,
            publishedProfiles = 2,
            showNumConnections = true,
            numConnections = 8,
            marketPrice = "111247.40 BTC/USD",
            tradeRulesConfirmed = tradeRulesConfirmed,
            showIosDisclaimer = showIosDisclaimer,
            onNavigateToMarkets = {},
            onOpenTradeGuide = {},
        )
    }
}

@Preview
@Composable
private fun DashboardContent_EnPreview() = DashboardContentPreview(tradeRulesConfirmed = true)

@Preview
@Composable
private fun DashboardContent_EnRulesNotConfirmedPreview() = DashboardContentPreview(tradeRulesConfirmed = false)

@Preview
@Composable
private fun DashboardContent_RuPreview() = DashboardContentPreview("ru", true)

@Preview
@Composable
private fun DashboardContent_RuRulesNotConfirmedPreview() = DashboardContentPreview("ru", false)

@Preview
@Composable
private fun DashboardContent_IosDisclaimerPreview() = DashboardContentPreview(tradeRulesConfirmed = true, showIosDisclaimer = true)
