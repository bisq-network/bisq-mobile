package network.bisq.mobile.presentation.ui.helpers

import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.i18n.i18n

// Ref: bisq2:common/src/main/java/bisq/common/validation/LightningInvoiceValidation.java
object LightningInvoiceValidation {
    private val LN_BECH32_PATTERN = Regex("^(lnbc|LNBC)(\\d*[munpMUNP]?)1[02-9a-zA-Z]{50,7089}$")

    fun validateInvoice(invoice: String): Boolean {
        return LN_BECH32_PATTERN.matches(invoice)
    }
}

// Ref: bisq2:common/src/main/java/bisq/common/validation/BitcoinAddressValidation.java
object BitcoinAddressValidation {
    const val MIN_LENGTH: Int = 25
    const val MAX_LENGTH: Int = 62
    private val BASE_58_PATTERN = Regex("^[13][a-km-zA-HJ-NP-Z1-9]{$MIN_LENGTH,34}$")
    private val BECH32_PATTERN = Regex("^(bc1|[13])[a-zA-HJ-NP-Z0-9]{$MIN_LENGTH,$MAX_LENGTH}$")

    fun validateAddress(address: String): Boolean {
        return BASE_58_PATTERN.matches(address) || BECH32_PATTERN.matches(address)
    }
}

// Ref: bisq2:common/src/main/java/bisq/common/validation/LightningPreImageValidation.java
object LightningPreImageValidation {
    private val HEX64_PATTERN = Regex("^[a-fA-F0-9]{64}$")

    fun validatePreImage(preimage: String): Boolean {
        return HEX64_PATTERN.matches(preimage)
    }
}

// Ref: bisq2:common/src/main/java/bisq/common/validation/BitcoinTransactionValidation.java
object BitcoinTransactionValidation {
    private val TXID_PATTERN = Regex("^[a-fA-F0-9]{64}$")

    fun validateTxId(txId: String): Boolean {
        return TXID_PATTERN.matches(txId)
    }
}

object AmountValidator {
    fun validate(value: String, min: Long, max: Long): String? {
        val _value = value.toDoubleOrNullLocaleAware()

        return when {
            _value == null -> "mobile.validations.amountValidator.invalidNumber".i18n()
            _value * 10000 < min-> "mobile.validations.amountValidator.shouldBeGreaterThan".i18n() + " ${min/ 10000.0}"
            _value * 10000 > max-> "mobile.validations.amountValidator.shouldBeLessThan".i18n() + " ${max/ 10000.0}"
            else -> null
        }
    }
}