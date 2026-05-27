package network.bisq.mobile.client.payment_accounts.domain.repository

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountCountryDetailsDto

interface BankAccountCountryDetailsRepository {
    suspend fun get(
        countryCode: String,
        refresh: suspend () -> List<BankAccountCountryDetailsDto>,
    ): BankAccountCountryDetailsDto
}
