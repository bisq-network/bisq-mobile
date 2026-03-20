package network.bisq.mobile.data.replicated.common.validation.fiat

import network.bisq.mobile.data.replicated.common.validation.EmailValidation

object AdvancedCashAccountNrValidation {
    private val accountNrPattern = Regex("^[A-Za-z]\\d{12}$")

    fun isValid(accountNr: String): Boolean =
        accountNr.isNotEmpty() &&
            (EmailValidation.isValid(accountNr) || accountNrPattern.matches(accountNr))
}
