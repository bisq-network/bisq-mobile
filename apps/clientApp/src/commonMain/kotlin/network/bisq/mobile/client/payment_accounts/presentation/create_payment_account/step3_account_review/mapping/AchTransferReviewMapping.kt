package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.mapping

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.AchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.AchTransferAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccount

internal fun CreateAchTransferAccount.toReviewPaymentAccount(paymentMethod: FiatPaymentMethod): AchTransferAccount =
    AchTransferAccount(
        accountName = accountName,
        accountPayload =
            AchTransferAccountPayload(
                chargebackRisk = paymentMethod.chargebackRisk,
                paymentMethodName = paymentMethod.name,
                currency = paymentMethod.supportedCurrencies.first(),
                country = paymentMethod.supportedCountries.first(),
                holderName = accountPayload.holderName,
                holderAddress = accountPayload.holderAddress,
                bankName = accountPayload.bankName,
                routingNr = accountPayload.routingNr,
                accountNr = accountPayload.accountNr,
                bankAccountType = accountPayload.bankAccountType,
            ),
        creationDate = null,
        tradeLimitInfo = paymentMethod.tradeLimitInfo,
        tradeDuration = paymentMethod.tradeDuration,
    )
