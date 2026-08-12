package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.client.common.test_utils.ClientInjectComposeUiTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.PaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit.CashDepositAccountDetailPresenter
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.assertTrue

class PaymentAccountReviewScreenUiTest : ClientInjectComposeUiTestBase() {
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var mainPresenter: MainPresenter

    override fun onBeforeKoinStart() {
        paymentAccountsServiceFacade = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        every { globalUiManager.scheduleShowLoading() } returns Unit
        every { globalUiManager.scheduleHideLoading() } returns Unit
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<GlobalUiManager> { globalUiManager }
                single<PaymentAccountsServiceFacade> { paymentAccountsServiceFacade }
                factory { PaymentAccountReviewPresenter(paymentAccountsServiceFacade, mainPresenter) }
                factory { CashDepositAccountDetailPresenter(paymentAccountsServiceFacade, mainPresenter) }
            },
        )

    private fun setScreenContent(
        createPaymentAccount: CreatePaymentAccount,
        paymentMethod: PaymentMethod,
        onCloseCreateAccountFlow: () -> Unit = {},
    ) {
        setInjectTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                PaymentAccountReviewScreen(
                    createPaymentAccount = createPaymentAccount,
                    paymentMethod = paymentMethod,
                    onCloseCreateAccountFlow = onCloseCreateAccountFlow,
                )
            }
        }
    }

    @Test
    fun `when zelle account rendered then shared and zelle specific fields are shown`() {
        setScreenContent(
            createPaymentAccount = sampleCreateZelleAccount(),
            paymentMethod = sampleZellePaymentMethod(),
        )

        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.review".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Zelle").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.summary.accountNameOverlay.accountName.description".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.emailOrMobileNr".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when revolut account rendered then revolut specific fields are shown`() {
        setScreenContent(
            createPaymentAccount = sampleCreateRevolutAccount(),
            paymentMethod = sampleRevolutPaymentMethod(),
        )

        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.review".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("Revolut").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.userName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("satoshi").assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.title".i18n())
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when monero account rendered then monero specific fields are shown`() {
        setScreenContent(
            createPaymentAccount = sampleCreateMoneroAccount(),
            paymentMethod = sampleCryptoPaymentMethod(code = "XMR", name = "Monero"),
        )

        composeTestRule.onNodeWithText("XMR").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monero").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when other crypto account rendered then other crypto specific fields are shown`() {
        setScreenContent(
            createPaymentAccount = sampleCreateOtherCryptoAssetAccount(),
            paymentMethod = sampleCryptoPaymentMethod(code = "ETH", name = "Ethereum"),
        )

        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("ETH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ethereum").assertIsDisplayed()
    }

    @Test
    fun `when create account does not map to review account then unsupported state is shown`() {
        setScreenContent(
            createPaymentAccount = sampleCreateZelleAccount(),
            paymentMethod = sampleCryptoPaymentMethod(code = "ETH", name = "Ethereum"),
        )

        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("paymentAccounts.country".i18n()).assertCountEquals(0)
    }

    @Test
    fun `when create account button clicked then account is added and close callback is invoked`() {
        val account = sampleCreateZelleAccount()
        var closeCallbackInvoked = false
        coEvery { paymentAccountsServiceFacade.addAccount(account) } returns Result.success(Unit)
        setScreenContent(
            createPaymentAccount = account,
            paymentMethod = sampleZellePaymentMethod(),
            onCloseCreateAccountFlow = { closeCallbackInvoked = true },
        )

        composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).performClick()

        composeTestRule.waitForIdle()
        coVerify(exactly = 1) { paymentAccountsServiceFacade.addAccount(account) }
        verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
        verify(exactly = 1) { globalUiManager.scheduleHideLoading() }
        assertTrue(closeCallbackInvoked)
    }

    private fun sampleCreateZelleAccount(accountName: String = "Zelle Personal"): CreateZelleAccount =
        CreateZelleAccount(
            accountName = accountName,
            accountPayload =
                CreateZelleAccountPayload(
                    holderName = "Alice",
                    emailOrMobileNr = "alice@example.com",
                ),
        )

    private fun sampleCreateRevolutAccount(accountName: String = "Revolut Personal"): CreateRevolutAccount =
        CreateRevolutAccount(
            accountName = accountName,
            accountPayload =
                CreateRevolutAccountPayload(
                    userName = "satoshi",
                    selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")),
                ),
        )

    private fun sampleCreateMoneroAccount(): CreateMoneroAccount =
        CreateMoneroAccount(
            accountName = "Monero Main",
            accountPayload =
                CreateMoneroAccountPayload(
                    address = "48A_MAIN_ADDRESS",
                    isInstant = false,
                    useSubAddresses = false,
                ),
        )

    private fun sampleCreateOtherCryptoAssetAccount(): CreateOtherCryptoAssetAccount =
        CreateOtherCryptoAssetAccount(
            accountName = "ETH Account",
            accountPayload =
                CreateOtherCryptoAssetAccountPayload(
                    currencyCode = "ETH",
                    address = "0xABC123",
                    isInstant = false,
                ),
        )

    private fun sampleZellePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.ZELLE,
            name = "Zelle",
            supportedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar")),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "1 day",
        )

    private fun sampleRevolutPaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.REVOLUT,
            name = "Revolut",
            supportedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")),
            supportedCountries = listOf(Country(code = "GB", name = "United Kingdom")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "1 day",
        )

    private fun sampleCryptoPaymentMethod(
        code: String,
        name: String,
    ): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = code,
            name = name,
            supportAutoConf = false,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )
}
