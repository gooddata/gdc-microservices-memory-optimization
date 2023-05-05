/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.route

import com.gooddata.panther.organizationapi.handler.OrganizationHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router

@Configuration
class OrganizationStatusRoutes {
    companion object {
        private const val ORG_API_STATUS_URI = "/api/v1/panther/organizations/status"
    }

    @Bean
    fun organizationStatusRouter(statusHandler: OrganizationHandler) = router {
        accept(MediaType.APPLICATION_JSON).nest {
            GET(ORG_API_STATUS_URI) {
                ServerResponse.noContent().build()
            }
        }
    }
}
