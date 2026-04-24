package network.bisq.mobile.presentation.common.di

import network.bisq.mobile.presentation.create_payment_account.CreatePaymentAccountPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto.SelectCryptoPaymentMethodPresenter
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat.SelectFiatPaymentMethodPresenter
import org.koin.dsl.module

val paymentsPresentationModule =
    module {
        factory { CreatePaymentAccountPresenter(get()) }

        factory { SelectCryptoPaymentMethodPresenter(get(), get()) }
        factory { SelectFiatPaymentMethodPresenter(get(), get()) }

        factory { ZelleFormPresenter(get()) }
        factory { MoneroFormPresenter(get()) }
    }
