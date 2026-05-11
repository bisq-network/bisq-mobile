@file:Suppress("ktlint:compose:vm-forwarding-check")

package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.formatters.TradeDurationFormatter
import network.bisq.mobile.domain.utils.StringUtils.truncateBitcoinIdentifier
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.UpIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileRow
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxCurrency
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxRow
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.common.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@ExcludeFromCoverage
@Composable
fun TradeDetailsHeader(presenter: TradeDetailsHeaderPresenter = koinInject()) {
    RememberPresenterLifecycle(presenter)

    val tradeUiState by presenter.tradeUiState.collectAsState()
    val sessionUiState by presenter.sessionUiState.collectAsState()

    tradeUiState?.let { tradeState ->
        TradeDetailsHeaderContent(
            tradeUiState = tradeState,
            sessionUiState = sessionUiState,
            userProfileIconProvider = presenter.userProfileIconProvider,
            onAction = presenter::onAction,
        )
    }
}

@Composable
fun TradeDetailsHeaderContent(
    tradeUiState: TradeDetailsHeaderTradeUiState,
    sessionUiState: TradeDetailsHeaderSessionUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onAction: (TradeDetailsHeaderUiAction) -> Unit,
) {
    val enterTransition =
        remember {
            expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(300),
            ) +
                fadeIn(
                    initialAlpha = 0.3f,
                    animationSpec = tween(300),
                )
        }
    val exitTransition =
        remember {
            shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(300),
            ) +
                fadeOut(
                    animationSpec = tween(300),
                )
        }

    val arrowRotationDegree by animateFloatAsState(
        targetValue = if (sessionUiState.showDetails) 0f else 180f,
        animationSpec = tween(durationMillis = 300),
        label = "tradeDetailsHeaderArrowRotation",
    )

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .fillMaxWidth()
                .background(color = BisqTheme.colors.dark_grey40)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BisqText.BaseRegular(
                text = tradeUiState.directionalTitle.uppercase(), // 'Buying from:' or 'Selling to:'
            )

            BisqGap.H1()

            UserProfileRow(
                userProfile = tradeUiState.peersUserProfile,
                reputation = tradeUiState.peersReputationScore,
                showUserName = true,
                userProfileIconProvider = userProfileIconProvider,
            )
        }

        BisqGap.VHalf()

        if (sessionUiState.isCompleted) {
            val paymentProof = sessionUiState.paymentProof
            if (!paymentProof.isNullOrBlank()) {
                val paymentProofLabel =
                    if (tradeUiState.isMainChainPayment) {
                        "bisqEasy.tradeState.paymentProof.MAIN_CHAIN".i18n()
                    } else {
                        "bisqEasy.tradeState.paymentProof.LN".i18n()
                    }
                if (tradeUiState.isMainChainPayment) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy((-4).dp),
                    ) {
                        InfoBoxRow(
                            label = paymentProofLabel,
                            value = paymentProof.truncateBitcoinIdentifier(),
                            fullValueToCopy = paymentProof,
                            showCopy = true,
                        )
                        val explorerUrl = "https://mempool.space/tx/$paymentProof"
                        LinkButton(
                            text = "mobile.bisqEasy.openTrades.tradeDetails.viewInBlockExplorer".i18n(),
                            link = explorerUrl,
                            forceConfirm = true,
                            padding = PaddingValues(0.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 0.dp),
                        )
                    }
                } else {
                    InfoBoxRow(
                        label = paymentProofLabel,
                        value = paymentProof.truncateBitcoinIdentifier(),
                        fullValueToCopy = paymentProof,
                        showCopy = true,
                    )
                }
            }
        } else {
            if (tradeUiState.isSmallScreen) {
                if (tradeUiState.isSell) {
                    InfoBoxSats(label = tradeUiState.leftAmountDescription, value = tradeUiState.leftAmount)
                } else {
                    InfoBoxCurrency(
                        label = tradeUiState.leftAmountDescription,
                        value = "${tradeUiState.leftAmount} ${tradeUiState.leftCode}",
                    )
                }
                if (tradeUiState.isSell) {
                    InfoBoxCurrency(
                        label = tradeUiState.rightAmountDescription,
                        value = "${tradeUiState.rightAmount} ${tradeUiState.rightCode}",
                    )
                } else {
                    InfoBoxSats(label = tradeUiState.rightAmountDescription, value = tradeUiState.rightAmount)
                }
            } else {
                InfoRowContainer {
                    if (tradeUiState.isSell) {
                        InfoBoxSats(label = tradeUiState.leftAmountDescription, value = tradeUiState.leftAmount)
                    } else {
                        InfoBoxCurrency(
                            label = tradeUiState.leftAmountDescription,
                            value = "${tradeUiState.leftAmount} ${tradeUiState.leftCode}",
                        )
                    }
                    if (tradeUiState.isSell) {
                        InfoBoxCurrency(
                            label = tradeUiState.rightAmountDescription,
                            value = "${tradeUiState.rightAmount} ${tradeUiState.rightCode}",
                            rightAlign = true,
                        )
                    } else {
                        InfoBoxSats(
                            label = tradeUiState.rightAmountDescription,
                            value = tradeUiState.rightAmount,
                            rightAlign = true,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = sessionUiState.showDetails,
            enter = enterTransition,
            exit = exitTransition,
        ) {
            if (sessionUiState.isCompleted) {
                CompletedTradeDetailsSection(
                    tradeUiState = tradeUiState,
                    sessionUiState = sessionUiState,
                )
            } else {
                ActiveTradeDetailsSection(
                    tradeUiState = tradeUiState,
                    sessionUiState = sessionUiState,
                    onAction = onAction,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onAction(TradeDetailsHeaderUiAction.ToggleHeader) },
                enabled = sessionUiState.isInteractive,
            ) {
                UpIcon(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(shape = RoundedCornerShape(16.dp))
                            .rotate(arrowRotationDegree)
                            .background(color = BisqTheme.colors.primary),
                )
            }
        }
    }
}

@Composable
private fun ActiveTradeDetailsSection(
    tradeUiState: TradeDetailsHeaderTradeUiState,
    sessionUiState: TradeDetailsHeaderSessionUiState,
    onAction: (TradeDetailsHeaderUiAction) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (tradeUiState.isSmallScreen) {
            InfoBox(
                label = "bisqEasy.openTrades.table.price".i18n(),
                value = tradeUiState.priceDisplay,
            )
            BisqGap.V1()
            InfoBox(
                label = "bisqEasy.openTrades.tradeDetails.tradeDate".i18n(),
                value = "${tradeUiState.formattedDate} ${tradeUiState.formattedTime}",
            )
        } else {
            InfoRow(
                label1 = "bisqEasy.openTrades.table.price".i18n(),
                value1 = tradeUiState.priceDisplay,
                label2 = "bisqEasy.openTrades.tradeDetails.tradeDate".i18n(),
                value2 = "${tradeUiState.formattedDate} ${tradeUiState.formattedTime}",
            )
        }

        BisqGap.V1()

        InfoRow(
            label1 = "bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n(),
            value1 = "${tradeUiState.fiatPaymentMethodDisplayString} / ${tradeUiState.bitcoinSettlementMethodDisplayString}",
            label2 = "bisqEasy.tradeState.header.tradeId".i18n(),
            value2 = tradeUiState.shortTradeId,
        )

        BisqGap.V1()

        InfoBox(
            label = "bisqEasy.mediator".i18n(),
            value = tradeUiState.mediatorUserName ?: "",
        )

        val showInterruptTradeButton = sessionUiState.interruptTradeButtonText.isNotEmpty()
        val showMediationButton = !sessionUiState.isInMediation && sessionUiState.openMediationButtonText.isNotEmpty()
        if (showInterruptTradeButton || showMediationButton) {
            BisqGap.V2()

            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showInterruptTradeButton) {
                    BisqButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        text = sessionUiState.interruptTradeButtonText,
                        onClick = { onAction(TradeDetailsHeaderUiAction.OpenInterruptionConfirmationDialog) },
                        type = BisqButtonType.Outline,
                    )
                }
                if (showMediationButton) {
                    BisqButton(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        text = sessionUiState.openMediationButtonText,
                        onClick = { onAction(TradeDetailsHeaderUiAction.OpenMediationConfirmationDialog) },
                        type = BisqButtonType.WarningOutline,
                    )
                }
            }

            BisqGap.VHalf()
        }
    }
}

