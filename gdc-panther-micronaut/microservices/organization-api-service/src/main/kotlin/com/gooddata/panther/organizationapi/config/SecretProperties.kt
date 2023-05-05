/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import jakarta.inject.Singleton

@Singleton
data class SecretProperties(
    val secrets: Map<String, Map<String, EnvironmentSecrets>> = emptyMap()
)

data class EnvironmentSecrets(
    val clientId: String? = null,
    val secretKey: String? = null,
    val wildcardCertSecret: String? = null,
)
