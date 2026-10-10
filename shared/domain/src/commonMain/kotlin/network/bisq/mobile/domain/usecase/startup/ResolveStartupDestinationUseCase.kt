package network.bisq.mobile.domain.usecase.startup

import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository

/** Where a user who has accepted the terms continues: the one decision the startup screens share. */
enum class StartupDestination {
    /** A profile exists, on the node or on the trusted node Connect is paired with. */
    HOME,

    /** First start on this device without a profile: the onboarding carousel. */
    ONBOARDING,

    /** This device has started before but there is no profile, typically after a reset. */
    CREATE_PROFILE,
}

/**
 * Decides where to send the user once the terms are accepted. The splash and the agreement screen both
 * need this: core asks for the terms again whenever their version changes, so an accepted agreement no
 * longer means a fresh install, and the user may well have profiles already.
 */
class ResolveStartupDestinationUseCase(
    private val userProfileService: UserProfileServiceFacade,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(): StartupDestination =
        when {
            userProfileService.hasUserProfile() -> StartupDestination.HOME
            settingsRepository.fetch().firstLaunch -> StartupDestination.ONBOARDING
            else -> StartupDestination.CREATE_PROFILE
        }
}
