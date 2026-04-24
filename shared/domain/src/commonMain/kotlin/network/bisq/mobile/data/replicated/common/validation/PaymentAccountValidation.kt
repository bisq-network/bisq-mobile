package network.bisq.mobile.data.replicated.common.validation

import network.bisq.mobile.data.replicated.common.asset.fiatCurrencyMap

object PaymentAccountValidation {
    const val HOLDER_NAME_MIN_LENGTH: Int = 2
    const val HOLDER_NAME_MAX_LENGTH: Int = 70

    // Values taken from bisq2 bisq.desktop.main.content.user.accounts.fiat_accounts.create.summary PaymentSummaryModel
    const val ACCOUNT_NAME_MIN_LENGTH: Int = 2
    const val ACCOUNT_NAME_MAX_LENGTH: Int = 50

    fun validateHolderName(name: String) {
        val trimmed = name.trim()
        require(trimmed.length in HOLDER_NAME_MIN_LENGTH..HOLDER_NAME_MAX_LENGTH) {
            "Holder name must be between $HOLDER_NAME_MIN_LENGTH and $HOLDER_NAME_MAX_LENGTH characters"
        }
    }

    fun validateUniqueAccountName(name: String) {
        val trimmed = name.trim()
        require(trimmed.length in ACCOUNT_NAME_MIN_LENGTH..ACCOUNT_NAME_MAX_LENGTH) {
            "Unique account name must be between $ACCOUNT_NAME_MIN_LENGTH and $ACCOUNT_NAME_MAX_LENGTH characters"
        }
    }

    fun validateCurrencyCodes(currencyCodes: List<String>) {
        require(currencyCodes.isNotEmpty()) { "Currency codes must not be empty" }
        currencyCodes.forEach(::validateCurrencyCode)
    }

    fun validateCurrencyCodes(
        currencyCodes: List<String>,
        allowedCurrencyCodes: List<String>,
        contextDescription: String,
    ) {
        require(currencyCodes.isNotEmpty()) { "Currency codes list must not be empty for $contextDescription" }
        require(allowedCurrencyCodes.isNotEmpty()) { "Allowed currency codes list must not be empty for $contextDescription" }

        allowedCurrencyCodes.forEach(::validateCurrencyCode)
        currencyCodes.forEach { currencyCode ->
            validateCurrencyCode(currencyCode)
            require(allowedCurrencyCodes.contains(currencyCode)) {
                "Currency code '$currencyCode' is not supported for $contextDescription. Supported currencies: $allowedCurrencyCodes"
            }
        }
    }

    fun validateCurrencyCode(currencyCode: String) {
        require(fiatCurrencyMap.containsKey(currencyCode)) { "No Fiat currency found for $currencyCode" }
    }

    fun validateFasterPaymentsSortCode(sortCode: String) {
        require(sortCode.isNotEmpty() && sortCode.length == 6) { "UK sort code must consist of 6 numbers." }
        require(sortCode.all { it.isDigit() }) { "UK sort code must consist of numbers." }
    }

    fun validateFasterPaymentsAccountNr(accountNr: String) {
        require(accountNr.isNotEmpty() && accountNr.length == 8) { "Account number must consist of 8 numbers." }
        require(accountNr.all { it.isDigit() }) { "Account number must consist of numbers." }
    }
}
