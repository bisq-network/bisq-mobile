package network.bisq.mobile.data.replicated.contract

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

@Serializable
data class BisqEasyContractVO(
    val takeOfferDate: Long,
    val offer: BisqEasyOfferVO,
    val maker: PartyVO,
    val taker: PartyVO,
    val baseSideAmount: Long,
    val quoteSideAmount: Long,
    val baseSidePaymentMethodSpec: BitcoinPaymentMethodSpecVO,
    val quoteSidePaymentMethodSpec: FiatPaymentMethodSpecVO,
    val mediator: UserProfileVO?,
    val priceSpec: PriceSpecVO,
    val marketPrice: Long,
)
