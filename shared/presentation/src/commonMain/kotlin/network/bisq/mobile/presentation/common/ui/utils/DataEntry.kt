package network.bisq.mobile.presentation.common.ui.utils

/**
 * A data class that encapsulates a field value with its validation state and logic.
 *
 * This class is designed to reduce boilerplate in UiState classes where each input field
 * typically requires both a value property and an error message property.
 *
 * @property value The current value of the field
 * @property errorMessage The validation error message, null if valid
 * @property validator Optional validation function that takes the value and returns an error message or null
 *
 * Example usage:
 * ```
 * // In UiState:
 * data class MyUiState(
 *     val nameEntry: DataEntry = DataEntry()
 * )
 *
 * // In Presenter:
 * // 1. Define validation function
 * private fun validateNameField(value: String): String? =
 *     when {
 *         value.length < 3 -> "Name must be at least 3 characters"
 *         value.length > 100 -> "Name must be at most 100 characters"
 *         else -> null
 *     }
 *
 * // 2. Initialize with validator at UiState creation (IMPORTANT)
 * private val _uiState = MutableStateFlow(
 *     MyUiState(
 *         nameEntry = DataEntry(validator = ::validateNameField)
 *     )
 * )
 *
 * // 3. Update value when user types (clears error automatically)
 * fun onNameChange(newValue: String) {
 *     _uiState.update { it.copy(nameEntry = it.nameEntry.updateValue(newValue)) }
 * }
 *
 * // 4. Validate on submit
 * fun onSubmit() {
 *     val validatedEntry = _uiState.value.nameEntry.validate()
 *     if (validatedEntry.isValid) {
 *         // Process valid data
 *     } else {
 *         _uiState.update { it.copy(nameEntry = validatedEntry) }
 *     }
 * }
 * ```
 */
data class DataEntry(
    val value: String = EMPTY_STRING,
    val errorMessage: String? = null,
    val validator: ((String) -> String?)? = null,
) {
    /**
     * Returns true if there is no error message
     */
    val isValid: Boolean
        get() = errorMessage == null

    /**
     * Updates the value and clears any existing error message.
     * Typically used when the user is typing/changing the field.
     *
     * @param newValue The new value for the field
     * @return A new DataEntry with the updated value and cleared error
     */
    fun updateValue(newValue: String): DataEntry = copy(value = newValue, errorMessage = null)

    /**
     * Validates the current value using the validator function if present.
     *
     * @return A new DataEntry with the validation result in errorMessage
     */
    fun validate(): DataEntry {
        val error = validator?.invoke(value)
        return copy(errorMessage = error)
    }

    /**
     * Sets a specific error message without running validation.
     * Useful for server-side validation errors or custom error conditions.
     *
     * @param error The error message to set
     * @return A new DataEntry with the specified error message
     */
    fun withError(error: String?): DataEntry = copy(errorMessage = error)

    /**
     * Clears the error message without changing the value.
     *
     * @return A new DataEntry with no error message
     */
    fun clearError(): DataEntry = copy(errorMessage = null)
}
