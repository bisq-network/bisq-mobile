package network.bisq.mobile.presentation.settings.user_profile

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.TimeUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute.CreateProfile
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.concurrent.Volatile

class UserProfilePresenter(
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    mainPresenter: MainPresenter,
) : BasePresenter(mainPresenter),
    IUserProfilePresenter {
    companion object Companion {
        // We use 120.dp here, so we want the max size with 300 px for good resolution
        const val ICON_SIZE = 300

        /**
         * Get localized "N/A" value
         */
        fun getLocalizedNA(): String = "data.na".i18n()
    }

    @Volatile
    private var profileIdCollectJob: Job? = null

    @Volatile
    private var profileCollectJob: Job? = null

    @Volatile
    private var profilesCollectJob: Job? = null

    @Volatile
    private var tickerJob: Job? = null

    private val _uiState = MutableStateFlow(UserProfileUiState())
    override val uiState = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        _uiState.update { it.copy(isLoadingData = true) }
        launchJobs()
    }

    override fun onViewUnattaching() {
        super.onViewUnattaching()
        cancelJobs()
    }

    override suspend fun getUserProfileIcon(userProfile: UserProfileVO): PlatformImage = userProfileServiceFacade.getUserProfileIcon(userProfile, ICON_SIZE)

    override fun onAction(action: UserProfileUiAction) {
        when (action) {
            is UserProfileUiAction.OnStatementChange -> {
                _uiState.value.selectedUserProfile?.let {
                    _uiState.update { current ->
                        current.copy(
                            statementDraft = action.value,
                        )
                    }
                }
            }

            is UserProfileUiAction.OnTermsChange -> {
                _uiState.value.selectedUserProfile?.let {
                    _uiState.update { current ->
                        current.copy(
                            termsDraft = action.value,
                        )
                    }
                }
            }

            is UserProfileUiAction.OnSavePress -> onSavePress()

            is UserProfileUiAction.OnCreateProfilePress -> {
                navigateTo(CreateProfile(false))
            }

            is UserProfileUiAction.OnDeletePress -> {
                _uiState.update {
                    it.copy(showDeleteConfirmationForProfile = it.selectedUserProfile)
                }
            }

            is UserProfileUiAction.OnDeleteConfirm -> onDeleteConfirm()

            is UserProfileUiAction.OnDeleteConfirmationDismiss -> {
                _uiState.update {
                    it.copy(showDeleteConfirmationForProfile = null)
                }
            }

            is UserProfileUiAction.OnDeleteError -> {
                _uiState.update { it.copy(showDeleteErrorDialog = true) }
            }

            is UserProfileUiAction.OnDeleteErrorDialogDismiss -> {
                _uiState.update { it.copy(showDeleteErrorDialog = false) }
            }

            is UserProfileUiAction.OnUserProfileSelect -> onUserProfileSelect(action.profile.id)
        }
    }

    private fun onSavePress() {
        val uiStateSnapshot = _uiState.value
        val selectedProfile = uiStateSnapshot.selectedUserProfile ?: return
        disableInteractive()
        _uiState.update { it.copy(isBusyWithAction = true) }
        presenterScope.launch {
            try {
                val na = getLocalizedNA()
                val safeStatement = uiStateSnapshot.statementDraft.takeUnless { it == na } ?: ""
                val safeTerms = uiStateSnapshot.termsDraft.takeUnless { it == na } ?: ""
                val result =
                    userProfileServiceFacade.updateAndPublishUserProfile(
                        selectedProfile.id,
                        safeStatement,
                        safeTerms,
                    )
                if (result.isSuccess) {
                    showSnackbar("mobile.settings.userProfile.saveSuccess".i18n(), isError = false)
                } else {
                    showSnackbar("mobile.settings.userProfile.saveFailure".i18n(), isError = true)
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to save user profile settings" }
            } finally {
                _uiState.update { it.copy(isBusyWithAction = false) }
                enableInteractive()
            }
        }
    }

    private fun onDeleteConfirm() {
        val profileId = _uiState.value.showDeleteConfirmationForProfile?.id ?: return
        disableInteractive()
        _uiState.update { it.copy(isBusyWithAction = true, showDeleteConfirmationForProfile = null) }
        presenterScope.launch {
            userProfileServiceFacade
                .deleteUserProfile(profileId)
                .onSuccess {
                    showSnackbar("mobile.settings.userProfile.deleteSuccess".i18n(), isError = false)
                }.onFailure { e ->
                    log.e(e) { "Failed to delete user profile" }
                    onAction(UserProfileUiAction.OnDeleteError)
                }
            _uiState.update { it.copy(isBusyWithAction = false) }
            enableInteractive()
        }
    }

    private fun onUserProfileSelect(profileId: String) {
        disableInteractive()
        _uiState.update { it.copy(isBusyWithAction = true) }
        presenterScope.launch {
            userProfileServiceFacade
                .selectUserProfile(profileId)
                .onSuccess {
                    showSnackbar("mobile.settings.userProfile.selectSuccess".i18n(), isError = false)
                    _uiState.update { it.copy(isBusyWithAction = false) }
                    enableInteractive()
                }.onFailure { e ->
                    log.e(e) { "Failed to change user profile" }
                    showSnackbar("mobile.settings.userProfile.selectFailure".i18n(), isError = true)
                    _uiState.update { it.copy(isBusyWithAction = false) }
                    enableInteractive()
                }
        }
    }

    private fun launchJobs() {
        profileIdCollectJob?.cancel()
        profileIdCollectJob =
            presenterScope.launch {
                userProfileServiceFacade.selectedUserProfile.distinctUntilChangedBy { it?.id }.collect { profile ->
                    profile?.let { profile ->
                        val profileAge =
                            reputationServiceFacade
                                .getProfileAge(profile.id)
                                .getOrNull()
                                ?.let { age -> DateUtils.formatProfileAge(age) } ?: getLocalizedNA()
                        val reputation =
                            reputationServiceFacade
                                .getReputation(profile.id)
                                .getOrNull()
                                ?.totalScore
                                ?.toString()
                                ?: getLocalizedNA()
                        _uiState.update { current ->
                            current.copy(
                                selectedUserProfile = profile,
                                profileAge = profileAge,
                                reputation = reputation,
                                statementDraft = profile.statement,
                                termsDraft = profile.terms,
                                isLoadingData = false,
                            )
                        }
                    }
                }
            }
        profileCollectJob?.cancel()
        profileCollectJob =
            presenterScope.launch {
                // this job's difference from previous one is that this one is
                // to reflect the changes in other fields of userProfile
                userProfileServiceFacade.selectedUserProfile.collect { profile ->
                    _uiState.update {
                        it.copy(selectedUserProfile = profile)
                    }
                }
            }
        tickerJob?.cancel()
        tickerJob =
            presenterScope.launch {
                TimeUtils.tickerFlow(1_000L).onStart { emit(Unit) }.collect {
                    _uiState.update {
                        it.copy(
                            lastUserActivity =
                                it.selectedUserProfile?.publishDate?.let { ts -> DateUtils.lastSeen(ts) }
                                    ?: getLocalizedNA(),
                        )
                    }
                }
            }
        profilesCollectJob?.cancel()
        profilesCollectJob =
            presenterScope.launch {
                userProfileServiceFacade.userProfiles.collect { profiles ->
                    _uiState.update {
                        it.copy(userProfiles = profiles)
                    }
                }
            }
    }

    private fun cancelJobs() {
        profileIdCollectJob?.cancel()
        profileCollectJob?.cancel()
        profilesCollectJob?.cancel()
        tickerJob?.cancel()
    }
}
