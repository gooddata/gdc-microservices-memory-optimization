/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.service

import com.gooddata.panther.organizationapi.config.DemoTierProperties
import com.gooddata.panther.organizationapi.config.EnterpriseTierProperties
import com.gooddata.panther.organizationapi.config.InternalTierProperties
import com.gooddata.panther.organizationapi.config.OidcProperties
import com.gooddata.panther.organizationapi.config.OrganizationTierProperties
import com.gooddata.panther.organizationapi.config.PartnersTierProperties
import com.gooddata.panther.organizationapi.config.PocTierProperties
import com.gooddata.panther.organizationapi.config.ProfessionalTierProperties
import com.gooddata.panther.organizationapi.config.SecretProperties
import com.gooddata.panther.organizationapi.config.TrialTierProperties
import com.gooddata.panther.organizationapi.dto.DeploymentPropertiesDto
import com.gooddata.panther.organizationapi.dto.EntitlementType
import com.gooddata.panther.organizationapi.dto.EntitlementType.CONTRACT
import com.gooddata.panther.organizationapi.dto.EntitlementType.MANAGED_OIDC
import com.gooddata.panther.organizationapi.dto.EntitlementType.TIER
import com.gooddata.panther.organizationapi.dto.EntitlementType.USER_COUNT
import com.gooddata.panther.organizationapi.dto.EntitlementType.WORKSPACE_COUNT
import com.gooddata.panther.organizationapi.dto.KubernetesSecret
import com.gooddata.panther.organizationapi.dto.KubernetesSecretWrapper
import com.gooddata.panther.organizationapi.dto.OAuthProvider
import com.gooddata.panther.organizationapi.dto.OrganizationDto
import com.gooddata.panther.organizationapi.dto.TierOrganizationDto
import com.gooddata.panther.organizationapi.dto.TierType
import com.gooddata.panther.organizationapi.dto.TierType.DEMO
import com.gooddata.panther.organizationapi.dto.TierType.ENTERPRISE
import com.gooddata.panther.organizationapi.dto.TierType.INTERNAL
import com.gooddata.panther.organizationapi.dto.TierType.PARTNERS
import com.gooddata.panther.organizationapi.dto.TierType.POC
import com.gooddata.panther.organizationapi.dto.TierType.PROFESSIONAL
import com.gooddata.panther.organizationapi.dto.TierType.TRIAL
import com.gooddata.panther.organizationapi.dto.TlsDto
import com.gooddata.panther.organizationapi.dto.TlsDto.Companion.REGEX_SECRET_NAME
import com.gooddata.panther.organizationapi.utils.IdGenerator
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class OrganizationTierPropertyService(
    val pocTierProperties: PocTierProperties,
    val professionalTierProperties: ProfessionalTierProperties,
    val enterpriseTierProperties: EnterpriseTierProperties,
    val demoTierProperties: DemoTierProperties,
    val trialTierProperties: TrialTierProperties,
    val internalTierProperties: InternalTierProperties,
    val partnersTierProperties: PartnersTierProperties,
    val oidcProperties: OidcProperties,
    val secretProperties: SecretProperties
) {
    companion object {
        private const val DEFAULT_ISSUER_TYPE = "ClusterIssuer"
    }

    fun toOrganizationDto(organization: TierOrganizationDto, tier: TierType): OrganizationDto {
        val tierDefaults = getTierDefaults(tier)

        val organizationId = organization.organizationId ?: IdGenerator.generateOrganizationId(
            organization.organizationName,
            organization.contactEmail,
            organization.deploymentProperties.cluster,
            organization.deploymentProperties.deployment
        )

        return OrganizationDto(
            id = organizationId,
            name = organization.organizationName,
            hostname = organization.hostname,
            adminUserTokenSecret = KubernetesSecretWrapper(
                KubernetesSecret(
                    name = tierDefaults.defaultProperties.orgPasswordSecretName,
                    key = tierDefaults.defaultProperties.orgPasswordSecretKey
                )
            ),
            deploymentProperties = DeploymentPropertiesDto(
                cluster = organization.deploymentProperties.cluster,
                deployment = organization.deploymentProperties.deployment
            ),
            tls = tls(organizationId, organization, tierDefaults),
            entitlements = entitlements(organization, tierDefaults),
            orgAnnotations = tierDefaults.defaultProperties.orgAnnotations,
            ingressAnnotations = tierDefaults.defaultProperties.ingressAnnotations,
            oauthProvider = oauthProvider(organization, tierDefaults),
            dryRun = organization.dryRun
        )
    }

    private fun getTierDefaults(tier: TierType): TierDefaults =
        when (tier) {
            POC -> TierDefaults(tier.toString(), pocTierProperties)
            PROFESSIONAL -> TierDefaults(tier.toString(), professionalTierProperties)
            ENTERPRISE -> TierDefaults(tier.toString(), enterpriseTierProperties)
            DEMO -> TierDefaults(tier.toString(), demoTierProperties)
            TRIAL -> TierDefaults(tier.toString(), trialTierProperties)
            INTERNAL -> TierDefaults(tier.toString(), internalTierProperties)
            PARTNERS -> TierDefaults(tier.toString(), partnersTierProperties)
        }

    private fun tls(
        organizationId: String,
        organization: TierOrganizationDto,
        tierDefaults: TierDefaults
    ): TlsDto {
        val wildcardCertSecret = secretProperties.secrets[organization.deploymentProperties.cluster]
            ?.get(tierDefaults.tier.lowercase())?.wildcardCertSecret
        return if (isValidWildcardCertSecret(wildcardCertSecret)) {
            TlsDto(
                secretName = wildcardCertSecret!!
            )
        } else {
            TlsDto(
                secretName = "$organizationId-tls",
                issuerName = tierDefaults.defaultProperties.orgCertIssuer,
                issuerType = DEFAULT_ISSUER_TYPE
            )
        }
    }

    private fun isValidWildcardCertSecret(wildcardCertSecret: String?): Boolean =
        wildcardCertSecret?.let {
            REGEX_SECRET_NAME.matches(wildcardCertSecret)
        } ?: false

    private fun entitlements(
        organization: TierOrganizationDto,
        tierDefaults: TierDefaults
    ): Map<String, String> {
        val expirationDate = expirationDate(organization, tierDefaults)
        val entitlements: MutableMap<EntitlementType, Any> = mutableMapOf(
            CONTRACT to expirationDate,
            TIER to tierDefaults.tier,
            USER_COUNT to tierDefaults.defaultProperties.orgMaxUserCount,
            WORKSPACE_COUNT to (organization.workspaceCount
                ?: tierDefaults.defaultProperties.orgMaxWorkspaceCount)
        )
        if (tierDefaults.defaultProperties.orgManagedOidc) {
            entitlements[MANAGED_OIDC] = true
        }

        return entitlements.map {
            it.key.toString() to it.value.toString()
        }.toMap()
    }

    private fun expirationDate(organization: TierOrganizationDto, tierDefaults: TierDefaults): Instant =
        (organization.expirationDate ?: LocalDate.now().plus(tierDefaults.defaultProperties.orgExpiry))
            .atStartOfDay().toInstant(ZoneOffset.UTC)

    private fun oauthProvider(
        organization: TierOrganizationDto,
        tierDefaults: TierDefaults
    ) = if (tierDefaults.defaultProperties.orgManagedOidc) {
        val defaultOAuthProvider: OAuthProvider? =
            oidcProperties.profile[organization.deploymentProperties.cluster]?.oauth
        val environmentSecrets = secretProperties.secrets[organization.deploymentProperties.cluster]
            ?.get(tierDefaults.tier.lowercase())
        defaultOAuthProvider?.let {
            OAuthProvider(
                oauthIssuerId = defaultOAuthProvider.oauthIssuerId,
                oauthIssuerLocation = defaultOAuthProvider.oauthIssuerLocation,
                oauthClientId = environmentSecrets?.clientId?.let { environmentSecrets.clientId }
                    ?: defaultOAuthProvider.oauthClientId,
                oauthClientSecret = KubernetesSecretWrapper(
                    kubernetesSecret = KubernetesSecret(
                        name = defaultOAuthProvider.oauthClientSecret.kubernetesSecret.name,
                        key = environmentSecrets?.secretKey?.let { environmentSecrets.secretKey }
                            ?: defaultOAuthProvider.oauthClientSecret.kubernetesSecret.key
                    )
                )
            )
        }
    } else {
        null
    }
}

data class TierDefaults(
    val tier: String,
    val defaultProperties: OrganizationTierProperties
)
