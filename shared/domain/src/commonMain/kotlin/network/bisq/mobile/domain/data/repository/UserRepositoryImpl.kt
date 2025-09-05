package network.bisq.mobile.domain.data.repository

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.datetime.Clock
import kotlinx.io.IOException
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.utils.Logging

class UserRepositoryImpl(
    private val userStore: DataStore<User>,
) : UserRepository, Logging {

    override val data: Flow<User>
        get() =
            userStore.data.catch { exception ->
                if (exception is IOException) {
                    log.e("Error reading User datastore", exception)
                    emit(User())
                } else {
                    throw exception
                }
            }

    override suspend fun updateLastActivity() {
        userStore.updateData {
            it.copy(lastActivity = Clock.System.now().toEpochMilliseconds())
        }
    }

    override suspend fun updateTerms(value: String) {
        userStore.updateData {
            it.copy(tradeTerms = value)
        }
    }

    override suspend fun updateStatement(value: String) {
        userStore.updateData {
            it.copy(statement = value)
        }
    }

    override suspend fun update(value: User) {
        userStore.updateData {
            value
        }
    }

    override suspend fun clear() {
        userStore.updateData { User() }
    }


}