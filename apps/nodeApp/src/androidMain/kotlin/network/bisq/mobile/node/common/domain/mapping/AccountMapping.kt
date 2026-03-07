package network.bisq.mobile.node.common.domain.mapping

import bisq.account.accounts.AccountOrigin
import bisq.account.accounts.fiat.UserDefinedFiatAccount
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload
import bisq.account.timestamp.KeyType
import bisq.common.util.StringUtils
import bisq.security.keys.KeyGeneration
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO

object UserDefinedFiatAccountMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountVO): UserDefinedFiatAccount {
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

    fun fromBisq2Model(value: UserDefinedFiatAccount): UserDefinedFiatAccountVO =
        UserDefinedFiatAccountVO(
            value.accountName,
            UserDefinedFiatAccountPayloadMapping.fromBisq2Model(value.accountPayload),
        )
}

object UserDefinedFiatAccountPayloadMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountPayloadVO): UserDefinedFiatAccountPayload =
        UserDefinedFiatAccountPayload(
            StringUtils.createUid(),
            value.accountData,
        )

    fun fromBisq2Model(value: UserDefinedFiatAccountPayload): UserDefinedFiatAccountPayloadVO =
        UserDefinedFiatAccountPayloadVO(
            value.accountData,
        )
}
