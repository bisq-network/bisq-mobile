package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountTypeDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import kotlin.test.Test
import kotlin.test.assertEquals

class BankAccountTypeMappingTest {
    @Test
    fun `toDomain maps every BankAccountTypeDto to matching domain type`() {
        BankAccountTypeDto.entries.forEach { dto ->
            assertEquals(BankAccountType.valueOf(dto.name), dto.toDomain())
        }
    }

    @Test
    fun `toDto maps every BankAccountType to matching dto type`() {
        BankAccountType.entries.forEach { domain ->
            assertEquals(BankAccountTypeDto.valueOf(domain.name), domain.toDto())
        }
    }
}
