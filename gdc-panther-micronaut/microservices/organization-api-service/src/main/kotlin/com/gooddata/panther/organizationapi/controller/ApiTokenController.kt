/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.controller

import com.gooddata.panther.organizationapi.dto.ApiTokenRequestDto
import com.gooddata.panther.organizationapi.dto.ApiTokenResponseDto
import com.gooddata.panther.organizationapi.service.ApiTokenService
import com.gooddata.panther.organizationapi.utils.logUser
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import mu.KotlinLogging

@Controller(ApiTokenController.API_TOKEN_URL)
class ApiTokenController(private val apiTokenService: ApiTokenService) {

    @Post
    suspend fun createApiToken(
        request: HttpRequest<*>,
        @Body apiTokenRequestDto: ApiTokenRequestDto,
    ): ApiTokenResponseDto {
        logger.logUser("create_api_token", request.headers)
        return apiTokenService.createApiToken(request, apiTokenRequestDto)
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        const val API_TOKEN_URL = "/api/v1/panther/tokens"
    }
}
