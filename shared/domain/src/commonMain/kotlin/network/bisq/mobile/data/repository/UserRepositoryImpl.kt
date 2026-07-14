package network.bisq.mobile.data.repository

import androidx.datastore.core.DataStore
import network.bisq.mobile.data.model.User
import network.bisq.mobile.domain.repository.UserRepository

class UserRepositoryImpl(
    userStore: DataStore<User>,
) : DataStoreRepository<User>(userStore),
    UserRepository {
    override fun createDefault() = User()

    override suspend fun updateTerms(value: String) = set { it.copy(tradeTerms = value) }

    override suspend fun updateStatement(value: String) = set { it.copy(statement = value) }

    override suspend fun update(value: User) = set { value }
}
