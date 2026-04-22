package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.utils.StringUtils.truncateBitcoinIdentifier
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [TradeDetailsHeaderContent] using Robolectric.
 *
 * Verifies rendering for trade/session state combinations and that interactions dispatch
 * the correct [TradeDetailsHeaderUiAction].
 */
@RunWith(AndroidJUnit4::class)
class TradeDetailsHeaderContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockOnAction: (TradeDetailsHeaderUiAction) -> Unit

    private val userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

    private companion object {
        const val DEFAULT_PEER_USER_NAME: String = "SatoshiFan42"
        const val INTERRUPT_TRADE_BUTTON: String = "Cancel trade"
        const val OPEN_MEDIATION_BUTTON: String = "Request mediation"
        const val LONG_MAIN_CHAIN_PAYMENT_PROOF: String =
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6abcd"
        const val SAMPLE_TOR_PEER: String =
            "runbtcx3wfygbq2wdde6qzjnpyrqn3gvbks7t5jdymmunxttdvvttpyd.onion:9999"
        const val SAMPLE_RECEIVER_BTC: String = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"
        const val SAMPLE_TRADE_DURATION: String = "3h 15m"
    }

    private fun blockExplorerLinkText(): String = "mobile.bisqEasy.openTrades.tradeDetails.viewInBlockExplorer".i18n()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        mockOnAction = mockk(relaxed = true)
    }

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        content()
                    }
                }
            }
        }
    }

    private fun baseTradeUiState(
        directionalTitle: String = "Buying from:",
        userName: String = DEFAULT_PEER_USER_NAME,
        isSell: Boolean = false,
        isSmallScreen: Boolean = false,
        mediatorUserName: String? = "Mediator01",
        isMainChainPayment: Boolean = true,
        peerNetworkAddress: String? = null,
    ): TradeDetailsHeaderTradeUiState =
        TradeDetailsHeaderTradeUiState(
            directionalTitle = directionalTitle,
            peersUserProfile = createMockUserProfile(userName),
            peersReputationScore = ReputationScoreVO(totalScore = 100L, fiveSystemScore = 4.5, ranking = 10),
            priceDisplay = "68,420.00 USD/BTC",
            formattedDate = "Mar 27, 2026",
            formattedTime = "14:32",
            fiatPaymentMethodDisplayString = "SEPA",
            bitcoinSettlementMethodDisplayString = "On-chain",
            shortTradeId = "t-abc12",
            tradeId = "t-abc123full",
            mediatorUserName = mediatorUserName,
            isSell = isSell,
            isSmallScreen = isSmallScreen,
            leftAmountDescription = "Pay",
            rightAmountDescription = "Receive",
            leftAmount = "342.10",
            leftCode = "USD",
            rightAmount = "0.00500000",
            rightCode = "BTC",
            isMainChainPayment = isMainChainPayment,
            peerNetworkAddress = peerNetworkAddress,
        )

    private fun baseSessionUiState(
        showDetails: Boolean = true,
        isInteractive: Boolean = true,
        isCompleted: Boolean = false,
        interruptTradeButtonText: String = "",
        openMediationButtonText: String = "",
        isInMediation: Boolean = false,
        paymentProof: String? = null,
        receiverAddress: String? = null,
        formattedTradeDuration: String = "",
    ): TradeDetailsHeaderSessionUiState =
        TradeDetailsHeaderSessionUiState(
            showDetails = showDetails,
            isInteractive = isInteractive,
            interruptTradeButtonText = interruptTradeButtonText,
            openMediationButtonText = openMediationButtonText,
            isInMediation = isInMediation,
            isCompleted = isCompleted,
            paymentProof = paymentProof,
            receiverAddress = receiverAddress,
            formattedTradeDuration = formattedTradeDuration,
        )

    private fun formattedTradeDateTime(trade: TradeDetailsHeaderTradeUiState): String = "${trade.formattedDate} ${trade.formattedTime}"

    private fun formattedSettlementLine(trade: TradeDetailsHeaderTradeUiState): String = "${trade.fiatPaymentMethodDisplayString} / ${trade.bitcoinSettlementMethodDisplayString}"

    private fun renderHeader(
        trade: TradeDetailsHeaderTradeUiState,
        session: TradeDetailsHeaderSessionUiState,
    ) {
        setTestContent {
            TradeDetailsHeaderContent(
                tradeUiState = trade,
                sessionUiState = session,
                userProfileIconProvider = userProfileIconProvider,
                onAction = mockOnAction,
            )
        }
        composeTestRule.waitForIdle()
    }

    /** Completed on-chain trade with proof, peer onion, and payout address — shared by expanded/collapsed tests. */
    private fun completedTradeWithRichDetails(showDetails: Boolean): Pair<TradeDetailsHeaderTradeUiState, TradeDetailsHeaderSessionUiState> {
        val trade =
            baseTradeUiState(
                mediatorUserName = null,
                peerNetworkAddress = SAMPLE_TOR_PEER,
            )
        val session =
            baseSessionUiState(
                showDetails = showDetails,
                isCompleted = true,
                paymentProof = LONG_MAIN_CHAIN_PAYMENT_PROOF,
                receiverAddress = SAMPLE_RECEIVER_BTC,
                formattedTradeDuration = SAMPLE_TRADE_DURATION,
            )
        return trade to session
    }

    @Test
    fun `when active trade expanded then shows header direction peer and detail labels`() {
        val trade = baseTradeUiState()
        val session =
            baseSessionUiState(
                showDetails = true,
                isCompleted = false,
                interruptTradeButtonText = INTERRUPT_TRADE_BUTTON,
                openMediationButtonText = OPEN_MEDIATION_BUTTON,
            )
        val tradeDateTime = formattedTradeDateTime(trade)
        val settlementValue = formattedSettlementLine(trade)

        renderHeader(trade, session)
        composeTestRule.onNodeWithText(trade.directionalTitle.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(DEFAULT_PEER_USER_NAME).assertIsDisplayed()

        composeTestRule.onNodeWithText("bisqEasy.openTrades.table.price".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(trade.priceDisplay).assertIsDisplayed()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDate".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(tradeDateTime).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(settlementValue).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.header.tradeId".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(trade.shortTradeId).assertIsDisplayed()

        composeTestRule.onNodeWithText("bisqEasy.mediator".i18n()).assertIsDisplayed()
        val mediatorName = checkNotNull(trade.mediatorUserName)
        composeTestRule.onNodeWithText(mediatorName).assertIsDisplayed()

        composeTestRule.onNodeWithText(INTERRUPT_TRADE_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithText(OPEN_MEDIATION_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `when active trade collapsed then detail fields and action buttons are not rendered`() {
        val trade = baseTradeUiState()
        val session =
            baseSessionUiState(
                showDetails = false,
                isCompleted = false,
                interruptTradeButtonText = INTERRUPT_TRADE_BUTTON,
                openMediationButtonText = OPEN_MEDIATION_BUTTON,
            )
        val tradeDateTime = formattedTradeDateTime(trade)
        val settlementValue = formattedSettlementLine(trade)
        val mediatorName = checkNotNull(trade.mediatorUserName)

        renderHeader(trade, session)

        composeTestRule.onNodeWithText(trade.directionalTitle.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(DEFAULT_PEER_USER_NAME).assertIsDisplayed()
        composeTestRule.onNodeWithText("Pay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Receive").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Up icon").assertIsDisplayed()

        composeTestRule.onNodeWithText("bisqEasy.openTrades.table.price".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(trade.priceDisplay).assertDoesNotExist()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDate".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(tradeDateTime).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n())
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(settlementValue).assertDoesNotExist()
        composeTestRule.onNodeWithText("bisqEasy.tradeState.header.tradeId".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(trade.shortTradeId).assertDoesNotExist()
        composeTestRule.onNodeWithText("bisqEasy.mediator".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(mediatorName).assertDoesNotExist()

        composeTestRule.onNodeWithText(INTERRUPT_TRADE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithText(OPEN_MEDIATION_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `when completed trade expanded then shows header direction peer payment proof and detail labels`() {
        val (trade, session) = completedTradeWithRichDetails(showDetails = true)
        val tradeDateTime = formattedTradeDateTime(trade)
        val settlementValue = formattedSettlementLine(trade)

        renderHeader(trade, session)
        composeTestRule.onNodeWithText(trade.directionalTitle.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(DEFAULT_PEER_USER_NAME).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.paymentProof.MAIN_CHAIN".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(LONG_MAIN_CHAIN_PAYMENT_PROOF.truncateBitcoinIdentifier()).assertIsDisplayed()
        composeTestRule.onNodeWithText(blockExplorerLinkText()).assertIsDisplayed()

        composeTestRule.onNodeWithText("bisqEasy.openTrades.table.price".i18n()).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(trade.priceDisplay).performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDate".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(tradeDateTime).performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDuration".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(SAMPLE_TRADE_DURATION).performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(settlementValue).performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeId".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(trade.shortTradeId).performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.peerNetworkAddress".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(SAMPLE_TOR_PEER.truncateBitcoinIdentifier()).performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.btcPaymentAddress".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(SAMPLE_RECEIVER_BTC.truncateBitcoinIdentifier()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `when completed trade collapsed then detail fields are not rendered`() {
        val (trade, session) = completedTradeWithRichDetails(showDetails = false)
        val tradeDateTime = formattedTradeDateTime(trade)
        val settlementValue = formattedSettlementLine(trade)

        renderHeader(trade, session)

        composeTestRule.onNodeWithText(trade.directionalTitle.uppercase()).assertIsDisplayed()
        composeTestRule.onNodeWithText(DEFAULT_PEER_USER_NAME).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.paymentProof.MAIN_CHAIN".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(LONG_MAIN_CHAIN_PAYMENT_PROOF.truncateBitcoinIdentifier()).assertIsDisplayed()
        composeTestRule.onNodeWithText(blockExplorerLinkText()).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Up icon").assertIsDisplayed()

        composeTestRule.onNodeWithText("Pay").assertDoesNotExist()
        composeTestRule.onNodeWithText("Receive").assertDoesNotExist()

        composeTestRule.onNodeWithText("bisqEasy.openTrades.table.price".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(trade.priceDisplay).assertDoesNotExist()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDate".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(tradeDateTime).assertDoesNotExist()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDuration".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(SAMPLE_TRADE_DURATION).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("bisqEasy.offerbook.offerList.table.columns.settlementMethod".i18n())
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(settlementValue).assertDoesNotExist()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeId".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText(trade.shortTradeId).assertDoesNotExist()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.peerNetworkAddress".i18n())
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.btcPaymentAddress".i18n())
            .assertDoesNotExist()
    }

    @Test
    fun `when toggle clicked then triggers ToggleHeader action`() {
        val trade = baseTradeUiState()
        val session = baseSessionUiState()

        renderHeader(trade, session)
        composeTestRule.onNodeWithContentDescription("Up icon").performClick()

        verify { mockOnAction(TradeDetailsHeaderUiAction.ToggleHeader) }
    }

    @Test
    fun `when header not interactive then toggle is disabled`() {
        val trade = baseTradeUiState()
        val session = baseSessionUiState(isInteractive = false)

        renderHeader(trade, session)
        composeTestRule.onNodeWithContentDescription("Up icon").assertIsNotEnabled()
    }

    @Test
    fun `when interrupt trade button shown and clicked then triggers OpenInterruptionConfirmationDialog`() {
        val trade = baseTradeUiState()
        val session =
            baseSessionUiState(
                interruptTradeButtonText = INTERRUPT_TRADE_BUTTON,
            )

        renderHeader(trade, session)
        composeTestRule.onNodeWithText(INTERRUPT_TRADE_BUTTON).performClick()

        verify { mockOnAction(TradeDetailsHeaderUiAction.OpenInterruptionConfirmationDialog) }
    }

    @Test
    fun `when open mediation button shown and clicked then triggers OpenMediationConfirmationDialog`() {
        val trade = baseTradeUiState()
        val session =
            baseSessionUiState(
                openMediationButtonText = OPEN_MEDIATION_BUTTON,
            )

        renderHeader(trade, session)
        composeTestRule.onNodeWithText(OPEN_MEDIATION_BUTTON).performClick()

        verify { mockOnAction(TradeDetailsHeaderUiAction.OpenMediationConfirmationDialog) }
    }

    @Test
    fun `when in mediation then open mediation button is not shown`() {
        val trade = baseTradeUiState()
        val session =
            baseSessionUiState(
                openMediationButtonText = OPEN_MEDIATION_BUTTON,
                isInMediation = true,
            )

        renderHeader(trade, session)
        composeTestRule.onNodeWithText(OPEN_MEDIATION_BUTTON).assertDoesNotExist()
    }

    @Test
    fun `when completed main chain with payment proof then shows explorer link`() {
        val trade = baseTradeUiState(isMainChainPayment = true, mediatorUserName = null)
        val proof = "a1b2c3d4e5f6a1b2c3d4e5f6abcd"
        val session =
            baseSessionUiState(
                isCompleted = true,
                paymentProof = proof,
                formattedTradeDuration = SAMPLE_TRADE_DURATION,
            )

        renderHeader(trade, session)
        composeTestRule.onNodeWithText("bisqEasy.tradeState.paymentProof.MAIN_CHAIN".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(blockExplorerLinkText()).assertIsDisplayed()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDuration".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(SAMPLE_TRADE_DURATION).assertIsDisplayed()
    }

    @Test
    fun `when completed lightning payment proof then shows ln label without block explorer link`() {
        val trade = baseTradeUiState(isMainChainPayment = false, mediatorUserName = null)
        val session =
            baseSessionUiState(
                isCompleted = true,
                paymentProof = "ln_proof_value_here",
            )

        renderHeader(trade, session)
        composeTestRule.onNodeWithText("bisqEasy.tradeState.paymentProof.LN".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(blockExplorerLinkText()).assertDoesNotExist()
    }

    @Test
    fun `when completed with peer address then shows truncated peer address row`() {
        val addr = "peer_tor_address.onion:9999"
        val trade =
            baseTradeUiState(
                isMainChainPayment = true,
                mediatorUserName = null,
                peerNetworkAddress = addr,
            )
        val session = baseSessionUiState(isCompleted = true, formattedTradeDuration = "1m")

        renderHeader(trade, session)
        composeTestRule
            .onNodeWithText("bisqEasy.openTrades.tradeDetails.peerNetworkAddress".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(addr.truncateBitcoinIdentifier())
            .assertIsDisplayed()
    }

    @Test
    fun `when sell trade then shows send and receive amounts for small screen layout`() {
        val trade =
            baseTradeUiState(
                directionalTitle = "Selling to:",
                userName = "BuyerPeer",
                isSell = true,
                isSmallScreen = true,
            ).copy(
                leftAmountDescription = "Send",
                rightAmountDescription = "Receive",
                leftAmount = "0.01",
                leftCode = "BTC",
                rightAmount = "900.00",
                rightCode = "EUR",
            )
        val session = baseSessionUiState(showDetails = false, isCompleted = false)

        renderHeader(trade, session)
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
        composeTestRule.onNodeWithText("Receive").assertIsDisplayed()
        composeTestRule.onNodeWithText("BTC").assertIsDisplayed()
        composeTestRule.onNodeWithText("900.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR").assertIsDisplayed()
    }

    @Test
    fun `when active trade expanded small screen then shows price and trade date in stacked info boxes`() {
        val trade = baseTradeUiState(isSmallScreen = true)
        val session =
            baseSessionUiState(
                showDetails = true,
                isCompleted = false,
                interruptTradeButtonText = INTERRUPT_TRADE_BUTTON,
                openMediationButtonText = OPEN_MEDIATION_BUTTON,
            )
        val tradeDateTime = formattedTradeDateTime(trade)

        renderHeader(trade, session)
        composeTestRule.onNodeWithText("bisqEasy.openTrades.table.price".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(trade.priceDisplay).assertIsDisplayed()
        composeTestRule.onNodeWithText("bisqEasy.openTrades.tradeDetails.tradeDate".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText(tradeDateTime).assertIsDisplayed()
    }
}
