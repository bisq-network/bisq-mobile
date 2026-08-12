package network.bisq.mobile.client.payment_accounts.common.ui.account_detail.cash_deposit

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import io.mockk.coEvery
import io.mockk.mockk
import network.bisq.mobile.client.common.test_utils.ClientInjectComposeUiTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountCountryDetails
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit.CashDepositAccountDetailContent
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit.CashDepositAccountDetailPresenter
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

class CashDepositAccountDetailContentUiTest : ClientInjectComposeUiTestBase() {
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade

    override fun onBeforeKoinStart() {
        paymentAccountsServiceFacade = mockk(relaxed = true)
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<PaymentAccountsServiceFacade> { paymentAccountsServiceFacade }
                factory {
                    CashDepositAccountDetailPresenter(
                        paymentAccountsServiceFacade,
                        mockk<MainPresenter>(relaxed = true),
                    )
                }
            },
        )

    private fun setAccountContent(account: CashDepositAccount = sampleAccount()) {
        setInjectTestContent {
            CashDepositAccountDetailContent(account = account)
        }
    }

    @Test
    fun `when country details load then renders metadata driven labels`() {
        coEvery { paymentAccountsServiceFacade.getBankAccountCountryDetails("US") } returns Result.success(sampleCountryDetails())

        setAccountContent()

        composeTestRule.onNodeWithText("Cash Deposit").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("United States").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.currency".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("USD (US Dollar)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Account owner ID").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Routing number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Branch number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Account number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("National account number").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("paymentAccounts.cashDeposit.requirements".i18n()).assertCountEquals(1)
        composeTestRule.onAllNodesWithText("Bring cash deposit receipt.").assertCountEquals(1)
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
                holderId = null,
                bankId = null,
                branchId = null,
                bankAccountType = null,
                nationalAccountId = null,
                requirements = null,
                chargebackRisk = null,
            ),
        )

        composeTestRule.onAllNodesWithText("Account owner ID").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Routing number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Branch number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("National account number").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.bank.bankAccountType".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.cashDeposit.requirements".i18n()).assertCountEquals(0)
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
        holderId: String? = "1234",
        bankId: String? = "BANKUS33",
        branchId: String? = "001",
        bankAccountType: BankAccountType? = BankAccountType.CHECKING,
        nationalAccountId: String? = "NAT-123",
        requirements: String? = "Bring cash deposit receipt.",
        chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    ): CashDepositAccount =
        CashDepositAccount(
            accountName = "Cash Deposit Main",
            accountPayload =
                CashDepositAccountPayload(
                    chargebackRisk = chargebackRisk,
                    paymentMethodName = "Cash Deposit",
                    currency = FiatCurrency("USD", "US Dollar"),
                    country = Country("US", "United States"),
                    holderName = "Alice Doe",
                    holderId = holderId,
                    bankName = "Bisq Bank",
                    bankId = bankId,
                    branchId = branchId,
                    accountNr = "123456789",
                    bankAccountType = bankAccountType,
                    nationalAccountId = nationalAccountId,
                    requirements = requirements,
                ),
            creationDate = "Apr 3, 2026",
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "4 days",
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
