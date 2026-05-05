package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.MoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.MoneroAccountPayloadDto
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload

fun MoneroAccountDto.toDomain(): MoneroAccount =
    MoneroAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun MoneroAccount.toDto(): MoneroAccountDto =
    MoneroAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
        creationDate = creationDate,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )

fun MoneroAccountPayloadDto.toDomain(): MoneroAccountPayload =
    MoneroAccountPayload(
        currencyName = currencyName,
        address = address,
        isInstant = isInstant,
        isAutoConf = isAutoConf,
        autoConfNumConfirmations = autoConfNumConfirmations,
        autoConfMaxTradeAmount = autoConfMaxTradeAmount,
        autoConfExplorerUrls = autoConfExplorerUrls,
        useSubAddresses = useSubAddresses,
        mainAddress = mainAddress,
        privateViewKey = privateViewKey,
        subAddress = subAddress,
        accountIndex = accountIndex,
        initialSubAddressIndex = initialSubAddressIndex,
        supportAutoConf = supportAutoConf,
        currencyCode = currencyCode,
    )

fun MoneroAccountPayload.toDto(): MoneroAccountPayloadDto =
    MoneroAccountPayloadDto(
        currencyName = currencyName,
        currencyCode = currencyCode,
        address = address,
        isInstant = isInstant,
        isAutoConf = isAutoConf,
        autoConfNumConfirmations = autoConfNumConfirmations,
        autoConfMaxTradeAmount = autoConfMaxTradeAmount,
        autoConfExplorerUrls = autoConfExplorerUrls,
        useSubAddresses = useSubAddresses,
        mainAddress = mainAddress,
        privateViewKey = privateViewKey,
        subAddress = subAddress,
        accountIndex = accountIndex,
        initialSubAddressIndex = initialSubAddressIndex,
        supportAutoConf = supportAutoConf,
    )
