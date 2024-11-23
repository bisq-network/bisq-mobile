package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.domain.bootstrap.NodeApplicationBootstrapFacade
import network.bisq.mobile.android.node.domain.offerbook.NodeOfferbookServiceFacade
import network.bisq.mobile.android.node.domain.user_profile.NodeUserProfileServiceFacade
import network.bisq.mobile.android.node.presentation.NodeMainPresenter
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.offerbook.OfferbookServiceFacade
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeModule = module {
    single<AndroidMemoryReportService> {
        AndroidMemoryReportService(androidContext())
    }

    single { AndroidApplicationService.Supplier() }

    single<ApplicationBootstrapFacade> { NodeApplicationBootstrapFacade(get()) }

    single<UserProfileServiceFacade> { NodeUserProfileServiceFacade(get()) }

    single<OfferbookServiceFacade> { NodeOfferbookServiceFacade(get()) }
    // this line showcases both, the possibility to change behaviour of the app by changing one definition
    // and binding the same obj to 2 different abstractions
    single<MainPresenter> { NodeMainPresenter(get(), get(),  get(), get()) } bind AppPresenter::class
}