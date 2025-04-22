package network.bisq.mobile.client.service.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade

class ClientLanguageServiceFacade : ServiceFacade(), LanguageServiceFacade {

    // Properties
    private val _i18nPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val i18nPairs: StateFlow<List<Pair<String, String>>> = _i18nPairs

    private val _allPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val allPairs: StateFlow<List<Pair<String, String>>> = _allPairs

    private val _defaultLanguage: MutableStateFlow<String> = MutableStateFlow("en")

    // Misc

    // Life cycle
    override fun activate() {
        super<ServiceFacade>.activate()

        serviceScope.launch(Dispatchers.Default) {

            _i18nPairs.value = listOf(
                "en" to "English (English)",
                "de" to "German (Deutsch)",
                "es" to "Spanish (español)",
                "it" to "Italian (italiano)",
                "pt-BR" to "Portuguese (português (Brasil))",
                "cs" to "Czech (čeština)",
                "pcm" to "Nigerian Pidgin (Naijíriá Píjin)",
                "ru" to "Russian (русский)",
                "af-ZA" to "Afrikaans (Afrikaans (Suid-Afrika))",
            )

            _allPairs.value = listOf(
                "af" to "Afrikaans (Afrikaans)",
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
                "pt" to "Portuguese (português)",
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
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override fun setDefaultLanguage(languageCode: String) {
        _defaultLanguage.value = languageCode
    }

    override suspend fun sync() {
        activate()
    }
}