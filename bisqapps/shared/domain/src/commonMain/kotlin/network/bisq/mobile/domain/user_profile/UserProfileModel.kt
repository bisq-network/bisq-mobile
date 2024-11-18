package network.bisq.mobile.domain.user_profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

open class UserProfileModel {
    private val _nickName = MutableStateFlow("")
    val nickName: StateFlow<String> get() = _nickName
    fun setNickName(value: String) {
        _nickName.value = value
    }

    private val _id = MutableStateFlow("")
    val id: StateFlow<String> get() = _id
    fun setId(value: String) {
        _id.value = value
    }

    private val _nym = MutableStateFlow("")
    val nym: StateFlow<String> get() = _nym
    fun setNym(value: String) {
        _nym.value = value
    }

    private val _generateKeyPairInProgress = MutableStateFlow(false)
    val generateKeyPairInProgress: StateFlow<Boolean> get() = _generateKeyPairInProgress
    fun setGenerateKeyPairInProgress(value: Boolean) {
        _generateKeyPairInProgress.value = value
    }

    private val _createAndPublishInProgress = MutableStateFlow(false)
    val createAndPublishInProgress: StateFlow<Boolean> get() = _createAndPublishInProgress
    fun setCreateAndPublishInProgress(value: Boolean) {
        _createAndPublishInProgress.value = value
    }

    lateinit var pubKeyHash: ByteArray
}