@Composable
private fun CompletedTradeDetailsSection(
    tradeUiState: TradeDetailsHeaderTradeUiState,
    sessionUiState: TradeDetailsHeaderSessionUiState,
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        InfoBox(
            label = "bisqEasy.openTrades.table.price".i18n(),
            value = tradeUiState.priceDisplay,
        )

        BisqGap.V1()

        InfoBox(
            label = "bisqEasy.openTrades.tradeDetails.tradeDate".i18n(),
            value = "${tradeUiState.formattedDate} ${tradeUiState.formattedTime}",
        )

        BisqGap.V1()

        InfoBox(
            label = "bisqEasy.openTrades.tradeDetails.tradeDuration".i18n(),
            value = sessionUiState.formattedTradeDuration,
        )

        BisqGap.V1()

        InfoBox(
            label = "bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n(),
            value = "${tradeUiState.fiatPaymentMethodDisplayString} / ${tradeUiState.bitcoinSettlementMethodDisplayString}",
        )

        BisqGap.V1()

        InfoBoxRow(
            label = "bisqEasy.openTrades.tradeDetails.tradeId".i18n(),
            value = tradeUiState.shortTradeId,
            fullValueToCopy = tradeUiState.tradeId,
            showCopy = true,
        )

        if (!tradeUiState.peerNetworkAddress.isNullOrBlank()) {
            BisqGap.V1()
            InfoBoxRow(
                label = "bisqEasy.openTrades.tradeDetails.peerNetworkAddress".i18n(),
                value = tradeUiState.peerNetworkAddress.truncateBitcoinIdentifier(),
                fullValueToCopy = tradeUiState.peerNetworkAddress,
                showCopy = true,
            )
        }

        if (!sessionUiState.receiverAddress.isNullOrBlank()) {
            BisqGap.V1()
            InfoBoxRow(
                label = "bisqEasy.openTrades.tradeDetails.btcPaymentAddress".i18n(),
                value = sessionUiState.receiverAddress.truncateBitcoinIdentifier(),
                fullValueToCopy = sessionUiState.receiverAddress,
                showCopy = true,
            )
        }
    }
}

