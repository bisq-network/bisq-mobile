package network.bisq.mobile.client.common.domain.access.session

import network.bisq.mobile.client.common.domain.access.session.dto.SessionRequestDto
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.utils.Logging

class SessionService(
    private val apiGateway: SessionApiGateway,
) : ServiceFacade(),
    Logging {
    suspend fun requestSession(
        clientId: String,
        clientSecret: String,
    ): Result<SessionResponse> {
        val sessionRequestDto =
            SessionRequestDto(clientId, clientSecret)

        val result = apiGateway.requestSession(sessionRequestDto)

        return if (result.isSuccess) {
            val dto = result.getOrThrow()
            Result.success(
                SessionResponse(
                    dto.sessionId,
                    dto.expiresAt,
                ),
            )
        } else {
            Result.failure(result.exceptionOrNull()!!)
        }
    }
}
