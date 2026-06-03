package network.bisq.mobile.data.replicated.common.validation

object SepaPaymentAccountValidation {
    private val ibanRegex = Regex("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$")
    private val bicRegex = Regex("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$")

    fun validateIban(iban: String?) {
        require(!iban.isNullOrEmpty()) { "IBAN must not be null or empty" }
        require(ibanRegex.matches(iban)) {
            "Invalid IBAN format. Must start with country code (2 letters) followed by checksum digits (2 numbers) and BBAN."
        }
        require(iban.length in 15..34) { "IBAN length must be between 15 and 34 characters." }

        validateIbanChecksum(iban)
    }

    fun validateSepaIban(
        iban: String,
        sepaCountryCodes: Set<String>,
    ) {
        val normalizedIban = iban.replace(Regex("\\s"), "").uppercase()
        validateIban(normalizedIban)
        val countryCode = normalizedIban.substring(0, 2)
        require(sepaCountryCodes.contains(countryCode)) {
            "IBAN country code '$countryCode' is not a SEPA member country. Only SEPA countries are supported for SEPA transfers."
        }
    }

    fun validateBic(bic: String) {
        require(bic.isNotEmpty()) { "BIC must not be null or empty" }
        require(bic.length == 8 || bic.length == 11) { "BIC length must be 8 or 11 characters." }
        require(bicRegex.matches(bic)) {
            "Invalid BIC/SWIFT format. Must follow pattern of institution code (4 letters) + country code (2 letters) + location code (2 alphanumeric) + optional branch code (3 alphanumeric)."
        }

        val upperBic = bic.uppercase()

        for (i in 0 until 6) {
            require(upperBic[i].isLetter()) {
                "BIC bank code and country code must be letters only. Invalid character at position $i."
            }
        }

        val locationFirst = upperBic[6]
        val locationSecond = upperBic[7]
        require(locationFirst != '0' && locationFirst != '1') {
            "BIC location code cannot start with 0 or 1 (reserved for test purposes)."
        }
        require(locationSecond != 'O') {
            "BIC location code cannot end with letter O (to avoid confusion with zero)."
        }

        if (upperBic.length == 11 && upperBic[8] == 'X') {
            require(upperBic[9] == 'X' && upperBic[10] == 'X') {
                "BIC branch code starting with X must be XXX."
            }
        }

        require(!upperBic.startsWith("REVO")) {
            "Revolut BIC codes are not supported for traditional SEPA transfers."
        }
    }

    fun validateIbanMatchesCountryCode(
        iban: String,
        countryCode: String,
    ) {
        require(iban.isNotEmpty()) { "IBAN must not be empty for country consistency check" }
        require(countryCode.isNotEmpty()) { "Country code must not be empty for IBAN consistency check" }
        require(iban.length >= 2) { "IBAN too short for country code extraction." }

        val cleanIban = iban.replace(Regex("\\s"), "").uppercase()
        val ibanCountryCode = cleanIban.substring(0, 2)
        require(countryCode == ibanCountryCode) {
            "IBAN country code '$ibanCountryCode' does not match declared country '$countryCode'."
        }
    }

    private fun validateIbanChecksum(iban: String) {
        try {
            val upperIban = iban.uppercase()

            require(upperIban[0].isLetter() && upperIban[1].isLetter()) { "IBAN country code must be letters." }
            require(upperIban[2].isDigit() && upperIban[3].isDigit()) { "IBAN check digits must be numeric." }

            val rearranged = upperIban.substring(4) + upperIban.substring(0, 4)
            val result =
                rearranged.fold(0) { remainder, char ->
                    when {
                        char.isDigit() -> (remainder * 10 + char.digitToInt()) % 97
                        char.isLetter() -> {
                            val letterValue = char - 'A' + 10
                            ((remainder * 100) + letterValue) % 97
                        }
                        else -> throw IllegalArgumentException(
                            "IBAN contains invalid characters. Only letters and digits are allowed.",
                        )
                    }
                }

            require(result == 1) { "IBAN checksum validation failed. The IBAN appears to contain errors." }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("IBAN checksum validation failed due to invalid format.", e)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("IBAN checksum validation failed. The IBAN appears to contain errors.", e)
        }
    }
}
