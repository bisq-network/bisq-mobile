package network.bisq.mobile.data.replicated.common.validation

object EmailValidation {
    private val emailPattern =
        Regex(
            pattern = "^(?!.*\\.\\.)[A-Z0-9_%+-]+(?:\\.[A-Z0-9_%+-]+)*@(?:[A-Z0-9](?:[A-Z0-9-]*[A-Z0-9])?\\.)+[A-Z]{2,}$",
            option = RegexOption.IGNORE_CASE,
        )

    fun isValid(email: String?): Boolean {
        if (email == null || email.length > 254) {
            return false
        }
        return emailPattern.matches(email)
    }
}
