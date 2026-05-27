package network.bisq.mobile.client.payment_accounts.data.mapping

import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.CreateMoneroAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.revolut.CreateRevolutAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined.CreateUserDefinedFiatAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.wise.CreateWiseAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.fiat.zelle.CreateZelleAccountDto
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccountPayload
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateUserDefinedFiatAccountPayload
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
        assertIs<CreateRevolutAccountDto>(
            createRevolutAccount().toDto(),
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

    private fun createRevolutAccount(): CreatePaymentAccount =
        CreateRevolutAccount(
            accountName = "Revolut Main",
            accountPayload = CreateRevolutAccountPayload(userName = "alice", selectedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar"), FiatCurrency(code = "EUR", name = "Euro"))),
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
