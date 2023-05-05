/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import com.gooddata.panther.organizationapi.dto.OAuthProvider
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "oidc")
data class OidcProperties(
    val profile: Map<String, OAuthProperties> = emptyMap()
)

data class OAuthProperties(
    val oauth: OAuthProvider
)
