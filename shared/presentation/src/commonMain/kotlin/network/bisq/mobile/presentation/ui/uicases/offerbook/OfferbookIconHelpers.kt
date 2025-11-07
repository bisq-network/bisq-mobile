package network.bisq.mobile.presentation.ui.uicases.offerbook

// Shared helpers to build icon paths consistent with offer cards
fun paymentIconPath(id: String): String =
    "drawable/payment/fiat/${id.lowercase().replace("-", "_")}.png"

fun settlementIconPath(id: String): String =
    when (id.uppercase()) {
        "BTC", "MAIN_CHAIN", "ONCHAIN", "ON_CHAIN" -> "drawable/payment/bitcoin/main_chain.png"
        "LIGHTNING", "LN" -> "drawable/payment/bitcoin/ln.png"
        else -> "drawable/payment/bitcoin/${id.lowercase().replace("-", "_")}.png"
    }

