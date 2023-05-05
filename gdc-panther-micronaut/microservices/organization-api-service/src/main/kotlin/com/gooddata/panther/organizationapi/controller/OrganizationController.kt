/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.controller

import com.gooddata.panther.organizationapi.PathParams
import com.gooddata.panther.organizationapi.QueryParams
import com.gooddata.panther.organizationapi.dto.OrganizationDto
import com.gooddata.panther.organizationapi.dto.SynchronizeRequestDto
import com.gooddata.panther.organizationapi.dto.TierOrganizationDto
import com.gooddata.panther.organizationapi.service.OrganizationService
import com.gooddata.panther.organizationapi.utils.Validator
import com.gooddata.panther.organizationapi.utils.logUser
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import mu.KotlinLogging

@Controller(OrganizationController.PANTHER_ORGANIZATIONS_URL)
class OrganizationController(
    private val organizationService: OrganizationService,
) {

    @Post("/synchronize")
    suspend fun synchronizeOrganizations(
        request: HttpRequest<*>,
        @Body synchronizationRequest: SynchronizeRequestDto,
    ): Map<String, List<String>> {
        logger.logUser("synchronize_organizations", request.headers)
        return organizationService.synchronizeOrganizations(synchronizationRequest)
    }

    @Post("/tier/{${PathParams.TIER_TYPE}}")
    suspend fun createTierOrganization(
        request: HttpRequest<*>,
        @Body organizationRequest: TierOrganizationDto,
        @PathVariable("tierType") tierType: String,
    ): OrganizationDto {
        logger.logUser("create_organization", request.headers)
        return organizationService.create(organizationRequest, tierType)
    }

    @Post
    suspend fun createOrganization(
        request: HttpRequest<*>,
        @Body organizationRequest: OrganizationDto,
    ): OrganizationDto {
        logger.logUser("create_organization", request.headers)
        return organizationService.create(organizationRequest)
    }

    @Delete("/{${PathParams.ORGANIZATION_ID}}")
    suspend fun deleteOrganization(
        request: HttpRequest<*>,
        @PathVariable organizationId: String,
        @QueryValue(QueryParams.CLUSTER) cluster: String,
        @QueryValue(QueryParams.DEPLOYMENT) deployment: String,
    ) {
        logger.logUser("delete_organization", request.headers)
        Validator.validateQueryParams(request, logger, QueryParams.CLUSTER, QueryParams.DEPLOYMENT)
        organizationService.deleteOrganization(organizationId, cluster, deployment)
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        const val PANTHER_ORGANIZATIONS_URL = "/api/v1/panther/organizations"
    }
}
