package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.UpIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxStyle
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun TradeDetailsComposable() {
    val presenter: TradeDetailsHeaderPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val item: TradeItemPresentationModel = presenter.selectedTrade.value!!
    val strings = LocalStrings.current.bisqEasyTradeState
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val interruptTradeButtonVisible by presenter.interruptTradeButtonVisible.collectAsState()
    val interruptTradeButtonText by presenter.interruptTradeButtonText.collectAsState()

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
    var showDetails by remember { mutableStateOf(false) }

    val transitionState = remember {
        MutableTransitionState(showDetails).apply {
            targetState = !showDetails
        }
    }
    val transition = rememberTransition(transitionState)
    val arrowRotationDegree by transition.animateFloat({
        tween(durationMillis = 300)
    }) {
        if (showDetails) 0f else 180f
    }

    Row(modifier = Modifier.clip(shape = RoundedCornerShape(12.dp))) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(color = BisqTheme.colors.dark5)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BisqText.baseRegular(
                        text = item.directionalTitle.uppercase(), // 'Buying from:' or 'Selling to:'
                    )

                    BisqGap.H1()

                    UserProfile(item.peersUserProfile)
                }
            }

            BisqGap.VHalf()

            InfoRow(
                style = InfoBoxStyle.Style2,
                label1 = presenter.leftAmountDescription,
                value1 = "${presenter.leftAmount} ${presenter.leftCode}",
                label2 = presenter.rightAmountDescription,
                value2 = "${presenter.rightAmount} ${presenter.rightCode}",
            )

            AnimatedVisibility(
                visible = showDetails,
                enter = enterTransition,
                exit = exitTransition
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.weight(2f)
                ) {
                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = stringsBisqEasy.bisqEasy_openTrades_table_price,
                        value1 = item.formattedPrice,
                        label2 = "Trade date", // -> bisqEasy.openTrades.tradeDetails.tradeDate  //strings.bisqEasy_tradeCompleted_body_date,
                        value2 = "${item.formattedDate} ${item.formattedTime}",
                    )

                    BisqGap.V1()

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = stringsBisqEasy.bisqEasy_offerbook_offerList_table_columns_settlementMethod,
                        value1 = "${item.fiatPaymentMethodDisplayString} / ${item.bitcoinSettlementMethodDisplayString}",
                        label2 = strings.bisqEasy_tradeState_header_tradeId,
                        value2 = item.shortTradeId,
                    )

                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val buttonAlpha: Float = if (interruptTradeButtonVisible) 1f else 0f
                BisqButton(
                    modifier = Modifier.alpha(buttonAlpha),
                    text = interruptTradeButtonText,
                    onClick = { presenter.onInterruptTrade() },
                    type = BisqButtonType.Outline,
                    color = BisqTheme.colors.primary,
                    borderColor = BisqTheme.colors.primary,
                    // padding = PaddingValues(horizontal = 70.dp, vertical = 6.dp)
                )
                IconButton(onClick = { showDetails = !showDetails }) {
                    //TODO icon is not high resolution
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
