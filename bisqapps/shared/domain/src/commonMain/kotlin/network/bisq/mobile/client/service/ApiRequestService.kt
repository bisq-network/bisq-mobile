package network.bisq.mobile.client.service

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiRequestService(val baseUrl: String) {
    private val log = Logger.withTag("RequestService")

    suspend fun get(path: String): String {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return client.get(baseUrl + path).bodyAsText()
    }

    suspend fun post(path: String, requestBody: Any): String {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return client.post(baseUrl + path) {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(requestBody)
        }.body()
    }
}