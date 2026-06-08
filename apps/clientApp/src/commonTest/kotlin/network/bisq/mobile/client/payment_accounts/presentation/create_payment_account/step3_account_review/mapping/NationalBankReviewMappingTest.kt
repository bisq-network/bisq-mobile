package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccountPayload
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NationalBankReviewMappingTest {
    @Test
    fun `toReviewPaymentAccount maps create account and payment method fields correctly`() {
        val createAccount = sampleCreateAccount()
        val paymentMethod = samplePaymentMethod()

        val account = createAccount.toReviewPaymentAccount(paymentMethod)

        assertEquals("National Bank Main", account.accountName)
        assertEquals(FiatPaymentMethodChargebackRisk.MODERATE, account.accountPayload.chargebackRisk)
        assertEquals("National Bank", account.accountPayload.paymentMethodName)
        assertEquals(FiatCurrency(code = "USD", name = "US Dollar"), account.accountPayload.currency)
        assertEquals(Country(code = "US", name = "United States"), account.accountPayload.country)
        assertEquals("Alice Doe", account.accountPayload.holderName)
        assertEquals("ID-123", account.accountPayload.holderId)
        assertEquals("Bisq Bank", account.accountPayload.bankName)
        assertEquals("BANKUS33", account.accountPayload.bankId)
        assertEquals("001", account.accountPayload.branchId)
        assertEquals("123456789", account.accountPayload.accountNr)
        assertEquals(BankAccountType.CHECKING, account.accountPayload.bankAccountType)
        assertEquals("NAT-123", account.accountPayload.nationalAccountId)
        assertNull(account.creationDate)
        assertEquals("5000.00 USD", account.tradeLimitInfo)
        assertEquals("5 days", account.tradeDuration)
    }

    @Test
    fun `toReviewPaymentAccount falls back to selected codes when country and currency are unsupported`() {
        val createAccount =
            sampleCreateAccount(
                selectedCountryCode = "XX",
                selectedCurrencyCode = "YYY",
            )
        val paymentMethod = samplePaymentMethod()

        val account = createAccount.toReviewPaymentAccount(paymentMethod)

        assertEquals(Country(code = "XX", name = "XX"), account.accountPayload.country)
        assertEquals(FiatCurrency(code = "YYY", name = "YYY"), account.accountPayload.currency)
    }

    @Test
    fun `toReviewPaymentAccount preserves null optional payload fields`() {
        val createAccount =
            sampleCreateAccount(
                holderName = null,
                holderId = null,
                bankName = null,
                bankId = null,
                branchId = null,
                bankAccountType = null,
                nationalAccountId = null,
            )
        val paymentMethod = samplePaymentMethod()

        val account = createAccount.toReviewPaymentAccount(paymentMethod)

        assertNull(account.accountPayload.holderName)
        assertNull(account.accountPayload.holderId)
        assertNull(account.accountPayload.bankName)
        assertNull(account.accountPayload.bankId)
        assertNull(account.accountPayload.branchId)
        assertNull(account.accountPayload.bankAccountType)
        assertNull(account.accountPayload.nationalAccountId)
    }

    private fun sampleCreateAccount(
        selectedCountryCode: String = "US",
        selectedCurrencyCode: String = "USD",
        holderName: String? = "Alice Doe",
        holderId: String? = "ID-123",
        bankName: String? = "Bisq Bank",
        bankId: String? = "BANKUS33",
        branchId: String? = "001",
        bankAccountType: BankAccountType? = BankAccountType.CHECKING,
        nationalAccountId: String? = "NAT-123",
    ): CreateNationalBankAccount =
        CreateNationalBankAccount(
            accountName = "National Bank Main",
            accountPayload =
                CreateNationalBankAccountPayload(
                    selectedCountryCode = selectedCountryCode,
                    selectedCurrencyCode = selectedCurrencyCode,
                    holderName = holderName,
                    holderId = holderId,
                    bankName = bankName,
                    bankId = bankId,
                    branchId = branchId,
                    accountNr = "123456789",
                    bankAccountType = bankAccountType,
                    nationalAccountId = nationalAccountId,
                ),
        )

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.NATIONAL_BANK,
            name = "National Bank",
            supportedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar")),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "5 days",
        )
}
