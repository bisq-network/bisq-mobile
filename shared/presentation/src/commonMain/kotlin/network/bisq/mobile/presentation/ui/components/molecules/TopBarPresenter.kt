package network.bisq.mobile.presentation.ui.components.molecules

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter

open class TopBarPresenter(
    private val userRepository: UserRepository,
    mainPresenter: MainPresenter
): BasePresenter(mainPresenter), ITopBarPresenter {

    private val _uniqueAvatar = MutableStateFlow(userRepository.data.value?.uniqueAvatar)
    override val uniqueAvatar: StateFlow<PlatformImage?> get() = _uniqueAvatar

    private fun setUniqueAvatar(value: PlatformImage?) {
        _uniqueAvatar.value = value
    }

    init {
        refresh()
    }

    override fun onViewAttached() {
        super.onViewAttached()
        refresh()
    }

    private fun refresh() {
        backgroundScope.launch {
            val uniqueAvatar = userRepository.fetch()?.uniqueAvatar
//            log.d("Unique avatar fetched: $uniqueAvatar")
            setUniqueAvatar(uniqueAvatar)
        }
    }
}