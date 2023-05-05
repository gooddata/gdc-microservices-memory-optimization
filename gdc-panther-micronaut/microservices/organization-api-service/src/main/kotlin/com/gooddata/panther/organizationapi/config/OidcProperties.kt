/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import com.gooddata.panther.organizationapi.dto.OAuthProvider
import jakarta.inject.Singleton

@Singleton
data class OidcProperties(
    val profile: Map<String, OAuthProperties> = emptyMap(),
)

data class OAuthProperties(
    val oauth: OAuthProvider
)
