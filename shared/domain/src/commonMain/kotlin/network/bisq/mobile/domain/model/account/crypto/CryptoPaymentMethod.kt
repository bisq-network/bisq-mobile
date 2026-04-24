package network.bisq.mobile.domain.model.account.crypto

data class CryptoPaymentMethod(
    val code: String,
    val name: String,
    val category: String,
    val supportAutoConf: Boolean,
)
