package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.OtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.crypto.OtherCryptoAssetAccountPayloadDto
import network.bisq.mobile.data.model.account.crypto.create.CreateOtherCryptoAssetAccountDto
import network.bisq.mobile.data.model.account.crypto.create.CreateOtherCryptoAssetAccountPayloadDto
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccountPayload
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
