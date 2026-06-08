package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatPaymentRailDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank.CreateNationalBankAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank.NationalBankAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.national_bank.NationalBankAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.payment_method.FiatPaymentMethodChargebackRiskDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NationalBankAccountMappingTest {
    @Test
    fun `toDomain maps all NationalBankAccountDto fields correctly`() {
        val dto =
            NationalBankAccountDto(
                accountName = "National Bank Main",
                accountPayload =
                    NationalBankAccountPayloadDto(
                        chargebackRisk = FiatPaymentMethodChargebackRiskDto.MODERATE,
                        paymentMethodName = "National Bank",
                        currency = FiatCurrencyDto(code = "USD", name = "US Dollar"),
                        country = CountryDto(code = "US", name = "United States"),
                        holderName = "Alice Doe",
                        holderId = "ID-123",
                        bankName = "Bisq Bank",
                        bankId = "BANKUS33",
                        branchId = "001",
                        accountNr = "123456789",
                        bankAccountType = BankAccountTypeDto.CHECKING,
                        nationalAccountId = "NAT-123",
                    ),
                creationDate = "2026-05-10",
                tradeLimitInfo = "5000.00 USD",
                tradeDuration = "5 days",
            )

        val domain = dto.toDomain()

        assertEquals("National Bank Main", domain.accountName)
        assertEquals(FiatPaymentMethodChargebackRisk.MODERATE, domain.accountPayload.chargebackRisk)
        assertEquals("National Bank", domain.accountPayload.paymentMethodName)
        assertEquals(FiatCurrency(code = "USD", name = "US Dollar"), domain.accountPayload.currency)
        assertEquals(Country(code = "US", name = "United States"), domain.accountPayload.country)
        assertEquals("Alice Doe", domain.accountPayload.holderName)
        assertEquals("ID-123", domain.accountPayload.holderId)
        assertEquals("Bisq Bank", domain.accountPayload.bankName)
        assertEquals("BANKUS33", domain.accountPayload.bankId)
        assertEquals("001", domain.accountPayload.branchId)
        assertEquals("123456789", domain.accountPayload.accountNr)
        assertEquals(BankAccountType.CHECKING, domain.accountPayload.bankAccountType)
        assertEquals("NAT-123", domain.accountPayload.nationalAccountId)
        assertEquals("2026-05-10", domain.creationDate)
        assertEquals("5000.00 USD", domain.tradeLimitInfo)
        assertEquals("5 days", domain.tradeDuration)
    }

    @Test
    fun `toDomain preserves null optional NationalBankAccountDto fields`() {
        val dto =
            NationalBankAccountDto(
                accountName = "National Bank Main",
                accountPayload =
                    NationalBankAccountPayloadDto(
                        chargebackRisk = null,
                        paymentMethodName = "National Bank",
                        currency = FiatCurrencyDto(code = "USD", name = "US Dollar"),
                        country = CountryDto(code = "US", name = "United States"),
                        holderName = null,
                        holderId = null,
                        bankName = null,
                        bankId = null,
                        branchId = null,
                        accountNr = "123456789",
                        bankAccountType = null,
                        nationalAccountId = null,
                    ),
            )

        val domain = dto.toDomain()

        assertNull(domain.accountPayload.chargebackRisk)
        assertNull(domain.accountPayload.holderName)
        assertNull(domain.accountPayload.holderId)
        assertNull(domain.accountPayload.bankName)
        assertNull(domain.accountPayload.bankId)
        assertNull(domain.accountPayload.branchId)
        assertNull(domain.accountPayload.bankAccountType)
        assertNull(domain.accountPayload.nationalAccountId)
    }

    @Test
    fun `toDto maps create NationalBankAccount fields correctly`() {
        val domain =
            CreateNationalBankAccount(
                accountName = "National Bank Main",
                accountPayload =
                    CreateNationalBankAccountPayload(
                        selectedCountryCode = "US",
                        selectedCurrencyCode = "USD",
                        holderName = "Alice Doe",
                        holderId = "ID-123",
                        bankName = "Bisq Bank",
                        bankId = "BANKUS33",
                        branchId = "001",
                        accountNr = "123456789",
                        bankAccountType = BankAccountType.SAVINGS,
                        nationalAccountId = "NAT-123",
                    ),
            )

        val dto = domain.toDto()

        assertIs<CreateNationalBankAccountDto>(dto)
        assertEquals(FiatPaymentRailDto.NATIONAL_BANK, dto.paymentRail)
        assertEquals("National Bank Main", dto.accountName)
        assertEquals("US", dto.accountPayload.selectedCountryCode)
        assertEquals("USD", dto.accountPayload.selectedCurrencyCode)
        assertEquals("Alice Doe", dto.accountPayload.holderName)
        assertEquals("ID-123", dto.accountPayload.holderId)
        assertEquals("Bisq Bank", dto.accountPayload.bankName)
        assertEquals("BANKUS33", dto.accountPayload.bankId)
        assertEquals("001", dto.accountPayload.branchId)
        assertEquals("123456789", dto.accountPayload.accountNr)
        assertEquals(BankAccountTypeDto.SAVINGS, dto.accountPayload.bankAccountType)
        assertEquals("NAT-123", dto.accountPayload.nationalAccountId)
    }

    @Test
    fun `toDto preserves null optional NationalBankAccount fields`() {
        val domain =
            CreateNationalBankAccount(
                accountName = "National Bank Main",
                accountPayload =
                    CreateNationalBankAccountPayload(
                        selectedCountryCode = "US",
                        selectedCurrencyCode = "USD",
                        holderName = null,
                        holderId = null,
                        bankName = null,
                        bankId = null,
                        branchId = null,
                        accountNr = "123456789",
                        bankAccountType = null,
                        nationalAccountId = null,
                    ),
            )

        val dto = domain.toDto()

        assertNull(dto.accountPayload.holderName)
        assertNull(dto.accountPayload.holderId)
        assertNull(dto.accountPayload.bankName)
        assertNull(dto.accountPayload.bankId)
        assertNull(dto.accountPayload.branchId)
        assertNull(dto.accountPayload.bankAccountType)
        assertNull(dto.accountPayload.nationalAccountId)
    }
}
