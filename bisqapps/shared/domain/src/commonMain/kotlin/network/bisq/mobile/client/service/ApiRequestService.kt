package network.bisq.mobile.client.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import network.bisq.mobile.utils.Logging

class ApiRequestService(val httpClient: HttpClient, host: String): Logging {
    private var baseUrl = "http://$host:8082/api/v1/"

    init{
        log.i { "API base URL = $baseUrl" }
    }

    fun endpoint(path: String) = baseUrl + path

    suspend inline fun <reified T> get(path: String): T {
        return httpClient.get(endpoint(path)).body()
    }

    suspend inline fun <reified T> get(path: String, paramName: String, paramValue: String): T {
        return httpClient.get(endpoint(path)) {
            parameter(paramName, paramValue)
        }.body()
    }

    suspend inline fun <reified T> post(path: String, requestBody: Any): T {
        return httpClient.post(endpoint(path)) {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(requestBody)
        }.body()
    }
}