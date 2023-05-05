/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.grpc

import com.gooddata.api.logging.logInfo
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.AbstractStub
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure
import mu.KLogger

/**
 * Helper functions to simplify instantiating new Stubs.
 */
object GrpcClientBuilders {

    @Suppress("NOTHING_TO_INLINE")
    inline fun buildChannel(
        properties: ServiceGrpcProperties,
        clientInterceptors: List<ClientInterceptor>,
        logger: KLogger
    ) = ManagedChannelBuilder.forAddress(properties.host, properties.port)
        .defaultLoadBalancingPolicy("round_robin")
        .usePlaintext()
        .userAgent(properties.userAgent)
        .keepAliveWithoutCalls(properties.keepAliveWithoutCalls)
        .keepAliveTime(properties.keepAliveSeconds, TimeUnit.SECONDS)
        .keepAliveTimeout(properties.keepAliveTimeoutSeconds, TimeUnit.SECONDS)
        .intercept(clientInterceptors)
        .build()
        .also {
            logger.logInfo {
                withAction("grpcClientInit")
                withMessage { "GRPC Client started for host: ${properties.host}:${properties.port}}" }
            }
        }

    inline fun <reified T, reified R : AbstractStub<R>> buildBlockingStub(channel: ManagedChannel): R =
        generateStub<T, R>("newBlockingStub", channel)

    inline fun <reified T, reified R : AbstractStub<R>> buildCoroutineStub(channel: ManagedChannel): R =
        generateStub<T, R>("newStub", channel)

    /**
     * For clarity function will check if builder function for stubs is available.
     * @throws ClassCastException in case that result type is not expected [R]
     */
    inline fun <reified T, reified R : AbstractStub<R>> generateStub(methodName: String, channel: ManagedChannel) = (
        T::class.functions.find { it.name == methodName }
            ?: throw IllegalStateException("stub builder function $methodName is not available")
        )
        .run {
            instanceParameter?.let { call(it.type.jvmErasure.objectInstance, channel) } ?: call(channel)
        } as R
}
