package network.bisq.mobile.domain.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val IODispatcher: CoroutineDispatcher = Dispatchers.IO