@ExcludeFromCoverage
private fun previewFormattedTradeDuration(isCompleted: Boolean): String =
    if (isCompleted) {
        TradeDurationFormatter.formatAge(
            tradeCompletedDate = 1_720_000_000_000L,
            takeOfferDate = 1_719_971_200_000L,
        )
    } else {
        ""
    }

@ExcludeFromCoverage
private fun previewTradeDetailsHeaderStates(
    isSell: Boolean,
    showDetails: Boolean,
    isCompleted: Boolean,
    isMainChainPayment: Boolean = true,
    paymentProof: String? = null,
    peerNetworkAddress: String? = null,
    receiverAddress: String? = null,
): Pair<TradeDetailsHeaderTradeUiState, TradeDetailsHeaderSessionUiState> =
    if (isSell) {
        TradeDetailsHeaderTradeUiState(
            directionalTitle = "Selling to:",
            peersUserProfile = createMockUserProfile("LightningLover99"),
            peersReputationScore = ReputationScoreVO(totalScore = 980, fiveSystemScore = 4.2, ranking = 34),
            priceDisplay = "68,333.00 EUR/BTC",
            formattedDate = "Mar 26, 2026",
            formattedTime = "09:15",
            fiatPaymentMethodDisplayString = "Revolut",
            bitcoinSettlementMethodDisplayString = "Lightning",
            shortTradeId = "t-xyz789a",
            tradeId = "t-xyz789abc123def456",
            mediatorUserName = null,
            isSell = true,
            isSmallScreen = false,
            leftAmountDescription = "Send",
            rightAmountDescription = "Receive",
            leftAmount = "0.01200000",
            leftCode = "BTC",
            rightAmount = "820.00",
            rightCode = "EUR",
            isMainChainPayment = isMainChainPayment,
            peerNetworkAddress = peerNetworkAddress,
        ) to
            TradeDetailsHeaderSessionUiState(
                showDetails = showDetails,
                isInteractive = true,
                interruptTradeButtonText = "",
                openMediationButtonText = "",
                isInMediation = false,
                paymentProof = paymentProof,
                receiverAddress = receiverAddress,
                isCompleted = isCompleted,
                formattedTradeDuration = previewFormattedTradeDuration(isCompleted),
            )
    } else {
        TradeDetailsHeaderTradeUiState(
            directionalTitle = "Buying from:",
            peersUserProfile = createMockUserProfile("SatoshiFan42"),
            peersReputationScore = ReputationScoreVO(totalScore = 1234, fiveSystemScore = 4.8, ranking = 18),
            priceDisplay = "68,420.00 USD/BTC",
            formattedDate = "Mar 27, 2026",
            formattedTime = "14:32",
            fiatPaymentMethodDisplayString = "SEPA",
            bitcoinSettlementMethodDisplayString = "On-chain",
            shortTradeId = "t-abc123d",
            tradeId = "t-abc123def456ghi789",
            mediatorUserName = if (isCompleted) null else "Mediator01",
            isSell = false,
            isSmallScreen = false,
            leftAmountDescription = "Pay",
            rightAmountDescription = "Receive",
            leftAmount = "342.10",
            leftCode = "USD",
            rightAmount = "0.00500000",
            rightCode = "BTC",
            isMainChainPayment = isMainChainPayment,
            peerNetworkAddress = peerNetworkAddress,
        ) to
            TradeDetailsHeaderSessionUiState(
                showDetails = showDetails,
                isInteractive = true,
                interruptTradeButtonText = if (isCompleted) "" else "Cancel trade",
                openMediationButtonText = if (isCompleted) "" else "Request mediation",
                isInMediation = false,
                paymentProof = paymentProof,
                receiverAddress = receiverAddress,
                isCompleted = isCompleted,
                formattedTradeDuration = previewFormattedTradeDuration(isCompleted),
            )
    }

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

