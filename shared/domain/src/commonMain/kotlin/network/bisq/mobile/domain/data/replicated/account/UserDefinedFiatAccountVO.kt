package network.bisq.mobile.domain.data.replicated.account

data class UserDefinedFiatAccountVO(
    val accountName: String,
    val accountPayload: UserDefinedFiatAccountPayloadVO
)
//data class UserDefinedFiatAccountVO(
//    override val accountName: String,
//    override val paymentMethod: FiatPaymentMethodVO,
//    override val accountPayload: UserDefinedFiatAccountPayloadVO
//) : AccountVO<UserDefinedFiatAccountPayloadVO, FiatPaymentMethodVO>(accountName, paymentMethod, accountPayload)
