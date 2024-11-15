package network.bisq.mobile.domain.data.model

open class BtcPrice: BaseModel() {

    protected open val prices: Map<String, Double> = mapOf(
        "USD" to 64000.50,
        "EUR" to 58000.75,
        "GBP" to 52000.30,
    )

    fun getBtcPrice(): Map<String, Double> {
        return prices
    }

}

interface BtcPriceFactory {
    fun createBtcPrice(): BtcPrice
}

class DefaultBtcPriceeFactory : BtcPriceFactory {
    override fun createBtcPrice() = BtcPrice()
}