@Preview
@Composable
private fun TradeDetailsHeaderContent_ExpandedPreview() {
    BisqTheme.Preview {
        val (tradeUiState, sessionUiState) = previewTradeDetailsHeaderStates(isSell = false, showDetails = true, isCompleted = false)
        TradeDetailsHeaderContent(
            tradeUiState = tradeUiState,
            sessionUiState = sessionUiState,
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TradeDetailsHeaderContent_CollapsedPreview() {
    BisqTheme.Preview {
        val (tradeUiState, sessionUiState) = previewTradeDetailsHeaderStates(isSell = true, showDetails = false, isCompleted = false)
        TradeDetailsHeaderContent(
            tradeUiState = tradeUiState,
            sessionUiState = sessionUiState,
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TradeDetailsHeaderContent_CompletedExpandedPreview() {
    BisqTheme.Preview {
        val (tradeUiState, sessionUiState) =
            previewTradeDetailsHeaderStates(
                isSell = false,
                showDetails = true,
                isCompleted = true,
                paymentProof = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6abcd",
                peerNetworkAddress = "runbtcx3wfygbq2wdde6qzjnpyrqn3gvbks7t5jdymmunxttdvvttpyd.onion:9999",
                receiverAddress = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
            )
        TradeDetailsHeaderContent(
            tradeUiState = tradeUiState,
            sessionUiState = sessionUiState,
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TradeDetailsHeaderContent_CompletedCollapsedPreview() {
    BisqTheme.Preview {
        val (tradeUiState, sessionUiState) =
            previewTradeDetailsHeaderStates(
                isSell = true,
                showDetails = false,
                isCompleted = true,
                paymentProof = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6abcd",
            )
        TradeDetailsHeaderContent(
            tradeUiState = tradeUiState,
            sessionUiState = sessionUiState,
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun TradeDetailsHeaderContent_CompletedLightningExpandedPreview() {
    BisqTheme.Preview {
        val (tradeUiState, sessionUiState) =
            previewTradeDetailsHeaderStates(
                isSell = true,
                showDetails = true,
                isCompleted = true,
                isMainChainPayment = false,
                paymentProof = "preimage_2f4b0d9a7c1e6f3a9b8d4c2e1a7f5b3d",
                peerNetworkAddress = "runbtcx3wfygbq2wdde6qzjnpyrqn3gvbks7t5jdymmunxttdvvttpyd.onion:9999",
                receiverAddress = "lnbc2500n1pn8m0vfpp5g7t6h4y8k9q2x3w5e7r9t2u4i6o8p0a9s7d6f5g4h3j2k1l",
            )
        TradeDetailsHeaderContent(
            tradeUiState = tradeUiState,
            sessionUiState = sessionUiState,
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}
