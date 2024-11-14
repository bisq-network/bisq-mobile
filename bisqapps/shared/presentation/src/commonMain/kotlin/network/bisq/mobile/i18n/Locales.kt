package network.bisq.mobile.i18n

/**
 * An object that holds locale codes used for the application’s supported languages.
 *
 * Currently, this object supports English (`EN`) as the default language and French ('FR') only for testing.
 * Additional languages can be added in the future.
 *
 * ## Usage
 * ### Locales.kt
 * Defines all the supported languages
 *
 * ### Strigs.kt
 * A class that defines variable for all text resources used in the app.
 * TODO: This is going to be an insanely huge file. Look for ways to modularize it (for later)
 *
 * ### {Lang}Strings.kt
 * One file for each language. Assigns actual text for each variable in that specific language.
 *
 * ### Actual usage:
 * 1. Each view should be enclosed by `ProviderStrings`. This is now done at top level in App.kt
 *
 *     val lyricist = rememberStrings()
 *     ProvideStrings(lyricist) {}
 *
 * 2. And current language can be changed anytime by doing
 *
 *     lyricist.languageTag = Locales.FR
 *
 * 3. In the views, string resources can be accessed by
 *
 *     LocalStrings.current.{stringResourceName}
 *
 *
 * ## References:
 * For more detailed usage, please refer lyricist documentation @
 * https://github.com/adrielcafe/lyricist/
 * https://github.com/adrielcafe/lyricist/tree/main/sample-multiplatform/src/commonMain/kotlin/cafe/adriel/lyricist/sample/multiplatform (sample project)
 */
object Locales {
    const val EN = "en"
    const val FR = "fr"
}
