package network.bisq.mobile.domain.data.datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioSerializer
import androidx.datastore.core.okio.OkioStorage
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

private const val FILE_EXTENSION = ".preferences_pb"

fun <T> createDataStore(
    name: String,
    baseDirPath: String,
    serializer: OkioSerializer<T>,
    corruptionHandler: ReplaceFileCorruptionHandler<T>,
    migrations: List<DataMigration<T>> = emptyList(),
): DataStore<T> {
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            producePath = { baseDirPath.toPath().resolve("${name}$FILE_EXTENSION") },
            serializer = serializer,
        ),
        corruptionHandler = corruptionHandler,
        migrations = migrations,
    )
}

val dataStoreJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}