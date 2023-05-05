/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeName
import io.micronaut.serde.annotation.Serdeable
import java.time.LocalDate

@Serdeable
@JsonTypeName("tierOrganization")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TierOrganizationDto(
    val organizationId: String? = null,
    val contactEmail: String,
    val organizationName: String,
    val hostname: String,
    val entitlements: List<Entitlement> = emptyList(),
    val deploymentProperties: DeploymentPropertiesDto,
    val dryRun: Boolean? = false
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Entitlement(
    val name: EntitlementType,
    val value: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd")
    val expiry: LocalDate? = null,
)

enum class TierType {
    POC, PROFESSIONAL, ENTERPRISE, DEMO, TRIAL, INTERNAL, PARTNERS
}

enum class EntitlementType {
    CONTRACT, TIER, USER_COUNT, WORKSPACE_COUNT, MANAGED_OIDC, UNLIMITED_USERS, UNLIMITED_WORKSPACES
}
