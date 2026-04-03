package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.crypto.OtherCryptoAssetAccountPayloadDto
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccountPayload

fun OtherCryptoAssetAccountDto.toDomain(): OtherCryptoAssetAccount =
    OtherCryptoAssetAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun OtherCryptoAssetAccount.toDto(): OtherCryptoAssetAccountDto =
    OtherCryptoAssetAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
        tradeLimitInfo = tradeLimitInfo,
        creationDate = creationDate,
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
    )

fun OtherCryptoAssetAccountPayload.toDto(): OtherCryptoAssetAccountPayloadDto =
    OtherCryptoAssetAccountPayloadDto(
        currencyName = currencyName,
        address = address,
        isInstant = isInstant,
        isAutoConf = isAutoConf,
        autoConfNumConfirmations = autoConfNumConfirmations,
        autoConfMaxTradeAmount = autoConfMaxTradeAmount,
        autoConfExplorerUrls = autoConfExplorerUrls,
        currencyCode = currencyCode,
    )
