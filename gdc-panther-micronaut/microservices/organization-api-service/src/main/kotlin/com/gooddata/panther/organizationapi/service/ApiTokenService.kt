/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.service

import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import com.gooddata.panther.common.exception.BadRequestException
import com.gooddata.panther.organizationapi.XAUTHHttpHeaders
import com.gooddata.panther.organizationapi.dto.ApiTokenRequestDto
import com.gooddata.panther.organizationapi.dto.ApiTokenResponseDto
import com.gooddata.panther.organizationapi.grpc.ChannelBuilder
import com.gooddata.panther.organizationapi.grpc.GrpcProperties
import com.gooddata.panther.organizationapi.grpc.TIGER_ORGANIZATION_ID_METADATA_KEY
import com.gooddata.panther.organizationapi.grpc.TIGER_USER_ID_METADATA_KEY
import com.gooddata.panther.organizationapi.service.MetadataStoreServiceCoroutineGrpc.MetadataStoreServiceCoroutineStub
import io.grpc.ClientInterceptor
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton
import java.time.Instant
import java.util.regex.Pattern

@Singleton
class ApiTokenService(
    channelBuilder: ChannelBuilder,
    grpcProperties: GrpcProperties,
    clientInterceptors: List<ClientInterceptor>,
) {
    private val metadataStoreStub: MetadataStoreServiceCoroutineStub =
        MetadataStoreServiceCoroutineStub.newStub(channelBuilder.buildChannel(grpcProperties, clientInterceptors))

    suspend fun createApiToken(request: HttpRequest<*>, tokenRequest: ApiTokenRequestDto): ApiTokenResponseDto {
        validateRequest(request, tokenRequest)

        return Metadata().apply {
            put(TIGER_ORGANIZATION_ID_METADATA_KEY, tokenRequest.organizationId)
            put(TIGER_USER_ID_METADATA_KEY, tokenRequest.userId)
        }.let { requestMetadata -> sendTokenRequest(requestMetadata, tokenRequest) }
    }

    private suspend fun sendTokenRequest(headers: Metadata, tokenRequest: ApiTokenRequestDto) =
        metadataStoreStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
            .withCoroutineContext()
            .createInternalApiToken(
                ApiTokenRequest {
                    this.organizationId = tokenRequest.organizationId
                    this.userId = tokenRequest.userId
                    this.tokenId = tokenRequest.tokenId
                    validTo { seconds = Instant.now().plusSeconds(TOKEN_VALIDITY_SECONDS).epochSecond }
                }
            ).let { ApiTokenResponseDto(organizationId = it.organizationId, token = it.token) }

    companion object {

        private const val TOKEN_VALIDITY_SECONDS = 60L

        private const val JIRA_TICKET_REGEX = "[A-Z0-9]{2,}-[0-9]+"

        private fun validateRequest(
            request: HttpRequest<*>,
            apiTokenRequestDto: ApiTokenRequestDto,
        ): ApiTokenRequestDto = apiTokenRequestDto.apply {
            if (isExternalCall(request) && !containsValidJiraTicketId(apiTokenRequestDto)) {
                throw BadRequestException("Request doest not contain valid Jira ticket id")
            }
        }

        private fun isExternalCall(request: HttpRequest<*>) =
            request.headers.get(XAUTHHttpHeaders.X_AUTH_REQUEST_EMAIL) != null

        private fun containsValidJiraTicketId(apiTokenRequestDto: ApiTokenRequestDto) =
            Pattern.matches(JIRA_TICKET_REGEX, apiTokenRequestDto.ticketId)
    }
}
