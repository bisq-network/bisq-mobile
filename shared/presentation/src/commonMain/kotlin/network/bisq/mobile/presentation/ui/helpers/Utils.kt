package network.bisq.mobile.presentation.ui.helpers

fun convertToSet(value: String?): Set<String> = value?.let { setOf(it) } ?: emptySet()
