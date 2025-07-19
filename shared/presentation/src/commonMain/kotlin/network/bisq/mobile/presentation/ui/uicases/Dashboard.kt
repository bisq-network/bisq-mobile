package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.components.molecules.AmountWithCurrency
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IGettingStarted : ViewPresenter {
    val titleKey: String
    val bulletPointsKey: List<String>
    val offersOnline: StateFlow<Number>
    val publishedProfiles: StateFlow<Number>

    fun onStartTrading()
    fun navigateLearnMore()
}

@Composable
fun DashboardScreen() {
    val presenter: DashboardPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val offersOnline: Number by presenter.offersOnline.collectAsState()
    val publishedProfiles: Number by presenter.publishedProfiles.collectAsState()

    BisqScrollLayout(
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        isInteractive = presenter.isInteractive.collectAsState().value
    ) {

        Column {
            PriceProfileCard(
                price = presenter.formattedMarketPrice.collectAsState().value,
                priceText = "dashboard.marketPrice".i18n()
            )
            BisqGap.V1()
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    PriceProfileCard(
                        price = offersOnline.toString(),
                        priceText = "dashboard.offersOnline".i18n()
                    )
                }
                BisqGap.H1()
                Box(modifier = Modifier.weight(1f)) {
                    PriceProfileCard(
                        price = publishedProfiles.toString(),
                        priceText = "dashboard.activeUsers".i18n()
                    )
                }
            }
        }
        // BisqButton("bisqEasy.tradeGuide.tabs.headline".i18n(), onClick = { presenter.navigateToGuide() })
        WelcomeCard(
            presenter = presenter,
            titleKey = presenter.titleKey,
            bulletPointsKey = presenter.bulletPointsKey,
            primaryButtonText = "mobile.dashboard.startTrading".i18n(),
            footerLink = "action.learnMore".i18n()
        )
    }
}

@Composable
fun WelcomeCard(
    presenter: DashboardPresenter,
    titleKey: String,
    bulletPointsKey: List<String>,
    primaryButtonText: String,
    footerLink: String
) {
    BisqCard(
        padding = BisqUIConstants.ScreenPadding2X,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {
        // Title
        BisqText.h4Regular(titleKey.i18n())

        // Bullet Points
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalfQuarter)) {
            bulletPointsKey.forEach { pointKey ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val whiteColor = BisqTheme.colors.white
                    Canvas(modifier = Modifier.size(BisqUIConstants.ScreenPaddingHalf)) {
                        drawCircle(color = whiteColor)
                    }
                    BisqGap.H1()
                    BisqText.smallMedium(pointKey.i18n())
                }
            }
        }

        // Primary Button
        BisqButton(
            primaryButtonText,
            fullWidth = true,
            onClick = { presenter.onStartTrading() },
        )

        // Footer Link
        LinkButton(
            footerLink,
            link = BisqLinks.BISQ_EASY_WIKI_URL,
            onClick = { presenter.navigateLearnMore() }
        )
    }
}

@Composable
fun PriceProfileCard(price: String, priceText: String) {
    BisqCard(
        borderRadius = BisqUIConstants.ScreenPaddingQuarter,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AmountWithCurrency(price)

        BisqGap.V1()

        BisqText.smallRegularGrey(
            text = priceText,
            textAlign = TextAlign.Center,
        )
    }
}
