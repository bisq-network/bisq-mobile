package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_chat
import bisqapps.shared.presentation.generated.resources.icon_learn
import bisqapps.shared.presentation.generated.resources.icon_markets
import bisqapps.shared.presentation.generated.resources.icon_payment
import bisqapps.shared.presentation.generated.resources.reputation
import bisqapps.shared.presentation.generated.resources.thumbs_up
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.data.model.NotificationPermissionState
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.AmountWithCurrency
import network.bisq.mobile.presentation.ui.components.organisms.NotificationPermissionDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.rememberNotificationPermissionLauncher
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun DashboardScreen() {
    val presenter: DashboardPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val offersOnline: Number by presenter.offersOnline.collectAsState()
    val publishedProfiles: Number by presenter.publishedProfiles.collectAsState()
    val numConnections by presenter.numConnections.collectAsState()
    val isInteractive by presenter.isInteractive.collectAsState()
    val marketPrice by presenter.marketPrice.collectAsState()
    val tradeRulesConfirmed by presenter.tradeRulesConfirmed.collectAsState()
    val notifPermissionState by presenter.savedNotifPermissionState.collectAsState()
    var isPermissionRequestDialogVisible by remember { mutableStateOf(false) }
    val showNumConnections = presenter.showNumConnections

    val notifPermLauncher = rememberNotificationPermissionLauncher { granted ->
        if (granted) {
            presenter.saveNotificationPermissionState(NotificationPermissionState.GRANTED)
        } else {
            // we can't ask more than twice, so we won't ask again
            if (notifPermissionState == NotificationPermissionState.DENIED) {
                presenter.saveNotificationPermissionState(NotificationPermissionState.DONT_ASK_AGAIN)
                presenter.showSnackbar(
                    "mobile.permissions.notifications.dismissed".i18n(),
                    duration = SnackbarDuration.Indefinite,
                )
            } else {
                presenter.saveNotificationPermissionState(NotificationPermissionState.DENIED)
            }
        }
    }

    LaunchedEffect(notifPermissionState) {
        when (notifPermissionState) {
            NotificationPermissionState.GRANTED -> {
                if (presenter.hasNotificationPermission()) {
                    isPermissionRequestDialogVisible = false
                } else {
                    presenter.saveNotificationPermissionState(
                        NotificationPermissionState.NOT_GRANTED
                    )
                }
            }

            NotificationPermissionState.NOT_GRANTED,
            NotificationPermissionState.DENIED -> {
                if (notifPermissionState == NotificationPermissionState.DENIED && isPermissionRequestDialogVisible) {
                    isPermissionRequestDialogVisible = false
                } else {
                    isPermissionRequestDialogVisible = true
                }
            }

            NotificationPermissionState.DONT_ASK_AGAIN -> {
                isPermissionRequestDialogVisible = false
            }

            else -> null // ignore initial state
        }
    }

    DashboardContent(
        offersOnline = offersOnline,
        publishedProfiles = publishedProfiles,
        showNumConnections = showNumConnections,
        numConnections = numConnections,
        isInteractive = isInteractive,
        marketPrice = marketPrice,
        tradeRulesConfirmed = tradeRulesConfirmed,
        onNavigateToMarkets = presenter::onNavigateToMarkets,
        onOpenTradeGuide = presenter::onOpenTradeGuide,
        snackbarHostState = presenter.getSnackState(),
        isPermissionRequestDialogVisible = isPermissionRequestDialogVisible,
        onPermissionRequest = {
            notifPermLauncher.launch()
        },
        onPermissionDenied = { dontAskAgain ->
            if (dontAskAgain) {
                presenter.saveNotificationPermissionState(NotificationPermissionState.DONT_ASK_AGAIN)
                presenter.showSnackbar(
                    "mobile.permissions.notifications.dismissed".i18n(),
                    duration = SnackbarDuration.Indefinite,
                )
            } else if (getPlatformInfo().type != PlatformType.ANDROID) {
                // less important on iOS, so we allow background tap to dismiss
                isPermissionRequestDialogVisible = false
            }
        }
    )
}

