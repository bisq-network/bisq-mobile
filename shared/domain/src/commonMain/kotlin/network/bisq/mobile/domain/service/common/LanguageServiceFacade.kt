package network.bisq.mobile.domain.service.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.ServiceFacade

abstract class LanguageServiceFacade : ServiceFacade() {
    companion object {
         val DEFAULT_TRANSLATABLE_LANGUAGES = mapOf(
            "af-ZA" to "Afrikaans (Afrikaans)",
            "cs" to "Czech (\u010de\u0161tina)",
            "de" to "German (Deutsch)",
            "en" to "English",
            "es" to "Spanish (Espa\u00f1ol)",
            "it" to "Italian (Italiano)",
            "pcm" to "Pidgin (Naij\u00e1)",
            "pt-BR" to "Portuguese (Portugu\u00eas)",
            "ru" to "Russian (\u0440\u0443\u0441\u0441\u043a\u0438\u0439)"
        )

        val DEFAULT_SUPPORTED_LANGUAGES = mapOf(
            "af-ZA" to "Afrikaans (Afrikaans)",
            "sq" to "Albanian (shqip)",
            "am" to "Amharic (አማርኛ)",
            "ar" to "Arabic (العربية)",
            "hy" to "Armenian (հայերեն)",
            "az" to "Azerbaijani (azərbaycan)",
            "bn" to "Bangla (বাংলা)",
            "be" to "Belarusian (беларуская)",
            "bi" to "Bislama (Bislama)",
            "bs" to "Bosnian (bosanski)",
            "bg" to "Bulgarian (български)",
            "my" to "Burmese (မြန်မာ)",
            "ca" to "Catalan (català)",
            "zh" to "Chinese (中文)",
            "hr" to "Croatian (hrvatski)",
            "cs" to "Czech (čeština)",
            "da" to "Danish (dansk)",
            "dv" to "Divehi (Divehi)",
            "nl" to "Dutch (Nederlands)",
            "dz" to "Dzongkha (རྫོང་ཁ)",
            "en" to "English (English)",
            "et" to "Estonian (eesti)",
            "fo" to "Faroese (føroyskt)",
            "fi" to "Finnish (suomi)",
            "fr" to "French (français)",
            "ka" to "Georgian (ქართული)",
            "de" to "German (Deutsch)",
            "el" to "Greek (Ελληνικά)",
            "he" to "Hebrew (עברית)",
            "hi" to "Hindi (हिन्दी)",
            "hu" to "Hungarian (magyar)",
            "is" to "Icelandic (íslenska)",
            "id" to "Indonesian (Indonesia)",
            "ga" to "Irish (Gaeilge)",
            "it" to "Italian (italiano)",
            "ja" to "Japanese (日本語)",
            "kl" to "Kalaallisut (kalaallisut)",
            "kk" to "Kazakh (қазақ тілі)",
            "km" to "Khmer (ខ្មែរ)",
            "rw" to "Kinyarwanda (Kinyarwanda)",
            "ko" to "Korean (한국어)",
            "ky" to "Kyrgyz (кыргызча)",
            "lo" to "Lao (ລາວ)",
            "la" to "Latin (Latin)",
            "lv" to "Latvian (latviešu)",
            "lt" to "Lithuanian (lietuvių)",
            "mk" to "Macedonian (македонски)",
            "ms" to "Malay (Melayu)",
            "mt" to "Maltese (Malti)",
            "mn" to "Mongolian (монгол)",
            "ne" to "Nepali (नेपाली)",
            "no" to "Norwegian (norsk)",
            "ps" to "Pashto (پښتو)",
            "fa" to "Persian (فارسی)",
            "pl" to "Polish (polski)",
            "pt-BR" to "Portuguese (português)",
            "ro" to "Romanian (română)",
            "ru" to "Russian (русский)",
            "sm" to "Samoan (Samoan)",
            "sr" to "Serbian (српски)",
            "si" to "Sinhala (සිංහල)",
            "sk" to "Slovak (slovenčina)",
            "sl" to "Slovenian (slovenščina)",
            "so" to "Somali (Soomaali)",
            "es" to "Spanish (español)",
            "sw" to "Swahili (Kiswahili)",
            "sv" to "Swedish (svenska)",
            "tg" to "Tajik (тоҷикӣ)",
            "th" to "Thai (ไทย)",
            "ti" to "Tigrinya (ትግርኛ)",
            "tr" to "Turkish (Türkçe)",
            "tk" to "Turkmen (türkmen dili)",
            "uk" to "Ukrainian (українська)",
            "uz" to "Uzbek (o‘zbek)",
            "vi" to "Vietnamese (Tiếng Việt)"
        )
    }

    protected val _i18nPairs = MutableStateFlow(DEFAULT_TRANSLATABLE_LANGUAGES)
    val i18nPairs = _i18nPairs.asStateFlow()

    protected val _allPairs = MutableStateFlow(DEFAULT_SUPPORTED_LANGUAGES)
    val allPairs = _allPairs.asStateFlow()

    abstract fun setDefaultLanguage(languageCode: String)

    abstract suspend fun sync()
}