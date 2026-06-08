package network.bisq.mobile.client.payment_accounts.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details.BankAccountCountryDetailsCache
import network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details.BankAccountCountryDetailsCacheSerializer
import network.bisq.mobile.client.payment_accounts.data.repository.BankAccountCountryDetailsRepositoryImpl
import network.bisq.mobile.client.payment_accounts.domain.repository.BankAccountCountryDetailsRepository
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.bank.BankAccountDetailPresenter
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit.CashDepositAccountDetailPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.CreatePaymentAccountPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.crypto.SelectCryptoPaymentMethodPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.fiat.SelectFiatPaymentMethodPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer.AchTransferFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit.CashDepositFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.national_bank.NationalBankFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto.OtherCryptoFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut.RevolutFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.same_bank.SameBankFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.sepa.SepaFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise.WiseFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.PaymentAccountReviewPresenter
import network.bisq.mobile.client.payment_accounts.presentation.payment_account_detail.PaymentAccountMusigDetailPresenter
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.PaymentAccountsMusigPresenter
import network.bisq.mobile.data.datastore.createDataStore
import network.bisq.mobile.data.utils.getStorageDir
import org.koin.core.qualifier.named
import org.koin.dsl.module

val paymentsAccountsModule =
    module {
        factory { PaymentAccountsMusigPresenter(get(), get()) }
        factory { PaymentAccountMusigDetailPresenter(get(), get()) }

        factory { CreatePaymentAccountPresenter(get()) }

        factory { SelectCryptoPaymentMethodPresenter(get(), get()) }
        factory { SelectFiatPaymentMethodPresenter(get(), get()) }

        factory { PaymentAccountReviewPresenter(get(), get()) }
        factory { CashDepositAccountDetailPresenter(get(), get()) }
        factory { BankAccountDetailPresenter(get(), get()) }

        factory { AchTransferFormPresenter(get()) }
        factory { CashDepositFormPresenter(get(), get()) }
        factory { ZelleFormPresenter(get()) }
        factory { WiseFormPresenter(get()) }
        factory { RevolutFormPresenter(get()) }
        factory { SameBankFormPresenter(get(), get()) }
        factory { NationalBankFormPresenter(get(), get()) }
        factory { SepaFormPresenter(get()) }
        factory { MoneroFormPresenter(get()) }
        factory { OtherCryptoFormPresenter(get()) }

        single<DataStore<BankAccountCountryDetailsCache>>(named("BankAccountCountryDetailsCache")) {
            createDataStore(
                "BankAccountCountryDetailsCache",
                getStorageDir(),
                BankAccountCountryDetailsCacheSerializer,
                ReplaceFileCorruptionHandler { BankAccountCountryDetailsCache() },
            )
        }

        single<BankAccountCountryDetailsRepository> {
            BankAccountCountryDetailsRepositoryImpl(get(named("BankAccountCountryDetailsCache")))
        }
    }