@Composable
private fun DashboardContent(
    offersOnline: Number,
    publishedProfiles: Number,
    showNumConnections: Boolean,
    numConnections: Number,
    isInteractive: Boolean,
    marketPrice: String,
    tradeRulesConfirmed: Boolean,
    snackbarHostState: SnackbarHostState,
    onNavigateToMarkets: () -> Unit,
    onOpenTradeGuide: () -> Unit,
    isPermissionRequestDialogVisible: Boolean,
    onPermissionRequest: () -> Unit,
    onPermissionDenied: (dontAskAgain: Boolean) -> Unit,
) {
    val padding = BisqUIConstants.ScreenPadding
    BisqScrollScaffold(
        padding = PaddingValues(all = BisqUIConstants.Zero),
        snackbarHostState = snackbarHostState,
        verticalArrangement = Arrangement.spacedBy(padding),
        isInteractive = isInteractive,
    ) {
        Column {
            HomeInfoCard(
                price = marketPrice,
                text = "dashboard.marketPrice".i18n()
            )
            BisqGap.V1()

            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(IntrinsicSize.Max)
                    .semantics { contentDescription = "dashboard_content" },
                horizontalArrangement = Arrangement.spacedBy(padding)
            ) {
                HomeInfoCard(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    price = offersOnline.toString(),
                    text = "dashboard.offersOnline".i18n()
                )
                if (showNumConnections) {
                    HomeInfoCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        price = numConnections.toString(),
                        text = "mobile.dashboard.numConnections".i18n()
                    )
                } else {
                    HomeInfoCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        price = publishedProfiles.toString(),
                        text = "dashboard.activeUsers".i18n()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.fillMaxHeight().weight(0.1f))
        if (tradeRulesConfirmed) {
            DashBoardCard(
                title = "mobile.dashboard.startTrading.headline".i18n(),
                bulletPoints = listOf(
                    Pair("mobile.dashboard.main.content1".i18n(), Res.drawable.icon_markets),
                    Pair("mobile.dashboard.main.content2".i18n(), Res.drawable.icon_chat),
                    Pair("mobile.dashboard.main.content3".i18n(), Res.drawable.reputation)
                ),
                buttonText = "mobile.dashboard.startTrading.button".i18n(),
                buttonHandler = onNavigateToMarkets
            )
        } else {
            DashBoardCard(
                title = "mobile.dashboard.tradeGuide.headline".i18n(),
                bulletPoints = listOf(
                    Pair("mobile.dashboard.tradeGuide.bulletPoint1".i18n(), Res.drawable.thumbs_up),
                    Pair("bisqEasy.onboarding.top.content2".i18n(), Res.drawable.icon_payment),
                    Pair("bisqEasy.onboarding.top.content3".i18n(), Res.drawable.icon_learn)
                ),
                buttonText = "support.resources.guides.tradeGuide".i18n(),
                buttonHandler = onOpenTradeGuide
            )
        }
        Spacer(modifier = Modifier.fillMaxHeight().weight(0.2f))
    }

    if (isPermissionRequestDialogVisible) {
        NotificationPermissionDialog(
            onConfirm = onPermissionRequest,
            onDismiss = onPermissionDenied,
        )
    }
}


@Composable
fun DashBoardCard(
    title: String,
    bulletPoints: List<Pair<String, DrawableResource>>,
    buttonText: String,
    buttonHandler: () -> Unit
) {
    BisqCard(
        padding = BisqUIConstants.ScreenPadding2X,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)
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
                    modifier = Modifier.padding(bottom = BisqUIConstants.ScreenPadding)
                ) {
                    Image(
                        painterResource(icon), "",
                        modifier = Modifier.size(30.dp)
                    )
                    BisqGap.H1()
                    BisqText.baseLight(pointKey)
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
fun HomeInfoCard(modifier: Modifier = Modifier, price: String, text: String) {
    BisqCard(
        modifier = modifier,
        borderRadius = BisqUIConstants.ScreenPaddingQuarter,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountWithCurrency(price) // TODO should be generic
        BisqGap.V1()
        BisqText.smallRegularGrey(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DashboardContentPreview(
    language: String = "en",
    tradeRulesConfirmed: Boolean = true,
    isPermissionRequestDialogVisible: Boolean = false,
) {
    BisqTheme.Preview(language = language) {
        DashboardContent(
            offersOnline = 1,
            publishedProfiles = 2,
            showNumConnections = true,
            numConnections = 8,
            isInteractive = true,
            marketPrice = "111247.40 BTC/USD",
            tradeRulesConfirmed = tradeRulesConfirmed,
            onNavigateToMarkets = {},
            onOpenTradeGuide = {},
            snackbarHostState = SnackbarHostState(),
            isPermissionRequestDialogVisible = isPermissionRequestDialogVisible,
            onPermissionRequest = {},
            onPermissionDenied = { _ -> },
        )
    }
}

@Preview
@Composable
private fun DashboardContentPreview_En() = DashboardContentPreview(tradeRulesConfirmed = true)

@Preview
@Composable
private fun DashboardContentPreview_EnRulesNotConfirmed() =
    DashboardContentPreview(tradeRulesConfirmed = false)

@Preview
@Composable
private fun DashboardContentPreview_En_PermissionDialog() =
    DashboardContentPreview(isPermissionRequestDialogVisible = true)

@Preview
@Composable
private fun DashboardContentPreview_Ru() = DashboardContentPreview("ru", true)

@Preview
@Composable
private fun DashboardContentPreview_RuRulesNotConfirmed() = DashboardContentPreview("ru", false)


