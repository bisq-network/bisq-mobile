package network.bisq.mobile.android.node.di

import bisq.security.SecurityService
import bisq.user.identity.UserIdentityService
import network.bisq.mobile.android.node.domain.data.repository.NodeGreetingRepository
import network.bisq.mobile.android.node.presentation.MainNodePresenter
import network.bisq.mobile.android.node.service.AndroidApplicationService
import network.bisq.mobile.android.node.service.AndroidMemoryReportService
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeModule = module {
    // this one is for example properties, will be eliminated soon
    single<NodeGreetingRepository> { NodeGreetingRepository() }
    // this line showcases both, the posibility to change behaviour of the app by changing one definiton
    // and binding the same obj to 2 different abstractions
    single<MainPresenter> { MainNodePresenter(get()) } bind AppPresenter::class

    // Services
//    TODO might not work because of the jars dependencies, needs more work
//    single <AndroidMemoryReportService> {
//        val context = androidContext()
//        AndroidMemoryReportService(context)
//    }
//    single <AndroidApplicationService> {
//        val filesDirsPath = androidContext().filesDir.toPath()
//        val androidMemoryService: AndroidMemoryReportService = get()
//        AndroidApplicationService(androidMemoryService, filesDirsPath)
//    }
//    single <UserIdentityService> {
//        val applicationService: AndroidApplicationService = get()
//        applicationService.userService.userIdentityService
//    }
//    single <SecurityService> {
//        val applicationService: AndroidApplicationService = get()
//        applicationService.securityService
//    }
}