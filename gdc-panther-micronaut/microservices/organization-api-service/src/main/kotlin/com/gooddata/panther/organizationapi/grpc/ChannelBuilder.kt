/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.grpc

import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.inject.Singleton
import java.util.concurrent.TimeUnit

/**
 * Helper functions to simplify instantiating new Stubs.
 */
@Singleton
class ChannelBuilder {

    fun buildChannel(
        properties: GrpcProperties,
        clientInterceptors: List<ClientInterceptor>
    ): ManagedChannel = ManagedChannelBuilder.forAddress(properties.host, properties.port)
        .defaultLoadBalancingPolicy("round_robin")
        .usePlaintext()
        .userAgent(properties.userAgent)
        .keepAliveWithoutCalls(properties.keepAliveWithoutCalls)
        .keepAliveTime(properties.keepAliveSeconds, TimeUnit.SECONDS)
        .keepAliveTimeout(properties.keepAliveTimeoutSeconds, TimeUnit.SECONDS)
        .intercept(clientInterceptors)
        .build()
}
