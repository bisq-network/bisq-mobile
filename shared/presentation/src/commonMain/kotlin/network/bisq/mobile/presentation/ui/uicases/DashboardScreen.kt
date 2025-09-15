package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_chat
import bisqapps.shared.presentation.generated.resources.icon_markets
import bisqapps.shared.presentation.generated.resources.icon_payment
import bisqapps.shared.presentation.generated.resources.reputation
import bisqapps.shared.presentation.generated.resources.thumbs_up
import kotlinx.coroutines.delay
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.components.molecules.AmountWithCurrency
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
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

    DashboardContent(
        offersOnline = offersOnline,
        publishedProfiles = publishedProfiles,
        numConnections = numConnections,
        isInteractive = isInteractive,
        marketPrice = marketPrice,
        tradeRulesConfirmed = tradeRulesConfirmed,
        onNavigateToMarkets = presenter::onNavigateToMarkets,
        onOpenTradeGuide = presenter::onOpenTradeGuide
    )
}

@Composable
private fun DashboardContent(
    offersOnline: Number,
    publishedProfiles: Number,
    numConnections: Number,
    isInteractive: Boolean,
    marketPrice: String,
    tradeRulesConfirmed: Boolean,
    onNavigateToMarkets: () -> Unit,
    onOpenTradeGuide: () -> Unit
) {
    val padding = BisqUIConstants.ScreenPadding
    BisqScrollLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.spacedBy(padding),
        isInteractive = isInteractive,
    ) {
        Column {
            HomeInfoCard(
                price = marketPrice,
                text = "dashboard.marketPrice".i18n()
            )
            BisqGap.V1()
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardWidth: Dp = maxWidth / 2  // Each card uses 50%
                val modifier = Modifier.width(cardWidth)
                SlidingCards(
                    listOf(
                        {
                            HomeInfoCard(
                                modifier = modifier,
                                price = offersOnline.toString(),
                                text = "dashboard.offersOnline".i18n()
                            )
                        },
                        {
                            HomeInfoCard(
                                modifier = modifier,
                                price = numConnections.toString(),
                                text = "dashboard.numConnections".i18n()
                            )
                        },
                        {
                            HomeInfoCard(
                                modifier = modifier,
                                price = publishedProfiles.toString(),
                                text = "dashboard.activeUsers".i18n()
                            )
                        }
                    )
                )
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
                    Pair("bisqEasy.onboarding.top.content3".i18n(), Res.drawable.icon_chat)
                ),
                buttonText = "support.resources.guides.tradeGuide".i18n(),
                buttonHandler = onOpenTradeGuide
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
fun SlidingCards(
    cards: List<@Composable () -> Unit>,
    cardSpacing: Dp = BisqUIConstants.ScreenPadding,
    slideDuration: Int = 600,
    delayMillis: Int = 5000
) {
    require(cards.size >= 3) { "Need at least 3 cards for sliding animation" }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - cardSpacing) / 2
        val density = LocalDensity.current

        // Indexes
        var startIndex by remember { mutableStateOf(0) }
        val leftIndex = startIndex % cards.size
        val rightIndex = (startIndex + 1) % cards.size
        val nextIndex = (startIndex + 2) % cards.size

        val slideAnim = remember { Animatable(0f) }

        LaunchedEffect(startIndex) {
            delay(delayMillis.toLong()) // Wait before sliding
            slideAnim.animateTo(
                targetValue = -with(density) { (cardWidth + cardSpacing).toPx() },
                animationSpec = tween(durationMillis = slideDuration)
            )
            slideAnim.snapTo(0f)
            startIndex = (startIndex + 1) % cards.size
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            // Left card
            Box(
                modifier = Modifier
                    .offset { IntOffset(slideAnim.value.toInt(), 0) }
                    .width(cardWidth)
                    .fillMaxHeight()
            ) {
                cards[leftIndex]()
            }

            // Right card
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (slideAnim.value + with(density) { (cardWidth + cardSpacing).toPx() }).toInt(),
                            0
                        )
                    }
                    .width(cardWidth)
                    .fillMaxHeight()
            ) {
                cards[rightIndex]()
            }

            // Next card coming from right
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (slideAnim.value + 2 * with(density) { (cardWidth + cardSpacing).toPx() }).toInt(),
                            0
                        )
                    }
                    .width(cardWidth)
                    .fillMaxHeight()
            ) {
                cards[nextIndex]()
            }
        }
    }
}


@Composable
private fun DashboardContentPreview(
    language: String = "en",
    tradeRulesConfirmed: Boolean = true
) {
    BisqTheme.Preview(language = language) {
        DashboardContent(
            offersOnline = 1,
            publishedProfiles = 2,
            numConnections = 8,
            isInteractive = true,
            marketPrice = "111247.40 BTC/USD",
            tradeRulesConfirmed = tradeRulesConfirmed,
            onNavigateToMarkets = {},
            onOpenTradeGuide = {}
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
private fun DashboardContentPreview_Ru() = DashboardContentPreview("ru", true)

@Preview
@Composable
private fun DashboardContentPreview_RuRulesNotConfirmed() = DashboardContentPreview("ru", false)


