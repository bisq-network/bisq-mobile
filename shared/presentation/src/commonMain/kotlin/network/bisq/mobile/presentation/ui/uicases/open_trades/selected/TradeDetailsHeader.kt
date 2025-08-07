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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isSell
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.UpIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.UserProfileRow
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxCurrency
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxStyle
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun TradeDetailsHeader() {
    val presenter: TradeDetailsHeaderPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val item: TradeItemPresentationModel = presenter.selectedTrade.collectAsState().value ?: return
    val interruptTradeButtonText by presenter.interruptTradeButtonText.collectAsState()
    val openMediationButtonText by presenter.openMediationButtonText.collectAsState()
    val isInMediation by presenter.isInMediation.collectAsState()
    val leftAmount by presenter.leftAmount.collectAsState()
    val leftCode by presenter.leftCode.collectAsState()
    val rightAmount by presenter.rightAmount.collectAsState()
    val rightCode by presenter.rightCode.collectAsState()
    val peerAvatar: PlatformImage? by presenter.peerAvatar.collectAsState()

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
    val showDetails by presenter.isShowDetails.collectAsState()

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

    val isSell = presenter.directionEnum.isSell
    val isInteractive = presenter.isInteractive.collectAsState().value

    Row(modifier = Modifier.clip(shape = RoundedCornerShape(12.dp))) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(color = BisqTheme.colors.dark_grey40)
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

                    UserProfileRow(
                        user = item.peersUserProfile,
                        reputation = item.peersReputationScore,
                        showUserName = true,
                        userAvatar = peerAvatar,
                    )
                }
            }

            BisqGap.VHalf()

            if (presenter.isSmallScreen()) {
                if (isSell) {
                    InfoBoxSats(label = presenter.leftAmountDescription, value = leftAmount)
                } else {
                    InfoBoxCurrency(
                        label = presenter.leftAmountDescription,
                        value = "$leftAmount $leftCode"
                    )
                }
                if (isSell) {
                    InfoBoxCurrency(
                        label = presenter.rightAmountDescription,
                        value = "$rightAmount $rightCode"
                    )
                } else {
                    InfoBoxSats(label = presenter.rightAmountDescription, value = rightAmount)
                }
            } else {
                InfoRowContainer {
                    if (isSell) {
                        InfoBoxSats(label = presenter.leftAmountDescription, value = leftAmount)
                    } else {
                        InfoBoxCurrency(
                            label = presenter.leftAmountDescription,
                            value = "$leftAmount $leftCode"
                        )
                    }
                    if (isSell) {
                        InfoBoxCurrency(
                            label = presenter.rightAmountDescription,
                            value = "$rightAmount $rightCode",
                            rightAlign = true
                        )
                    } else {
                        InfoBoxSats(
                            label = presenter.rightAmountDescription,
                            value = rightAmount,
                            rightAlign = true
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showDetails,
                enter = enterTransition,
                exit = exitTransition
            ) {

                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.weight(2f)
                ) {
                    if (presenter.isSmallScreen()) {
                        InfoBox(
                            label = "bisqEasy.openTrades.table.price".i18n(),
                            value = item.formattedPrice,
                            style = InfoBoxStyle.Style2,
                        )
                        BisqGap.V1()
                        InfoBox(
                            label = "bisqEasy.openTrades.tradeDetails.tradeDate".i18n(),
                            value = "${item.formattedDate} ${item.formattedTime}",
                            style = InfoBoxStyle.Style2,
                        )
                    } else {
                        InfoRow(
                            style = InfoBoxStyle.Style2,
                            label1 = "bisqEasy.openTrades.table.price".i18n(),
                            value1 = item.formattedPrice,
                            label2 = "bisqEasy.openTrades.tradeDetails.tradeDate".i18n(),
                            value2 = "${item.formattedDate} ${item.formattedTime}",
                        )
                    }

                    BisqGap.V1()

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = "bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n(),
                        value1 = "${item.fiatPaymentMethodDisplayString} / ${item.bitcoinSettlementMethodDisplayString}",
                        label2 = "bisqEasy.tradeState.header.tradeId".i18n(),
                        value2 = item.shortTradeId,
                    )

                    val showInterruptTradeButton = interruptTradeButtonText.isNotEmpty()
                    val showMediationButton = !isInMediation && openMediationButtonText.isNotEmpty()
                    if (showInterruptTradeButton || showMediationButton) {
                        BisqGap.V2()

                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showInterruptTradeButton) {
                                BisqButton(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    text = interruptTradeButtonText,
                                    onClick = { presenter.onOpenInterruptionConfirmationDialog() },
                                    type = BisqButtonType.Outline,
                                )
                            }
                            if (showMediationButton) {
                                BisqButton(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    text = openMediationButtonText,
                                    onClick = { presenter.onOpenMediationConfirmationDialog() },
                                    type = BisqButtonType.GreyOutline,
                                )
                            }
                        }

                        BisqGap.VHalf()
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { presenter.onToggleHeader() },
                    enabled = isInteractive,
                ) {
                    UpIcon(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(shape = RoundedCornerShape(16.dp))
                            .rotate(arrowRotationDegree)
                            .background(color = BisqTheme.colors.primary)
                    )
                }
            }
        }
    }

}
