/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("organization")
data class OrganizationProperties(
    val mandatoryEntitlements: List<String> = listOf(),
    val defaultEntitlements: Map<String, String> = mapOf()
)
