package network.bisq.mobile.domain.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

private const val FILE_EXTENSION = ".preferences_pb"

fun <T> createDataStore(
    name: String,
    baseDirPath: String,
    serializer: OkioSerializer<T>,
    corruptionHandler: ReplaceFileCorruptionHandler<T>,
): DataStore<T> {
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            producePath = { baseDirPath.toPath().resolve("${name}$FILE_EXTENSION") },
            serializer = serializer,
        ),
        corruptionHandler = corruptionHandler,
    )
}
