package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.national_bank

import androidx.compose.runtime.Composable
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.NationalBankAccount
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.bank.BankAccountDetailContent

@Composable
fun NationalBankAccountDetailContent(account: NationalBankAccount) {
    BankAccountDetailContent(
        account = account,
        paymentType = PaymentTypeVO.NATIONAL_BANK,
    )
}
