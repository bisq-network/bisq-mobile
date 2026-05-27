package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit.CashDepositAccountDetailPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.CreatePaymentAccountPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.crypto.SelectCryptoPaymentMethodPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.fiat.SelectFiatPaymentMethodPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit.CashDepositFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto.OtherCryptoFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut.RevolutFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise.WiseFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.PaymentAccountReviewPresenter
import network.bisq.mobile.client.payment_accounts.presentation.payment_account_detail.PaymentAccountMusigDetailPresenter
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.PaymentAccountsMusigPresenter
import org.koin.dsl.module

val paymentsAccountModule =
    module {
        factory { PaymentAccountsMusigPresenter(get(), get()) }
        factory { PaymentAccountMusigDetailPresenter(get(), get()) }

        factory { CreatePaymentAccountPresenter(get()) }

        factory { SelectCryptoPaymentMethodPresenter(get(), get()) }
        factory { SelectFiatPaymentMethodPresenter(get(), get()) }

        factory { PaymentAccountReviewPresenter(get(), get()) }
        factory { CashDepositAccountDetailPresenter(get(), get()) }

        factory { CashDepositFormPresenter(get(), get()) }
        factory { ZelleFormPresenter(get()) }
        factory { WiseFormPresenter(get()) }
        factory { RevolutFormPresenter(get()) }
        factory { MoneroFormPresenter(get()) }
        factory { OtherCryptoFormPresenter(get()) }
    }
