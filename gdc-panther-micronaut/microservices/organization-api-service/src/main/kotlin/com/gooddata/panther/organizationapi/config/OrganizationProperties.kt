/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import jakarta.inject.Singleton

@Singleton
data class OrganizationProperties(
    val mandatoryEntitlements: List<String> = listOf(),
    val defaultEntitlements: Map<String, String?> = mapOf(
        "WORKSPACE_COUNT" to "10",
        "USER_COUNT" to "10"
    )
)
