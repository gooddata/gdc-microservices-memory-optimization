/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "redbox")
data class SecretProperties(
    val secrets: Map<String, Map<String, EnvironmentSecrets>> = emptyMap()
)

data class EnvironmentSecrets(
    val clientId: String? = null,
    val secretKey: String? = null,
    val wildcardCertSecret: String? = null
)
