package network.bisq.mobile.client.payment_accounts.common.ui.account_detail.same_bank

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.mockk.coEvery
import io.mockk.mockk
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.SameBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.SameBankAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.bank.BankAccountDetailPresenter
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.same_bank.SameBankAccountDetailContent
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.After
import org.junit.Test
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class SameBankAccountDetailContentUiTest : BisqComposeUiTestBase() {
    private lateinit var koinApplication: KoinApplication
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner

    override fun setUpUiTest() {
        super.setUpUiTest()
        paymentAccountsServiceFacade = mockk(relaxed = true)
        viewModelStore = ViewModelStore()
        viewModelStoreOwner =
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = this@SameBankAccountDetailContentUiTest.viewModelStore
            }
        runCatching { stopKoin() }
        koinApplication =
            startKoin {
                modules(
                    clientTestModule,
                    module {
                        single<PaymentAccountsServiceFacade> { paymentAccountsServiceFacade }
                        factory {
                            BankAccountDetailPresenter(
                                paymentAccountsServiceFacade,
                                mockk<MainPresenter>(relaxed = true),
                            )
                        }
                    },
                )
            }
    }

    @After
    fun tearDown() {
        runCatching {
            composeTestRule.setContent {}
            composeTestRule.waitForIdle()
        }
        runCatching { viewModelStore.clear() }
        runCatching { stopKoin() }
    }
    private fun setAccountContent(account: SameBankAccount = sampleAccount()) {
        setTestContent {
            KoinIsolatedContext(koinApplication) {
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                    SameBankAccountDetailContent(account = account)
                }
            }
        }
    }

    @Test
    fun `when country details load then renders metadata driven labels`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setAccountContent()

        composeTestRule.onNodeWithText("Same Bank").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("United States").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.currency".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("USD (US Dollar)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account owner ID").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Routing number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Branch number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Account number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("National account number").assertCountEquals(1)
    }

    @Test
    fun `when country details fail then renders error state`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.failure(RuntimeException("boom"))

        setAccountContent()

        composeTestRule.onNodeWithText("mobile.action.retry".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Routing number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.bank.bankId".i18n()).assertCountEquals(0)
    }

    @Test
    fun `when optional fields are absent then optional rows are hidden`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setAccountContent(
            sampleAccount(
                holderName = null,
                holderId = null,
                bankName = null,
                bankId = null,
                branchId = null,
                bankAccountType = null,
                nationalAccountId = null,
                chargebackRisk = null,
            ),
        )

        composeTestRule.onAllNodesWithText("paymentAccounts.holderName".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Account owner ID").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.bank.bankName".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Routing number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Branch number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("National account number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.bank.bankAccountType".i18n()).assertCountEquals(0)
    }

    @Test
    fun `when chargeback risk is present then badge is displayed`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setAccountContent(sampleAccount(chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE))

        composeTestRule
            .onAllNodesWithText(
                "paymentAccounts.createAccount.paymentMethod.risk.moderate".i18n(),
                substring = true,
            ).assertCountEquals(1)
    }

    private fun sampleAccount(
        holderName: String? = "Alice Doe",
        holderId: String? = "1234",
        bankName: String? = "Bisq Bank",
        bankId: String? = "BANKUS33",
        branchId: String? = "001",
        bankAccountType: BankAccountType? = BankAccountType.CHECKING,
        nationalAccountId: String? = "NAT-123",
        chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    ): SameBankAccount =
        SameBankAccount(
            accountName = "Same Bank Main",
            accountPayload =
                SameBankAccountPayload(
                    chargebackRisk = chargebackRisk,
                    paymentMethodName = "Same Bank",
                    currency = FiatCurrency("USD", "US Dollar"),
                    country = Country("US", "United States"),
                    holderName = holderName,
                    holderId = holderId,
                    bankName = bankName,
                    bankId = bankId,
                    branchId = branchId,
                    accountNr = "123456789",
                    bankAccountType = bankAccountType,
                    nationalAccountId = nationalAccountId,
                ),
            creationDate = "Apr 3, 2026",
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "5 days",
        )

    private fun sampleCountryDetails(): BankAccountCountryDetails =
        BankAccountCountryDetails(
            country = Country("US", "United States"),
            bankAccountValidationSupported = true,
            holderIdRequired = true,
            holderIdDescription = "Account owner ID",
            holderIdDescriptionShort = "Owner ID",
            bankAccountTypeRequired = true,
            bankNameRequired = true,
            bankIdRequired = true,
            bankIdDescription = "Routing number",
            bankIdDescriptionShort = "Routing",
            branchIdRequired = true,
            branchIdDescription = "Branch number",
            branchIdDescriptionShort = "Branch",
            accountNrDescription = "Account number",
            nationalAccountIdRequired = true,
            nationalAccountIdDescription = "National account number",
            nationalAccountIdDescriptionShort = "National ID",
        )
}
