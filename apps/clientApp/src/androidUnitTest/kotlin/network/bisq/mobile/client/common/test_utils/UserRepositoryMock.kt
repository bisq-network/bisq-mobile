package network.bisq.mobile.client.common.test_utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.UserRepository

/**
 * Mock implementation of UserRepository for testing.
 */
class UserRepositoryMock : UserRepository {
    private val _data = MutableStateFlow(User())
    override val data: StateFlow<User> = _data

    override suspend fun updateTerms(value: String) {
        _data.value = _data.value.copy(tradeTerms = value)
    }

    override suspend fun updateStatement(value: String) {
        _data.value = _data.value.copy(statement = value)
    }

    override suspend fun update(value: User) {
        _data.value = value
    }

    override suspend fun clear() {
        _data.value = User()
    }
}
