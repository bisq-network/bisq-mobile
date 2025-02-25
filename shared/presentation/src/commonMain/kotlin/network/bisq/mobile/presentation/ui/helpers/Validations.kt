package network.bisq.mobile.presentation.ui.helpers

import kotlin.text.Regex

// Ref: bisq2:common/src/main/java/bisq/common/validation/LightningInvoiceValidation.java
object LightningInvoiceValidation {
    private val LN_BECH32_PATTERN = Regex("^(lnbc|LNBC)(\\d*[munpMUNP]?)1[02-9a-zA-Z]{50,7089}$")

    fun validateInvoice(invoice: String): Boolean {
        return LN_BECH32_PATTERN.matches(invoice)
    }
}

// Ref: bisq2:common/src/main/java/bisq/common/validation/BitcoinAddressValidation.java
object BitcoinAddressValidation {
    private val BASE_58_PATTERN = Regex("^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$")
    private val BECH32_PATTERN = Regex("^(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,60}$")

    fun validateAddress(address: String): Boolean {
        return BASE_58_PATTERN.matches(address) || BECH32_PATTERN.matches(address)
    }
}
