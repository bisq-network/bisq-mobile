package network.bisq.mobile.client.payment_accounts.data.mapping.crypto

import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.CreateOtherCryptoAssetAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.other_crypto.OtherCryptoAssetAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.OtherCryptoAssetAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.OtherCryptoAssetAccountPayload

fun OtherCryptoAssetAccountDto.toDomain(): OtherCryptoAssetAccount =
    OtherCryptoAssetAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun OtherCryptoAssetAccountPayloadDto.toDomain(): OtherCryptoAssetAccountPayload =
    OtherCryptoAssetAccountPayload(
        currencyCode = currencyCode,
        address = address,
        isInstant = isInstant,
        isAutoConf = isAutoConf,
        autoConfNumConfirmations = autoConfNumConfirmations,
        autoConfMaxTradeAmount = autoConfMaxTradeAmount,
        autoConfExplorerUrls = autoConfExplorerUrls,
        currencyName = currencyName,
        supportAutoConf = supportAutoConf,
    )

fun CreateOtherCryptoAssetAccount.toDto(): CreateOtherCryptoAssetAccountDto =
    CreateOtherCryptoAssetAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateOtherCryptoAssetAccountPayload.toDto(): CreateOtherCryptoAssetAccountPayloadDto =
    CreateOtherCryptoAssetAccountPayloadDto(
        currencyCode = currencyCode,
        address = address,
        isInstant = isInstant,
        isAutoConf = isAutoConf,
        autoConfNumConfirmations = autoConfNumConfirmations,
        autoConfMaxTradeAmount = autoConfMaxTradeAmount,
        autoConfExplorerUrls = autoConfExplorerUrls,
    )
