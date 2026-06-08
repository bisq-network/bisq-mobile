package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.national_bank

import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.national_bank.CreateNationalBankAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank.BankAccountCreatePayloadData
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.bank.BankAccountFormPresenter
import network.bisq.mobile.presentation.main.MainPresenter

open class NationalBankFormPresenter(
    paymentAccountsServiceFacade: PaymentAccountsServiceFacade,
    mainPresenter: MainPresenter,
) : BankAccountFormPresenter<CreateNationalBankAccount>(paymentAccountsServiceFacade, mainPresenter) {
    override fun createAccount(
        accountName: String,
        payloadData: BankAccountCreatePayloadData,
    ): CreateNationalBankAccount =
        CreateNationalBankAccount(
            accountName = accountName,
            accountPayload =
                CreateNationalBankAccountPayload(
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
