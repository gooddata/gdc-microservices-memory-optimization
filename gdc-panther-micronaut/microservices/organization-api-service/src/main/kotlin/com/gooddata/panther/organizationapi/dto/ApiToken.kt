/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiTokenRequestDto(
    val ticketId: String?,
    val organizationId: String,
    val userId: String,
    val tokenId: String
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiTokenResponseDto(
    val organizationId: String,
    val token: String
)
