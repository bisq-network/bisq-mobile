package network.bisq.mobile.presentation.offerbook

// Shared helpers to build icon paths consistent with offer cards
fun paymentIconPath(id: String): String = "files/payment/fiat/${id.lowercase().replace("-", "_")}.png"

fun settlementIconPath(id: String): String =
    when (id.uppercase()) {
        "BTC", "MAIN_CHAIN", "ONCHAIN", "ON_CHAIN" -> "files/payment/bitcoin/main_chain.png"
        "LIGHTNING", "LN" -> "files/payment/bitcoin/ln.png"
        else -> "files/payment/bitcoin/${id.lowercase().replace("-", "_")}.png"
    }
