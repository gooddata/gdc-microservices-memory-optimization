/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.nio.file.Path

@ConfigurationProperties("organization.repository")
data class OrganizationRepositoryProperties(
    val path: String = "",
    val branch: String = "",
    val token: String = "",
    val committerName: String = "",
    val committerEmail: String = "",
    val directory: Path? = null,
)
