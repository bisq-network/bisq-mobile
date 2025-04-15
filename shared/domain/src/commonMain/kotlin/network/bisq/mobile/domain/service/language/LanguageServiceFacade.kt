package network.bisq.mobile.domain.service.language

interface LanguageServiceFacade {
    suspend fun getLanguageCode(): String
    suspend fun setLanguageCode(languageCode: String)
}