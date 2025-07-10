package network.bisq.mobile.android.node.service.network

class KmpTorException : Exception {
    constructor(cause: Throwable) : super(cause)
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}