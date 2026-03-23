package network.bisq.mobile.node.common.domain.mapping

import bisq.account.accounts.AccountOrigin
import bisq.account.timestamp.KeyType
import bisq.common.util.StringUtils
import bisq.security.keys.KeyGeneration
import bisq.account.accounts.fiat.UserDefinedFiatAccount as Bisq2UserDefinedFiatAccount
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload as Bisq2UserDefinedFiatAccountPayload
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount as DomainUserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccountPayload as DomainUserDefinedFiatAccountPayload

fun Bisq2UserDefinedFiatAccount.toDomain(): DomainUserDefinedFiatAccount =
    DomainUserDefinedFiatAccount(
        accountName = accountName,
        accountPayload = accountPayload.toDomain(),
    )

fun DomainUserDefinedFiatAccount.toBisq2(): Bisq2UserDefinedFiatAccount {
    val payload = accountPayload.toBisq2()
    val keyPair = KeyGeneration.generateDefaultEcKeyPair()
    val keyAlgorithm = KeyType.EC
    return Bisq2UserDefinedFiatAccount(
        StringUtils.createUid(),
        System.currentTimeMillis(),
        accountName,
        payload,
        keyPair,
        keyAlgorithm,
        AccountOrigin.BISQ2_NEW,
    )
}

fun Bisq2UserDefinedFiatAccountPayload.toDomain(): DomainUserDefinedFiatAccountPayload =
    DomainUserDefinedFiatAccountPayload(
        accountData = accountData,
    )

fun DomainUserDefinedFiatAccountPayload.toBisq2(): Bisq2UserDefinedFiatAccountPayload =
    Bisq2UserDefinedFiatAccountPayload(
        StringUtils.createUid(),
        accountData,
    )
