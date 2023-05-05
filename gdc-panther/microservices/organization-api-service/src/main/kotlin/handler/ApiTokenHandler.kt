/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.handler

import com.gooddata.panther.common.exception.BadRequestException
import com.gooddata.panther.organizationapi.XAUTHHttpHeaders.X_AUTH_REQUEST_EMAIL
import com.gooddata.panther.organizationapi.dto.ApiTokenRequestDto
import com.gooddata.panther.organizationapi.dto.ApiTokenResponseDto
import com.gooddata.panther.organizationapi.service.ApiTokenService
import java.util.regex.Pattern
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class ApiTokenHandler(val apiTokenService: ApiTokenService) {

    companion object {
        private const val JIRA_TICKET_REGEX = "[A-Z0-9]{2,}-[0-9]+"
    }

    fun createApiToken(request: ServerRequest): Mono<ServerResponse> =
        request
            .bodyToMono(ApiTokenRequestDto::class.java)
            .map { validateRequest(request, it) }
            .flatMap {
                mono(MDCContext()) {
                    apiTokenService.createInternalApiToken(
                        it.organizationId,
                        it.userId,
                        it.tokenId
                    )
                }
            }
            .flatMap {
                ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(ApiTokenResponseDto(it.organizationId, it.token))
            }

    private fun validateRequest(request: ServerRequest, apiTokenRequestDto: ApiTokenRequestDto): ApiTokenRequestDto {
        if (isExternalCall(request) && !containsValidJiraTicketId(apiTokenRequestDto)) {
            throw BadRequestException("Request doest not contain valid Jira ticket id")
        }
        return apiTokenRequestDto
    }

    private fun isExternalCall(request: ServerRequest) = request.headers().firstHeader(X_AUTH_REQUEST_EMAIL) != null

    private fun containsValidJiraTicketId(apiTokenRequestDto: ApiTokenRequestDto) =
        Pattern.matches(JIRA_TICKET_REGEX, apiTokenRequestDto.ticketId)
}
