package network.bisq.mobile.domain.repository

import network.bisq.mobile.data.model.account.fiat.BankAccountCountryDetailsDto

interface BankAccountCountryDetailsRepository {
    suspend fun get(
        countryCode: String,
        refresh: suspend () -> List<BankAccountCountryDetailsDto>,
    ): BankAccountCountryDetailsDto
}
