package network.bisq.mobile.domain.data.model

class BtcPrice: BaseModel() {
    val prices: Map<String, Double> = mapOf(
        "USD" to 64000.50,
        "EUR" to 58000.75,
        "GBP" to 52000.30,
    )
}

interface BtcPriceFactory {
    fun createBtcPrice(): BtcPrice
}

class DefaultBtcPriceeFactory : BtcPriceFactory {
    override fun createBtcPrice() = BtcPrice()
}