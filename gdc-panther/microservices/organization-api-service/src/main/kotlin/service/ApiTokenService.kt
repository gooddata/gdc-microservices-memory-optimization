/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.service

import com.github.marcoferrer.krotoplus.coroutines.withCoroutineContext
import com.gooddata.panther.organizationapi.grpc.TIGER_ORGANIZATION_ID_METADATA_KEY
import com.gooddata.panther.organizationapi.grpc.TIGER_USER_ID_METADATA_KEY
import com.gooddata.panther.organizationapi.service.MetadataStoreServiceCoroutineGrpc.MetadataStoreServiceCoroutineStub
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import java.time.Instant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ApiTokenService(
    private val metadataStoreStub: MetadataStoreServiceCoroutineStub,
    @Value("\${temporary-token.validitySeconds}") private val tokenValiditySeconds: Long
) {

    /**
     * Create internal API token for `userId` of `organizationId`
     *
     * @param organizationId ID of the organization that the user belongs to
     * @param userId ID of the user
     * @param tokenId ID of the token
     * @param ttl token expiration in seconds, 0 disables expiration
     * @return generated ApiToken structure
     */
    suspend fun createInternalApiToken(
        organizationId: String,
        userId: String,
        tokenId: String
    ): ApiToken {
        val headers = Metadata()
        headers.put(TIGER_ORGANIZATION_ID_METADATA_KEY, organizationId)
        headers.put(TIGER_USER_ID_METADATA_KEY, userId)

        // TODO consider client catching (same as in tiger -> Wrap client side call of gRPC service for exceptions)
        return metadataStoreStub
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
            .withCoroutineContext().createInternalApiToken(
                ApiTokenRequest {
                    this.organizationId = organizationId
                    this.userId = userId
                    this.tokenId = tokenId
                    validTo {
                        seconds = Instant.now().plusSeconds(tokenValiditySeconds).epochSecond
                    }
                }
            )
    }
}
