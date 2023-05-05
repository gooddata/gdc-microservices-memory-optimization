/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Period

open class OrganizationTierProperties protected constructor() {
    var orgPasswordSecretName: String = DEFAULT_PASSWORD_SECRET_NAME
    var orgPasswordSecretKey: String = DEFAULT_PASSWORD_SECRET_KEY
    var orgExpiry: Period = DEFAULT_ORG_EXPIRY
    var orgMaxUserCount: Int = DEFAULT_MAX_USER_COUNT
    var orgMaxWorkspaceCount: Int = DEFAULT_MAX_WORKSPACE_COUNT
    var orgManagedOidc: Boolean = true
    var orgCertIssuer: String? = null
    var ingressAnnotations: Map<String, String>? = null
    var orgAnnotations: Map<String, String>? = null

    companion object {
        private const val DEFAULT_PASSWORD_SECRET_NAME = "bootstrap-token"
        private const val DEFAULT_PASSWORD_SECRET_KEY = "hash"
        private const val DEFAULT_MAX_USER_COUNT = 25
        private const val DEFAULT_MAX_WORKSPACE_COUNT = 10
        private val DEFAULT_ORG_EXPIRY = Period.ofDays(30)
    }
}
@ConfigurationProperties(prefix = "tiers.poc")
class PocTierProperties : OrganizationTierProperties()

@ConfigurationProperties(prefix = "tiers.professional")
class ProfessionalTierProperties : OrganizationTierProperties()

@ConfigurationProperties(prefix = "tiers.enterprise")
class EnterpriseTierProperties : OrganizationTierProperties()

@ConfigurationProperties(prefix = "tiers.demo")
class DemoTierProperties : OrganizationTierProperties()

@ConfigurationProperties(prefix = "tiers.trial")
class TrialTierProperties : OrganizationTierProperties()

@ConfigurationProperties(prefix = "tiers.internal")
class InternalTierProperties : OrganizationTierProperties()

@ConfigurationProperties(prefix = "tiers.partners")
class PartnersTierProperties : OrganizationTierProperties()
