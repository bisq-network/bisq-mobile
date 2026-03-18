package network.bisq.mobile.node.common.domain.mapping

import bisq.account.accounts.AccountOrigin
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload
import bisq.account.timestamp.KeyType
import bisq.common.util.StringUtils
import bisq.security.keys.KeyGeneration
import network.bisq.mobile.data.replicated.api.dto.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.replicated.api.dto.account.fiat.UserDefinedFiatAccountPayloadDto

object UserDefinedFiatAccountMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountDto): UserDefinedFiatAccount {
        val payload = UserDefinedFiatAccountPayloadMapping.toBisq2Model(value.accountPayload)
        val keyPair = KeyGeneration.generateDefaultEcKeyPair()
        val keyAlgorithm = KeyType.EC
        return UserDefinedFiatAccount(
            StringUtils.createUid(),
            System.currentTimeMillis(),
            value.accountName,
            payload,
            keyPair,
            keyAlgorithm,
            AccountOrigin.BISQ2_NEW,
        )
    }

    fun fromBisq2Model(value: UserDefinedFiatAccount): UserDefinedFiatAccountDto =
        UserDefinedFiatAccountDto(
            value.accountName,
            UserDefinedFiatAccountPayloadMapping.fromBisq2Model(value.accountPayload),
        )
}

object UserDefinedFiatAccountPayloadMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountPayloadDto): UserDefinedFiatAccountPayload =
        UserDefinedFiatAccountPayload(
            StringUtils.createUid(),
            value.accountData,
        )

    fun fromBisq2Model(value: UserDefinedFiatAccountPayload): UserDefinedFiatAccountPayloadDto =
        UserDefinedFiatAccountPayloadDto(
            value.accountData,
        )
}
