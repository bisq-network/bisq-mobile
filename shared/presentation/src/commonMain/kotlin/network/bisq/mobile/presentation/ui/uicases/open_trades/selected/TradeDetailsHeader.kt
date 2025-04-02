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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
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
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxStyle
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.ui.components.organisms.trades.CancelTradeDialog
import network.bisq.mobile.presentation.ui.components.organisms.trades.OpenMediationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun TradeDetailsHeader() {
    val presenter: TradeDetailsHeaderPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val item: TradeItemPresentationModel = presenter.selectedTrade.value!!
    val interruptTradeButtonText by presenter.interruptTradeButtonText.collectAsState()
    val openMediationButtonText by presenter.openMediationButtonText.collectAsState()
    val isInMediation by presenter.isInMediation.collectAsState()
    val tradeCloseType by presenter.tradeCloseType.collectAsState()
    val showInterruptionConfirmationDialog by presenter.showInterruptionConfirmationDialog.collectAsState()
    val showMediationConfirmationDialog by presenter.showMediationConfirmationDialog.collectAsState()

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

    val isSell = presenter.directionEnum.isSell

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
                        item.peersUserProfile,
                        item.peersReputationScore,
                        true
                    )
                }
            }

            BisqGap.VHalf()

            if (presenter.isSmallScreen()) {
                if (isSell) {
                    InfoBoxSats(label = presenter.leftAmountDescription, value = presenter.leftAmount)
                } else {
                    InfoBox(
                        label = presenter.leftAmountDescription,
                        value = "${presenter.leftAmount} ${presenter.leftCode}"
                    )
                }
                if (isSell) {
                    InfoBox(
                        label = presenter.rightAmountDescription,
                        value = "${presenter.rightAmount} ${presenter.rightCode}"
                    )
                } else {
                    InfoBoxSats(label = presenter.rightAmountDescription, value = presenter.rightAmount)
                }
            } else {
                InfoRowContainer {
                    if (isSell) {
                        InfoBoxSats(label = presenter.leftAmountDescription, value = presenter.leftAmount)
                    } else {
                        InfoBox(
                            label = presenter.leftAmountDescription,
                            value = "${presenter.leftAmount} ${presenter.leftCode}"
                        )
                    }
                    if (isSell) {
                        InfoBox(
                            label = presenter.rightAmountDescription,
                            value = "${presenter.rightAmount} ${presenter.rightCode}"
                        )
                    } else {
                        InfoBoxSats(label = presenter.rightAmountDescription, value = presenter.rightAmount)
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (showInterruptTradeButton) {
                                BisqButton(
                                    text = interruptTradeButtonText,
                                    onClick = { presenter.onOpenInterruptionConfirmationDialog() },
                                    type = BisqButtonType.Outline,
                                )
                            }
                            if (showMediationButton) {
                                BisqButton(
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
                IconButton(onClick = { showDetails = !showDetails }) {
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

    if (showInterruptionConfirmationDialog) {
        CancelTradeDialog(
            onCancelConfirm = { presenter.onInterruptTrade() },
            onDismiss = { presenter.onCloseInterruptionConfirmationDialog() },
            isBuyer = presenter.directionEnum.isBuy,
            isRejection = tradeCloseType == TradeDetailsHeaderPresenter.TradeCloseType.REJECT
        )
    }

    if (showMediationConfirmationDialog) {
        OpenMediationDialog(
            onCancelConfirm = { presenter.onOpenMediation() },
            onDismiss = { presenter.onCloseMediationConfirmationDialog() },
        )
    }
}
