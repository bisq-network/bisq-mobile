package network.bisq.mobile.client.common.domain.access.session

import network.bisq.mobile.client.common.domain.access.session.dto.SessionRequestDto
import network.bisq.mobile.client.common.domain.access.session.dto.SessionResponseDto
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.domain.utils.Logging

class SessionApiGateway(
    private val httpClientService: HttpClientService,
) : Logging {
    val basePath = "access"

    suspend fun requestSession(
        request: SessionRequestDto,
    ): Result<SessionResponseDto> {
        val path = "$basePath/session"
        return httpClientService.post(path, request)
    }
}
