package network.bisq.mobile.node.common.domain.mapping

import bisq.account.accounts.fiat.UserDefinedFiatAccount
import bisq.account.accounts.fiat.UserDefinedFiatAccountPayload
import bisq.common.util.StringUtils
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO

object UserDefinedFiatAccountMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountVO): UserDefinedFiatAccount {
        val payload = UserDefinedFiatAccountPayloadMapping.toBisq2Model(value.accountPayload)
        return UserDefinedFiatAccount(
            StringUtils.createUid(),
            System.currentTimeMillis(),
            value.accountName,
            payload,
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
