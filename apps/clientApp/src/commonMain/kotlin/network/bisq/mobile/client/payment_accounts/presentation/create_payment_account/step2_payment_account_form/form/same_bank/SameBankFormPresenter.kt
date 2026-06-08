package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.same_bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.CreateSameBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.CreateSameBankAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank.BankAccountCreatePayloadData
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank.BankAccountFormPresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class SameBankFormPresenter(
    paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BankAccountFormPresenter<CreateSameBankAccount>(paymentAccountsServiceFacade, mainPresenter) {
    override fun createAccount(
        accountName: String,
        payloadData: BankAccountCreatePayloadData,
    ): CreateSameBankAccount =
        CreateSameBankAccount(
            accountName = accountName,
            accountPayload =
                CreateSameBankAccountPayload(
                    selectedCountryCode = payloadData.selectedCountryCode,
                    selectedCurrencyCode = payloadData.selectedCurrencyCode,
                    holderName = payloadData.holderName,
                    holderId = payloadData.holderId,
                    bankName = payloadData.bankName,
                    bankId = payloadData.bankId,
                    branchId = payloadData.branchId,
                    accountNr = payloadData.accountNr,
                    bankAccountType = payloadData.bankAccountType,
                    nationalAccountId = payloadData.nationalAccountId,
                ),
        )
}
