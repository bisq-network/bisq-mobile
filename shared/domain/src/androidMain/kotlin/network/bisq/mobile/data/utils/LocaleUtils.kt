package network.bisq.mobile.data.utils

import java.util.Locale

fun locale(
    language: String,
    country: String,
): Locale =
    Locale
        .Builder()
        .setLanguage(language)
        .setRegion(country)
        .build()
