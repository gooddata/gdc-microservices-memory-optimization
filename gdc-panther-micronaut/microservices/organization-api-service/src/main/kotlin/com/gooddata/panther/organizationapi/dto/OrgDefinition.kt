/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrgDefinition(
    val apiVersion: String = "controllers.gooddata.com/v1",
    val kind: String = "Organization",
    val metadata: OrgMetadata,
    val spec: OrgSpec
) {
    companion object {
        fun build(
            org: OrganizationDto,
            entitlementsOverride: Map<String, String?> = org.entitlementsConverted
        ): OrgDefinition {
            val metadata = OrgMetadata(org.id, org.deploymentProperties.deployment, org.orgAnnotations)
            val tls = OrgTls(
                issuerName = org.tls?.issuerName,
                issuerType = org.tls?.issuerType,
                secretName = org.tls?.secretName
            )
            val entitlements: List<OrgEntitlement> = entitlementsOverride.map {
                if (it.key == "CONTRACT") {
                    OrgEntitlement(name = it.key, expiry = it.value)
                } else {
                    OrgEntitlement(name = it.key, value = it.value)
                }
            }
            val spec = OrgSpec(
                adminUserToken = org.adminUserToken,
                adminUserTokenSecret = org.adminUserTokenSecret,
                entitlements = entitlements,
                hostname = org.hostname,
                id = org.id,
                name = org.name,
                tls = tls,
                ingressAnnotations = org.ingressAnnotations,
                oauthProvider = org.oauthProvider
            )

            return OrgDefinition(
                metadata = metadata,
                spec = spec
            )
        }
        fun build(
            org: OrgDefinition,
            annotationOverride: Map<String, String>? = org.metadata.annotations
        ): OrgDefinition {
            val metadata = OrgMetadata(org.metadata.name, org.metadata.namespace, annotationOverride)

            return OrgDefinition(
                metadata = metadata,
                spec = org.spec
            )
        }
    }
}

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrgEntitlement(
    val expiry: String? = null,
    val name: String,
    val value: String? = null
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrgMetadata(
    val name: String,
    val namespace: String,
    val annotations: Map<String, String>? = null
)

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OrgSpec(
    val adminGroup: String = "adminGroup",
    val adminUser: String = "admin",
    val adminUserToken: String?,
    val adminUserTokenSecret: KubernetesSecretWrapper?,
    val entitlements: List<OrgEntitlement>?,
    val hostname: String,
    val id: String,
    val name: String,
    val tls: OrgTls?,
    val ingressAnnotations: Map<String, String>?,
    val oauthProvider: OAuthProvider? = null
)

@Serdeable
data class OrgTls(
    val issuerName: String? = null,
    val issuerType: String? = null,
    val secretName: String? = null
)
