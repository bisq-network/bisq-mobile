package network.bisq.mobile.data.replicated.common.validation

object SepaPaymentAccountValidation {
    private val ibanRegex = Regex("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$")
    private val bicRegex = Regex("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$")

    fun validateIban(iban: String) {
        val clean = iban.replace(" ", "").uppercase()
        require(clean.isNotEmpty()) { "IBAN must not be empty" }
        require(clean.length in 15..34) { "IBAN length must be between 15 and 34 characters" }
        require(ibanRegex.matches(clean)) { "Invalid IBAN format" }
    }

    fun validateSepaIban(
        iban: String,
        sepaCountryCodes: List<String>,
    ) {
        validateIban(iban)
        val clean = iban.replace(" ", "").uppercase()
        val countryCode = clean.substring(0, 2)
        require(sepaCountryCodes.contains(countryCode)) {
            "IBAN country code '$countryCode' is not a SEPA member country"
        }
    }

    fun validateBic(bic: String) {
        val normalized = bic.uppercase()
        require(normalized.isNotEmpty()) { "BIC must not be empty" }
        require(normalized.length == 8 || normalized.length == 11) { "BIC length must be 8 or 11 characters" }
        require(bicRegex.matches(normalized)) { "Invalid BIC format" }
        require(!normalized.startsWith("REVO")) { "Revolut BIC codes are not supported for SEPA transfers" }
    }

    fun validateIbanMatchesCountryCode(
        iban: String,
        countryCode: String,
    ) {
        val cleanIban = iban.replace(" ", "").uppercase()
        val normalizedCountryCode = countryCode.uppercase()
        require(cleanIban.length >= 2) { "IBAN too short for country code extraction" }
        require(cleanIban.substring(0, 2) == normalizedCountryCode) {
            "IBAN country code '${cleanIban.substring(0, 2)}' does not match declared country '$normalizedCountryCode'"
        }
    }
}
