package network.bisq.mobile.data.replicated.contract

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO

@Serializable
data class ContractSignatureDataVO(
    val contractHashEncoded: String,
    val signatureEncoded: String,
    val publicKey: PublicKeyVO,
)
