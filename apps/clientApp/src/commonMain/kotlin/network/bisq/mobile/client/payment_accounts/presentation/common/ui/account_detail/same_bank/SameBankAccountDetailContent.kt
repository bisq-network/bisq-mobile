package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.same_bank

import androidx.compose.runtime.Composable
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.SameBankAccount
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.bank.BankAccountDetailContent

@Composable
fun SameBankAccountDetailContent(account: SameBankAccount) {
    BankAccountDetailContent(
        account = account,
        paymentType = PaymentTypeVO.SAME_BANK,
    )
}
