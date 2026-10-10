package network.bisq.mobile.domain.usecase.startup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class ResolveStartupDestinationUseCaseTest {
    private val userProfileService: UserProfileServiceFacade = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val useCase = ResolveStartupDestinationUseCase(userProfileService, settingsRepository)

    @Test
    fun `an existing profile goes home regardless of the first launch flag`() =
        runTest {
            coEvery { userProfileService.hasUserProfile() } returns true

            assertEquals(StartupDestination.HOME, useCase())
            coVerify(exactly = 0) { settingsRepository.fetch() }
        }

    @Test
    fun `first launch without a profile goes to onboarding`() =
        runTest {
            coEvery { userProfileService.hasUserProfile() } returns false
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = true)

            assertEquals(StartupDestination.ONBOARDING, useCase())
        }

    @Test
    fun `a later launch without a profile goes to create profile`() =
        runTest {
            coEvery { userProfileService.hasUserProfile() } returns false
            coEvery { settingsRepository.fetch() } returns Settings(firstLaunch = false)

            assertEquals(StartupDestination.CREATE_PROFILE, useCase())
        }
}
