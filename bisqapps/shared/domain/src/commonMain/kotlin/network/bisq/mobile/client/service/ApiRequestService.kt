package network.bisq.mobile.client.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType

class ApiRequestService(val httpClient: HttpClient, host: String) {
    private val log = Logger.withTag(this::class.simpleName ?: "ApiRequestService")

    private var baseUrl = "http://$host:8082/api/v1/"

    fun endpoint(path: String) = baseUrl + path

    suspend inline fun <reified T> get(path: String): T {
        return httpClient.get(endpoint(path)).body()
    }

    suspend inline fun <reified T> post(path: String, requestBody: Any): T {
        return httpClient.post(endpoint(path)) {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(requestBody)
        }.body()
    }
}