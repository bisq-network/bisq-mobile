package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.pager.PagerState
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.composeModels.PagerViewItem
import network.bisq.mobile.presentation.ui.navigation.Routes

open class OnBoardingPresenter(
    mainPresenter: MainPresenter,
    private val settingsRepository: SettingsRepository,
    private val userProfileService: UserProfileServiceFacade,
) : BasePresenter(mainPresenter), IOnboardingPresenter {

    override val indexesToShow = listOf(0)

    // TODO: Ideally slide content for xClients should only be here.
    // Android node content (along with resources), should be moved to `androidNode`
    // Then remove `indexesToShow`
    override val onBoardingData = listOf(
        PagerViewItem(
            title = "Introducing Bisq Easy",
            image = Res.drawable.img_bisq_Easy,
            desc = "Getting your first Bitcoin privately has never been easier"
        ),
        PagerViewItem(
            title = "Bisp p2p in mobile",
            image = Res.drawable.img_learn_and_discover,
            desc = "All the awesomeness of Bisq desktop now in your mobile. Android only. (TODO: Show apt image)"
        ),
        PagerViewItem(
            title = "Coming soon",
            image = Res.drawable.img_fiat_btc,
            desc = "Choose how to trade: Bisq MuSig, Lightning, Submarine Swaps,..."
        )
    )

    override fun onNextButtonClick(coroutineScope: CoroutineScope, pagerState: PagerState) {
        coroutineScope.launch {

            settingsRepository.fetch()
            val settings: Settings? = settingsRepository.data.value

            val hasProfile: Boolean = userProfileService.hasUserProfile()

            if (pagerState.currentPage == indexesToShow.lastIndex) {

                // to ensure event propagation, probably need to change settings equals definition to avoid this
                val updatedSettings = Settings().apply {
                    bisqApiUrl = settings?.bisqApiUrl ?: ""
                    firstLaunch = false
                }

                settingsRepository.update(updatedSettings)

                val remoteBisqUrl = settings?.bisqApiUrl ?: ""
                doCustomNavigationLogic(remoteBisqUrl.isNotEmpty(), hasProfile)

            } else {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    protected fun navigateToCreateProfile() {
        navigateTo(Routes.CreateProfile)
    }

    protected fun navigateToTrustedNodeSetup() {
        navigateTo(Routes.TrustedNodeSetup)
    }

    open fun doCustomNavigationLogic(isBisqUrlSet: Boolean, hasProfile: Boolean) {
        if (isBisqUrlSet) {
            navigateToCreateProfile()
        } else {
            navigateToTrustedNodeSetup()
        }
    }

}
