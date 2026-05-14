package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.MoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.MoneroAccountPayloadDto
import network.bisq.mobile.data.model.account.crypto.create.CreateMoneroAccountDto
import network.bisq.mobile.data.model.account.crypto.create.CreateMoneroAccountPayloadDto
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccountPayload
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

fun CreateMoneroAccount.toDto(): CreateMoneroAccountDto =
    CreateMoneroAccountDto(
        accountName = accountName,
        accountPayload = accountPayload.toDto(),
    )

fun CreateMoneroAccountPayload.toDto(): CreateMoneroAccountPayloadDto =
    CreateMoneroAccountPayloadDto(
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
