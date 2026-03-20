package network.bisq.mobile.data.replicated.common.validation

object NetworkDataValidation {
    fun validateCode(code: String) {
        require(code.length > 1) { "Code too short" }
        require(code.length < 10) { "Code too long" }
    }

    fun validateRequiredText(
        text: String,
        minLength: Int,
        maxLength: Int,
    ) {
        require(text.isNotEmpty()) { "Text must not be null or empty" }
        require(text.length >= minLength) { "Text must not be shorter than $minLength" }
        require(text.length <= maxLength) { "Text must not be longer than $maxLength" }
    }

    fun validateText(
        text: String?,
        maxLength: Int,
    ) {
        if (text != null) {
            require(text.length <= maxLength) { "Text must not be longer than $maxLength" }
        }
    }
}
