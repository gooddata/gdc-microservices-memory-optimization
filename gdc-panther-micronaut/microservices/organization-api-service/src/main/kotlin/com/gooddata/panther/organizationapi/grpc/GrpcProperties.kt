/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.grpc

import jakarta.inject.Singleton

/**
 * Default properties for GRPC Service/Interface. Note that it's not necessary to have one Interface deployed
 * per host+port but in current defaults it's always single GRPC Interface per address.
 */
@Singleton
class GrpcProperties(
    val host: String = "tiger-metadata-api-headless.tiger-latest.svc.cluster.local",
    val port: Int = INTERFACE_PORT,
    val userAgent: String = "organization-api-service",
    val keepAliveWithoutCalls: Boolean = true,
    val keepAliveSeconds: Long = 30,
    val keepAliveTimeoutSeconds: Long = 5
) {
    companion object Constants {
        private const val INTERFACE = "metadata"
        const val INTERFACE_PORT = 6572
        const val CHANNEL_BEAN = INTERFACE + "GrpcChannel"
        const val PROPERTIES_BEAN = INTERFACE + "Properties"
        const val PROPERTIES_PREFIX = "grpc.$INTERFACE"
    }
}
