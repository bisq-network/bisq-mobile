package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.UpIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxStyle
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun TradeHeader(
    offer: OfferListItemVO,
) {
    val strings = LocalStrings.current.bisqEasyTradeState
    val stringsBisqEasy = LocalStrings.current.bisqEasy

    val enterTransition = remember {
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeIn(
            initialAlpha = 0.3f,
            animationSpec = tween(300)
        )
    }
    val exitTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }
    var visible by remember { mutableStateOf(false) }

    val transitionState = remember {
        MutableTransitionState(visible).apply {
            targetState = !visible
        }
    }
    val transition = rememberTransition(transitionState)
    val arrowRotationDegree by transition.animateFloat({
        tween(durationMillis = 300)
    }) {
        if (visible) 0f else 180f
    }

    Row(modifier = Modifier.clip(shape = RoundedCornerShape(12.dp))) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(color = BisqTheme.colors.dark5)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(3f)) {
                    UserProfile(offer)
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(2f)
                ) {
                    BisqText.smallRegular(text = "10000.02 USD")
                    BisqText.smallRegular(text = "0.00173399 BTC")
                }
            }
            AnimatedVisibility(
                visible = visible,
                enter = enterTransition,
                exit = exitTransition
            ) {
                Column {

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = strings.bisqEasy_tradeState_header_tradeId,
                        value1 = "07b9bab1",
                        label2 = strings.bisqEasy_tradeCompleted_body_date,
                        value2 = "29 Sep 2024",
                    )

                    BisqGap.V1()

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = "Floating percentage",
                        value1 = "1.71%",
                        label2 = stringsBisqEasy.bisqEasy_openTrades_table_price,
                        value2 = "9567056.04 USD/BTC",
                    )

                    BisqGap.V1()

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = strings.bisqEasy_tradeCompleted_header_paymentMethod,
                        value1 = "CashApp",
                        label2 = stringsBisqEasy.bisqEasy_offerbook_offerList_table_columns_settlementMethod,
                        value2 = "Lightning",
                    )

                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BisqButton(
                    text = stringsBisqEasy.bisqEasy_openTrades_cancelTrade,
                    color = BisqTheme.colors.grey1,
                    onClick = {},
                    backgroundColor = Color.Transparent,
                    padding = PaddingValues(horizontal = 70.dp, vertical = 6.dp)
                )
                IconButton(
                    onClick = {
                        visible = !visible
                    }
                ) {
                    UpIcon(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(shape = RoundedCornerShape(12.dp))
                            .rotate(arrowRotationDegree)
                            .background(color = BisqTheme.colors.primary)
                    )
                }
            }
        }
    }
}
