/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDate

@JsonTypeName("tierOrganization")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TierOrganizationDto(
    val organizationId: String? = null,
    val contactEmail: String,
    val organizationName: String,
    val hostname: String,
    val expiry: String? = null,
    val workspaceCount: Int? = null,
    val deploymentProperties: DeploymentPropertiesDto,
    val dryRun: Boolean? = false
) {
    var expirationDate: LocalDate? = null
    companion object {
        private val REGEX_EXPIRY = """^[0-9]{4}-[0-9]{2}-[0-9]{2}$""".toRegex()
    }
    init {
        expiry?.let {
            require(REGEX_EXPIRY.matches(expiry)) {
                "Expiration date must be in YYYY-MM-DD format!"
            }
            expirationDate = LocalDate.parse(expiry)
        }
    }
}

enum class TierType {
    POC, PROFESSIONAL, ENTERPRISE, DEMO, TRIAL, INTERNAL, PARTNERS
}

enum class EntitlementType {
    CONTRACT, TIER, USER_COUNT, WORKSPACE_COUNT, MANAGED_OIDC
}
