package network.bisq.mobile.domain.data.replicated.contract

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.security.keys.PublicKeyVO

@Serializable
data class ContractSignatureDataVO(val contractHashEncoded: String, val signatureEncoded: String, val publicKey: PublicKeyVO)