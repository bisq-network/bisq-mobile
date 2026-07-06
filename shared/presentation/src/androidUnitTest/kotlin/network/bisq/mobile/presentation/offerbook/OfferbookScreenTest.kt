package network.bisq.mobile.presentation.offerbook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.model.market.MarketPriceItem
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.model.alert.AlertType
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.NoopNavigationManager
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiAction
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationUiState
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialogPresenter
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals

/**
 * Compose UI tests for the stateless [OfferbookContent] (the body of [OfferbookScreen]).
 *
 * Covers the states the content is responsible for: the delete-confirmation and trade-restriction
 * dialogs, the not-enough-reputation dialog (both the seller-as-taker and web-link variants), the
 * filter controller, and the offer list/card selection.
 *
 * The content is driven through [RenderOfferbookContent], which defaults every value/callback so a
 * test overrides only what its behavior claim depends on. `OfferbookContent`'s `TopBar` injects a
 * [NavigationManager] and an [ITopBarPresenter] via `koinInject()`, so a minimal Koin floor is
 * started in [setUp] even though the content itself is stateless.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OfferbookScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        runCatching { stopKoin() }
        startOfferbookContentTestKoin()
        I18nSupport.setLanguage()
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Delete-confirmation dialog
    // -------------------------------------------------------------------------

    @Test
    fun `when delete guard disabled then confirm button shows loading and is not enabled`() {
        var confirmCount = 0
        composeTestRule.setContent {
            RenderOfferbookContent(
                showDeleteConfirmation = true,
                isDeleteOfferEnabled = false,
                onConfirmDeleteOffer = { confirmCount++ },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("dialog_confirm_yes")
            .assertIsNotEnabled()

        assertEquals(0, confirmCount)
    }

    @Test
    fun `when confirm clicked then onConfirmDeleteOffer dispatched`() {
        var confirmCount = 0
        composeTestRule.setContent {
            RenderOfferbookContent(
                showDeleteConfirmation = true,
                isDeleteOfferEnabled = true,
                onConfirmDeleteOffer = { confirmCount++ },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription("dialog_confirm_yes")
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, confirmCount)
    }

    // -------------------------------------------------------------------------
    // Trade-restriction dialog
    // -------------------------------------------------------------------------

    @Test
    fun `when showTradeRestrictedDialog is null then no dialog headline shown`() {
        composeTestRule.setContent {
            RenderOfferbookContent(showTradeRestrictedDialog = null)
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText("Trading restricted — update required")
            .assertCountEquals(0)
    }

    @Test
    fun `when showTradeRestrictedDialog is non-null then dialog is displayed`() {
        composeTestRule.setContent {
            RenderOfferbookContent(showTradeRestrictedDialog = sampleAlert())
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Trading restricted — update required").assertIsDisplayed()
    }

    @Test
    fun `when close button clicked then OnCloseDialog dispatched`() {
        var captured: AlertNotificationUiAction? = null
        composeTestRule.setContent {
            RenderOfferbookContent(
                showTradeRestrictedDialog = sampleAlert("Alert to dismiss"),
                onTradeRestrictingAlertAction = { captured = it },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alert to dismiss").assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("mobile.alert.actions.dismiss.description".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(AlertNotificationUiAction.OnCloseDialog, captured)
    }

    // -------------------------------------------------------------------------
    // Not-enough-reputation dialog
    // -------------------------------------------------------------------------

    @Test
    fun `when reputation dialog shown for seller as taker then confirmation dialog is displayed`() {
        var dismissCount = 0
        composeTestRule.setContent {
            RenderOfferbookContent(
                showNotEnoughReputationDialog = true,
                isReputationWarningForSellerAsTaker = true,
                notEnoughReputationHeadline = "Not enough reputation",
                notEnoughReputationMessage = "You need more reputation to sell.",
                onDismissNotEnoughReputationDialog = { dismissCount++ },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Not enough reputation").assertIsDisplayed()
        composeTestRule.onNodeWithText("You need more reputation to sell.").assertIsDisplayed()

        composeTestRule.onNodeWithText("action.cancel".i18n()).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, dismissCount)
    }

    @Test
    fun `when reputation dialog shown for non seller as taker then web link dialog is displayed`() {
        composeTestRule.setContent {
            RenderOfferbookContent(
                showNotEnoughReputationDialog = true,
                isReputationWarningForSellerAsTaker = false,
                notEnoughReputationHeadline = "Seller lacks reputation",
                notEnoughReputationMessage = "The seller does not have enough reputation.",
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Seller lacks reputation").assertIsDisplayed()
        composeTestRule.onNodeWithText("The seller does not have enough reputation.").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // Filter controller + offer list
    // -------------------------------------------------------------------------

    @Test
    fun `when filters are active then filter controller is shown`() {
        composeTestRule.setContent {
            RenderOfferbookContent(
                filterUiState =
                    OfferbookFilterUiState(
                        payment = listOf(MethodIconState(id = "SEPA", label = "SEPA", iconPath = "", selected = false)),
                        settlement = emptyList(),
                        onlyMyOffers = false,
                        hasActiveFilters = true,
                    ),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("offerbook_filters_header").assertIsDisplayed()
    }

    @Test
    fun `when an offer is present and tapped then onOfferSelect is dispatched`() {
        val offer = sampleOffer()
        var selected: OfferItemPresentationModel? = null
        composeTestRule.setContent {
            RenderOfferbookContent(
                sortedFilteredOffers = listOf(offer),
                userProfileIconProvider = { createEmptyImage() },
                onOfferSelect = { selected = it },
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("500 EUR").performClick()
        composeTestRule.waitForIdle()

        assertEquals(offer.offerId, selected?.offerId)
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun sampleAlert(headline: String = "Trading restricted — update required") =
        AlertNotificationUiState(
            id = "screen-test-alert",
            type = AlertType.EMERGENCY,
            headline = headline,
            message = "Update the app to continue trading.",
            haltTrading = true,
        )

    private fun sampleOffer(): OfferItemPresentationModel {
        val market = MarketVO("BTC", "EUR", "Bitcoin", "Euro")
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "id"),
            )
        val offer =
            BisqEasyOfferVO(
                id = "offer-123",
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.SELL,
                market = market,
                amountSpec = QuoteSideFixedAmountSpecVO(500_00),
                priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(50_000L, market)),
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = listOf("en"),
            )
        val dto =
            OfferItemPresentationDto(
                bisqEasyOffer = offer,
                isMyOffer = false,
                userProfile = createMockUserProfile("Satoshi"),
                formattedDate = "2024-01-15",
                formattedQuoteAmount = "500 EUR",
                formattedBaseAmount = "0.01 BTC",
                formattedPrice = "50,000",
                formattedPriceSpec = "Fix",
                quoteSidePaymentMethods = listOf("SEPA"),
                baseSidePaymentMethods = listOf("MAIN_CHAIN"),
                reputationScore = ReputationScoreVO(totalScore = 1000L, fiveSystemScore = 5.0, ranking = 42),
            )
        return OfferItemPresentationModel(dto)
    }

    /**
     * Minimal Koin floor for rendering [OfferbookContent]. Its `TopBar` injects a [NavigationManager]
     * and an [ITopBarPresenter] via `koinInject()`, so even the stateless content needs these bound.
     */
    private fun startOfferbookContentTestKoin() {
        startKoin {
            modules(
                module {
                    single<NavigationManager> { NoopNavigationManager() }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single { GlobalUiManager(testDispatcher) }
                    single<MainPresenter> { mockk(relaxed = true) }
                    single<ITopBarPresenter> {
                        mockk<ITopBarPresenter>(relaxed = true).also { m ->
                            every { m.userProfile } returns MutableStateFlow(null)
                            every { m.showAnimation } returns MutableStateFlow(false)
                            every { m.connectivityStatus } returns
                                MutableStateFlow(ConnectivityService.ConnectivityStatus.CONNECTED_AND_DATA_RECEIVED)
                        }
                    }
                    // WebLinkConfirmationDialog (not-enough-reputation, non-seller variant) resolves this
                    // BasePresenter via koinInject(); showWebLinkConfirmation must be true for the
                    // dialog to actually render rather than silently open the link.
                    single<SettingsServiceFacade> {
                        mockk<SettingsServiceFacade>(relaxed = true).also {
                            every { it.showWebLinkConfirmation } returns MutableStateFlow(true)
                        }
                    }
                    factory { WebLinkConfirmationDialogPresenter(get(), get()) }
                },
            )
        }
    }

    private fun emptyOfferbookFilterUiState() =
        OfferbookFilterUiState(
            payment = emptyList(),
            settlement = emptyList(),
            onlyMyOffers = false,
            hasActiveFilters = false,
        )

    /**
     * Renders the stateless [OfferbookContent] under [BisqTheme] with [LocalIsTest] enabled and every
     * value/callback defaulted, so a test overrides only what its behavior claim depends on. The
     * default [userProfileIconProvider] throws — pass real offers only together with a provider such
     * as `{ createEmptyImage() }`.
     */
    @Composable
    private fun RenderOfferbookContent(
        sortedFilteredOffers: List<OfferItemPresentationModel> = emptyList(),
        selectedDirection: DirectionEnum = DirectionEnum.BUY,
        selectedMarket: MarketPriceItem? = null,
        filterUiState: OfferbookFilterUiState = emptyOfferbookFilterUiState(),
        showLoading: Boolean = false,
        showDeleteConfirmation: Boolean = false,
        showNotEnoughReputationDialog: Boolean = false,
        showTradeRestrictedDialog: AlertNotificationUiState? = null,
        isCreateOfferEnabled: Boolean = true,
        isDeleteOfferEnabled: Boolean = true,
        isTakeOfferEnabled: Boolean = true,
        isDemo: Boolean = false,
        userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { error("icon provider not used without offers") },
        isReputationWarningForSellerAsTaker: Boolean = false,
        notEnoughReputationHeadline: String = "",
        notEnoughReputationMessage: String = "",
        onSelectDirection: (DirectionEnum) -> Unit = {},
        onCreateOffer: () -> Unit = {},
        onOfferSelect: (OfferItemPresentationModel) -> Unit = {},
        onTogglePayment: (String) -> Unit = {},
        onToggleSettlement: (String) -> Unit = {},
        onOnlyMyOffersChange: (Boolean) -> Unit = {},
        onClearAllFilters: () -> Unit = {},
        onSetPaymentSelection: (Set<String>) -> Unit = {},
        onSetSettlementSelection: (Set<String>) -> Unit = {},
        onConfirmDeleteOffer: () -> Unit = {},
        onDismissDeleteOffer: () -> Unit = {},
        onNavigateToReputation: () -> Unit = {},
        onOpenReputationWiki: () -> Unit = {},
        onDismissNotEnoughReputationDialog: () -> Unit = {},
        onTradeRestrictingAlertAction: (AlertNotificationUiAction) -> Unit = {},
    ) {
        CompositionLocalProvider(LocalIsTest provides true) {
            BisqTheme {
                OfferbookContent(
                    sortedFilteredOffers = sortedFilteredOffers,
                    selectedDirection = selectedDirection,
                    selectedMarket = selectedMarket,
                    filterUiState = filterUiState,
                    showLoading = showLoading,
                    showDeleteConfirmation = showDeleteConfirmation,
                    showNotEnoughReputationDialog = showNotEnoughReputationDialog,
                    showTradeRestrictedDialog = showTradeRestrictedDialog,
                    isCreateOfferEnabled = isCreateOfferEnabled,
                    isDeleteOfferEnabled = isDeleteOfferEnabled,
                    isTakeOfferEnabled = isTakeOfferEnabled,
                    isDemo = isDemo,
                    userProfileIconProvider = userProfileIconProvider,
                    isReputationWarningForSellerAsTaker = isReputationWarningForSellerAsTaker,
                    notEnoughReputationHeadline = notEnoughReputationHeadline,
                    notEnoughReputationMessage = notEnoughReputationMessage,
                    onSelectDirection = onSelectDirection,
                    onCreateOffer = onCreateOffer,
                    onOfferSelect = onOfferSelect,
                    onTogglePayment = onTogglePayment,
                    onToggleSettlement = onToggleSettlement,
                    onOnlyMyOffersChange = onOnlyMyOffersChange,
                    onClearAllFilters = onClearAllFilters,
                    onSetPaymentSelection = onSetPaymentSelection,
                    onSetSettlementSelection = onSetSettlementSelection,
                    onConfirmDeleteOffer = onConfirmDeleteOffer,
                    onDismissDeleteOffer = onDismissDeleteOffer,
                    onNavigateToReputation = onNavigateToReputation,
                    onOpenReputationWiki = onOpenReputationWiki,
                    onDismissNotEnoughReputationDialog = onDismissNotEnoughReputationDialog,
                    onTradeRestrictingAlertAction = onTradeRestrictingAlertAction,
                )
            }
        }
    }
}
