package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.MoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.MoneroAccountPayload

internal fun CreateMoneroAccount.toReviewPaymentAccount(paymentMethod: CryptoPaymentMethod): MoneroAccount =
    MoneroAccount(
        accountName = accountName,
        accountPayload =
            MoneroAccountPayload(
                address = accountPayload.address,
                isInstant = accountPayload.isInstant,
                currencyName = paymentMethod.name,
                currencyCode = paymentMethod.code,
                isAutoConf = accountPayload.isAutoConf,
                autoConfNumConfirmations = accountPayload.autoConfNumConfirmations,
                autoConfMaxTradeAmount = accountPayload.autoConfMaxTradeAmount,
                autoConfExplorerUrls = accountPayload.autoConfExplorerUrls,
                supportAutoConf = paymentMethod.supportAutoConf,
                useSubAddresses = accountPayload.useSubAddresses,
                mainAddress = accountPayload.mainAddress,
                privateViewKey = accountPayload.privateViewKey,
                subAddress = accountPayload.subAddress,
                accountIndex = accountPayload.accountIndex,
                initialSubAddressIndex = accountPayload.initialSubAddressIndex,
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
