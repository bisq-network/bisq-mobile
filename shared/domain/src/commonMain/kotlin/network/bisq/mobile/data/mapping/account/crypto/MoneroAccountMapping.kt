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
    )

fun MoneroAccount.toDto(): MoneroAccountDto =
    MoneroAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
        creationDate = creationDate,
    )

fun MoneroAccountPayloadDto.toDomain(): MoneroAccountPayload =
    MoneroAccountPayload(
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
    )

fun MoneroAccountPayload.toDto(): MoneroAccountPayloadDto =
    MoneroAccountPayloadDto(
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
    )
