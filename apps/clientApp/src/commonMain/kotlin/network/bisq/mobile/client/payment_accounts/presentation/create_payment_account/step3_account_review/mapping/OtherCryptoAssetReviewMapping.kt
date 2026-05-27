package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.OtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.OtherCryptoAssetAccountPayload

internal fun CreateOtherCryptoAssetAccount.toReviewPaymentAccount(paymentMethod: CryptoPaymentMethod): OtherCryptoAssetAccount =
    OtherCryptoAssetAccount(
        accountName = accountName,
        accountPayload =
            OtherCryptoAssetAccountPayload(
                currencyCode = accountPayload.currencyCode,
                currencyName = paymentMethod.name,
                address = accountPayload.address,
                isInstant = accountPayload.isInstant,
                isAutoConf = accountPayload.isAutoConf,
                autoConfNumConfirmations = accountPayload.autoConfNumConfirmations,
                autoConfMaxTradeAmount = accountPayload.autoConfMaxTradeAmount,
                autoConfExplorerUrls = accountPayload.autoConfExplorerUrls,
                supportAutoConf = paymentMethod.supportAutoConf,
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
