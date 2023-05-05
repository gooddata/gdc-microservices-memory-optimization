/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.route

import com.gooddata.panther.organizationapi.PathParams
import com.gooddata.panther.organizationapi.QueryParams
import com.gooddata.panther.organizationapi.handler.OrganizationHandler
import com.gooddata.panther.organizationapi.utils.Validator.validateQueryParams
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.server.router

const val PANTHER_ORGANIZATIONS_URL = "/api/v1/panther/organizations"

private val logger = KotlinLogging.logger { }

@Configuration
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OrganizationRoutes {
    @Bean
    fun organizationRouter(organizationHandler: OrganizationHandler) = router {

        (PANTHER_ORGANIZATIONS_URL and accept(APPLICATION_JSON)).nest {
            method(HttpMethod.POST).nest {
                POST("/synchronize", organizationHandler::synchronizeOrganizations)
                POST("/tier/{${PathParams.TIER_TYPE}}", organizationHandler::createForTier)
                POST(organizationHandler::create)
            }
            method(HttpMethod.DELETE).nest {
                DELETE("/{${PathParams.ORGANIZATION_ID}}", organizationHandler::deleteOrganizationById)
                filter { request, handle ->
                    validateQueryParams(request, logger, QueryParams.CLUSTER, QueryParams.DEPLOYMENT)
                    handle(request)
                }
            }
        }
    }
}
