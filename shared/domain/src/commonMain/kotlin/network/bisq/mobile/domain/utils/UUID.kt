package network.bisq.mobile.domain.utils

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun createUuid(): String {
    return Uuid.random().toString()
}