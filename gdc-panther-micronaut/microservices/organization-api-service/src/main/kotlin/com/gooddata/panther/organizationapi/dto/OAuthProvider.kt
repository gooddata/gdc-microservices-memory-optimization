/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.micronaut.serde.annotation.Serdeable

@Serdeable
@JsonInclude(JsonInclude.Include.NON_NULL)
data class OAuthProvider(
    val oauthIssuerId: String,
    val oauthIssuerLocation: String,
    val oauthClientId: String,
    val oauthClientSecret: KubernetesSecretWrapper
)
