package network.bisq.mobile.client.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType

class ApiRequestService(private val httpClient: HttpClient, host: String) {
    private val log = Logger.withTag(this::class.simpleName ?: "ApiRequestService")
    private var baseUrl = "http://$host:8082/api/v1/"

    suspend fun get(path: String): String {
        return httpClient.get(baseUrl + path).bodyAsText()
    }

    suspend fun post(path: String, requestBody: Any): String {
        return httpClient.post(baseUrl + path) {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(requestBody)
        }.body()
    }
}