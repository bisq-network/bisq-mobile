package network.bisq.mobile.data.replicated.common.validation

import com.bayocode.kphonenumber.KPhoneNumber
import com.bayocode.kphonenumber.PhoneNumberException

object PhoneNumberValidation {
    private val validPhoneInput = Regex("^\\+?[0-9 ()-]*$")
    private val kPhoneNumber = KPhoneNumber()

    fun isValid(
        number: String?,
        regionCode: String?,
    ): Boolean {
        if (number.isNullOrEmpty() || regionCode.isNullOrEmpty()) {
            return false
        }

        if (!validPhoneInput.matches(number) || number.count { it == '+' } > 1 || (!number.startsWith('+') && number.contains('+'))) {
            return false
        }

        val digitCount = number.count { it.isDigit() }
        if (digitCount > 15) {
            return false
        }

        return try {
            kPhoneNumber.parse(number, regionCode)
            true
        } catch (_: PhoneNumberException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
