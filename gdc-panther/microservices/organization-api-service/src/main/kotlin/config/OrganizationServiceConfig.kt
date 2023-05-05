/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("organization-service")
data class OrganizationServiceConfig(
    val managedCluster: String = "",
    val controlledNamespaces: List<String> = listOf()
)
