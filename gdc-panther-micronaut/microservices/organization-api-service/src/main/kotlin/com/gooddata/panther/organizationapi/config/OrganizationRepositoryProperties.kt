/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import jakarta.inject.Singleton
import java.nio.file.Path

@Singleton
data class OrganizationRepositoryProperties(
    val path: String = "",
    val branch: String = "",
    val token: String = "",
    val committerName: String = "",
    val committerEmail: String = "",
    val directory: Path = Path.of("")
)
