package network.bisq.mobile.domain.data.model

// TODO: Update later based on model class from bisq2 lib
data class BisqOffer (
    val id: String = "offer_283UANJD19A",
    val isBuy: Boolean = true,
    val amIMaker: Boolean = false,
    val price: Number = 97000,
    val currency: String = "USD",
    val fiatAmount: Number = 1000, //Should be a range
    val satsAmount: Number= 1030927, //Should be a range

    val partyName: String = "satoshi",
    val partyRatings: Double = 4.2,
    val partyDP: String = "", // Image URL
)

class MyTrades(val trades: List<BisqOffer> = listOf()): BaseModel()

interface MyTradesFactory {
    fun createMyTrades(): MyTrades
}

class DefaultMyTradesFactory : MyTradesFactory {
    override fun createMyTrades() = MyTrades()
}