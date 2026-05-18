package network.bisq.mobile.data.mapping.account

import network.bisq.mobile.data.model.account.crypto.create.CreateMoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.create.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateUserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateWiseAccountDto
import network.bisq.mobile.data.model.account.fiat.create.CreateZelleAccountDto
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccountPayload
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccountPayload
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccountPayload
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateWiseAccountPayload
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccountPayload
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import kotlin.test.Test
import kotlin.test.assertIs

class CreatePaymentAccountMappingTest {
    @Test
    fun `toDto dispatches fiat create accounts to rail-specific dtos`() {
        assertIs<CreateZelleAccountDto>(
            createZelleAccount().toDto(),
        )
        assertIs<CreateWiseAccountDto>(
            createWiseAccount().toDto(),
        )
        assertIs<CreateUserDefinedFiatAccountDto>(
            createUserDefinedFiatAccount().toDto(),
        )
    }

    @Test
    fun `toDto dispatches crypto create accounts to rail-specific dtos`() {
        assertIs<CreateMoneroAccountDto>(
            createMoneroAccount().toDto(),
        )
        assertIs<CreateOtherCryptoAssetAccountDto>(
            createOtherCryptoAssetAccount().toDto(),
        )
    }

    private fun createZelleAccount(): CreatePaymentAccount =
        CreateZelleAccount(
            accountName = "Zelle Main",
            accountPayload = CreateZelleAccountPayload(holderName = "Alice", emailOrMobileNr = "alice@example.com"),
        )

    private fun createWiseAccount(): CreatePaymentAccount =
        CreateWiseAccount(
            accountName = "Wise Main",
            accountPayload = CreateWiseAccountPayload(selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro")), holderName = "Alice", email = "alice@example.com"),
        )

    private fun createUserDefinedFiatAccount(): CreatePaymentAccount =
        CreateUserDefinedFiatAccount(
            accountName = "Custom Main",
            accountPayload = CreateUserDefinedFiatAccountPayload(accountData = "IBAN: DE89"),
        )

    private fun createMoneroAccount(): CreatePaymentAccount =
        CreateMoneroAccount(
            accountName = "XMR Main",
            accountPayload =
                CreateMoneroAccountPayload(
                    address = "xmr-address",
                    isInstant = true,
                    isAutoConf = true,
                    autoConfNumConfirmations = 5,
                    autoConfMaxTradeAmount = 100_000L,
                    autoConfExplorerUrls = "https://explorer.example",
                    useSubAddresses = false,
                ),
        )

    private fun createOtherCryptoAssetAccount(): CreatePaymentAccount =
        CreateOtherCryptoAssetAccount(
            accountName = "ETH Main",
            accountPayload =
                CreateOtherCryptoAssetAccountPayload(
                    currencyCode = "ETH",
                    address = "eth-address",
                    isInstant = false,
                ),
        )
}
