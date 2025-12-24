package network.bisq.mobile.node.common.domain.mapping

import bisq.account.accounts.UserDefinedFiatAccount
import bisq.account.accounts.UserDefinedFiatAccountPayload
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO

object UserDefinedFiatAccountMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountVO): UserDefinedFiatAccount =
        UserDefinedFiatAccount(
            value.accountName,
            value.accountPayload.accountData,
        )

    fun fromBisq2Model(value: UserDefinedFiatAccount): UserDefinedFiatAccountVO =
        UserDefinedFiatAccountVO(
            value.accountName,
            UserDefinedFiatAccountPayloadMapping.fromBisq2Model(value.accountPayload),
        )
}

object UserDefinedFiatAccountPayloadMapping {
    fun toBisq2Model(value: UserDefinedFiatAccountPayloadVO): UserDefinedFiatAccountPayload =
        UserDefinedFiatAccountPayload(
            "",
            "",
            value.accountData,
        )

    fun fromBisq2Model(value: UserDefinedFiatAccountPayload): UserDefinedFiatAccountPayloadVO =
        UserDefinedFiatAccountPayloadVO(
            value.accountData,
        )
}
