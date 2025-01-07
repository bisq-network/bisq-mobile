package network.bisq.mobile.i18n

import network.bisq.mobile.domain.utils.getLogger

object MessageFormat {

    fun format(pattern: String, vararg arguments: Any): String {
        try {
            val args = if (arguments[0] is Array<*>) arguments[0] as Array<*> else arguments

            // Use a regular expression to match placeholders like {0}, {1}, etc.
            val regex = Regex("\\{(\\d+)\\}")

            return regex.replace(pattern) { matchResult ->
                // Extract the index from the placeholder
                val s = matchResult.groupValues[1]
                val index = s.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid placeholder: ${matchResult.value}")

                // Replace with the corresponding argument if it exists
                val indices = args.indices
                if (index in indices) {
                    args[index].toString() ?: "null"
                } else {
                    throw IllegalArgumentException("Missing argument for placeholder: ${matchResult.value}")
                }
            }
        } catch (e: Exception) {
            getLogger("MessageFormat").e("format failed", e)
            return pattern
        }
    }
}
