/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import jakarta.inject.Singleton

@Singleton
data class OrganizationServiceConfig(
    val managedCluster: String = "dev11",
    val controlledNamespaces: List<String> = listOf("tiger-latest")
)
