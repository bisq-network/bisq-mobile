package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.create_payment_account.CreatePaymentAccountPresenter
import network.bisq.mobile.client.create_payment_account.account_review.PaymentAccountReviewPresenter
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.other_crypto.OtherCryptoFormPresenter
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.wise.WiseFormPresenter
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.client.create_payment_account.select_payment_method.crypto.SelectCryptoPaymentMethodPresenter
import network.bisq.mobile.client.create_payment_account.select_payment_method.fiat.SelectFiatPaymentMethodPresenter
import network.bisq.mobile.client.settings.payment_accounts_musig.PaymentAccountsMusigPresenter
import network.bisq.mobile.client.settings.payment_accounts_musig.detail.PaymentAccountMusigDetailPresenter
import org.koin.dsl.module

val paymentsAccountModule =
    module {
        factory { PaymentAccountsMusigPresenter(get(), get()) }
        factory { PaymentAccountMusigDetailPresenter(get(), get()) }

        factory { CreatePaymentAccountPresenter(get()) }

        factory { SelectCryptoPaymentMethodPresenter(get(), get()) }
        factory { SelectFiatPaymentMethodPresenter(get(), get()) }

        factory { PaymentAccountReviewPresenter(get(), get()) }

        factory { ZelleFormPresenter(get()) }
        factory { WiseFormPresenter(get()) }
        factory { MoneroFormPresenter(get()) }
        factory { OtherCryptoFormPresenter(get()) }
    }
