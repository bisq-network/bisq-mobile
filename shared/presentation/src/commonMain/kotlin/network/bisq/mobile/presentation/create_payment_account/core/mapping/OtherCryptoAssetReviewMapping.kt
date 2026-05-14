package network.bisq.mobile.presentation.create_payment_account.core.mapping

import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccountPayload

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
