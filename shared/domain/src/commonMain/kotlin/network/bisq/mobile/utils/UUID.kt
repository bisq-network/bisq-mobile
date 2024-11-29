package network.bisq.mobile.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun createUuid(): String {
    return Uuid.random().toString()
}