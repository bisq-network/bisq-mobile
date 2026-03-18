package network.bisq.mobile.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import network.bisq.mobile.data.model.User

interface UserRepository {
    val data: Flow<User>

    suspend fun fetch() = data.first()

    suspend fun updateTerms(value: String)

    suspend fun updateStatement(value: String)

    suspend fun update(value: User)

    suspend fun clear()
}
