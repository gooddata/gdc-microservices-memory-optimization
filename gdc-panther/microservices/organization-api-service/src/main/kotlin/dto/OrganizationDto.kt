/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID

private const val MAX_IDENTIFIER_LENGTH = 36

@JsonTypeName("deploymentProperties")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DeploymentPropertiesDto(
    val cluster: String,
    val deployment: String
) {
    init {
        require(cluster.isNotBlank() && deployment.isNotBlank()) { "Fields cluster and deployment cannot by blank" }
    }
    override fun toString(): String {
        return "$cluster/$deployment"
    }
}

@JsonTypeName("tls")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class TlsDto(
    val secretName: String,
    val issuerName: String? = null,
    val issuerType: String? = null
) {
    init {
        if (REGEX_SECRET_NAME.matches(secretName)) {
            require(issuerName.isNullOrBlank() && issuerType.isNullOrBlank()) {
                "IssuerName and IssuerType cannot be set with wildcard secretName"
            }
        } else {
            require(!(issuerName.isNullOrBlank() || (issuerType.isNullOrBlank()))) {
                "IssuerName and IssuerType cannot be empty"
            }
        }
    }

    companion object {
        val REGEX_SECRET_NAME = """^wildcard\..*-tls""".toRegex()
    }
}

@JsonTypeName("organization")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrganizationDto(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val hostname: String,
    val adminUserToken: String? = null,
    val adminUserTokenSecret: KubernetesSecretWrapper?,
    val deploymentProperties: DeploymentPropertiesDto,
    private val entitlements: Map<String, String>,
    val tls: TlsDto?,
    val orgAnnotations: Map<String, String>?,
    val ingressAnnotations: Map<String, String>?,
    val oauthProvider: OAuthProvider? = null,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val dryRun: Boolean? = false
) {
    val entitlementsConverted = convertEntitlements()

    init {
        validateId()
        require(name.isNotBlank())
        require(hostname.isNotBlank())
        validateAdminUserToken()
        validateEntitlements()
    }

    companion object {
        private val REGEX_ID = """^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$""".toRegex()
        private val REGEX_EXPIRY = """^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z$""".toRegex()
    }

    private fun validateId() {
        require(REGEX_ID.matches(id)) { "Organization ID is not valid" }
        require(id.length <= MAX_IDENTIFIER_LENGTH)
    }

    private fun convertEntitlements(): Map<String, String> {
        return entitlements.map {
            it.key.uppercase() to it.value
        }.toMap()
    }

    private fun validateEntitlements() {
        require(REGEX_EXPIRY.matches(entitlementsConverted.getOrDefault("CONTRACT", ""))) {
            "Entitlement 'Contract' has invalid value"
        }
        if (entitlementsConverted.getOrDefault("MANAGED_OIDC", "").isBlank()) {
            require(oauthProvider == null) { "OIDC cannot be set when the MANAGED_OIDC entitlement is not set" }
        } else {
            require(oauthProvider != null) { "OIDC cannot be empty when the MANAGED_OIDC entitlement is set" }
        }
    }

    private fun validateAdminUserToken() {
        val errMsg = "Exactly one of adminUserToken or adminUserTokenSecret needs to be properly filled"
        if (adminUserToken == null || adminUserToken.isBlank()) {
            requireNotNull(adminUserTokenSecret) { errMsg }
            require(
                adminUserTokenSecret.kubernetesSecret.key.isNotBlank() &&
                    adminUserTokenSecret.kubernetesSecret.name.isNotBlank()
            ) { errMsg }
        } else {
            require(adminUserTokenSecret == null) { errMsg }
        }
    }
}

data class KubernetesSecretWrapper(
    val kubernetesSecret: KubernetesSecret
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KubernetesSecret(
    val name: String,
    val key: String
)
