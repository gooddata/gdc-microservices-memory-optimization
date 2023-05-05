/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.handler

import com.gooddata.panther.organizationapi.PathParams
import com.gooddata.panther.organizationapi.QueryParams
import com.gooddata.panther.organizationapi.dto.DeploymentPropertiesDto
import com.gooddata.panther.organizationapi.service.OrganizationService
import com.gooddata.panther.organizationapi.dto.OrganizationDto
import com.gooddata.panther.organizationapi.dto.SynchronizeRequestDto
import com.gooddata.panther.organizationapi.dto.TierOrganizationDto
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.slf4j.MDCContext
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class OrganizationHandler(val organizationService: OrganizationService) {
    fun create(request: ServerRequest): Mono<ServerResponse> =
        request
            .bodyToMono(OrganizationDto::class.java)
            .flatMap {
                mono(MDCContext()) { organizationService.create(it) }
            }
            .flatMap {
                ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(it)
            }

    fun createForTier(request: ServerRequest): Mono<ServerResponse> =
        request
            .bodyToMono(TierOrganizationDto::class.java)
            .flatMap {
                val tierType = request.pathVariable(PathParams.TIER_TYPE)
                mono(MDCContext()) { organizationService.create(it, tierType) }
            }
            .flatMap {
                ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(it)
            }

    fun synchronizeOrganizations(request: ServerRequest): Mono<ServerResponse> =
        request
            .bodyToMono(SynchronizeRequestDto::class.java)
            .flatMap {
                mono(MDCContext()) { organizationService.synchronizeOrganizations(it) }
            }
            .flatMap {
                ServerResponse
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(it)
            }

    fun deleteOrganizationById(request: ServerRequest): Mono<ServerResponse> =
        mono(MDCContext()) {
            organizationService.deleteOrganization(
                request.pathVariable(PathParams.ORGANIZATION_ID),
                DeploymentPropertiesDto(
                    request.queryParam(QueryParams.CLUSTER).get(),
                    request.queryParam(QueryParams.DEPLOYMENT).get()
                )
            )
        }.flatMap { ServerResponse.noContent().build() }
}
