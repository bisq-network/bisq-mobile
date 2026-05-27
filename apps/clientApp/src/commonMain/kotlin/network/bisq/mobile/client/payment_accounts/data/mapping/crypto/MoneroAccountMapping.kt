package network.bisq.mobile.client.payment_accounts.data.mapping.crypto

import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.CreateMoneroAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.CreateMoneroAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.MoneroAccountDto
import network.bisq.mobile.client.payment_accounts.data.model.crypto.monero.MoneroAccountPayloadDto
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.MoneroAccount
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.MoneroAccountPayload

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
