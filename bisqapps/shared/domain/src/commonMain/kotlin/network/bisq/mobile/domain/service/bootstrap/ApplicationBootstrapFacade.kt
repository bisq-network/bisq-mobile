package network.bisq.mobile.domain.data.repository.main.bootstrap

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.LifeCycleAware

abstract class ApplicationBootstrapFacade: LifeCycleAware {
    private val _state = MutableStateFlow("")
    val state: StateFlow<String> = _state
    fun setState(value: String) {
        _state.value = value
    }

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    fun setProgress(value: Float) {
        _progress.value = value
    }
}