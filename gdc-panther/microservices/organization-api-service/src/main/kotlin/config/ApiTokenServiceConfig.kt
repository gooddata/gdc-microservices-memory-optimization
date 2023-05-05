/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.config

import com.gooddata.panther.organizationapi.grpc.GrpcClientBuilders.buildChannel
import com.gooddata.panther.organizationapi.grpc.GrpcClientBuilders.buildCoroutineStub
import com.gooddata.panther.organizationapi.grpc.ServiceGrpcProperties
import com.gooddata.panther.organizationapi.service.MetadataStoreServiceCoroutineGrpc
import com.gooddata.panther.organizationapi.service.MetadataStoreServiceCoroutineGrpc.MetadataStoreServiceCoroutineStub
import com.gooddata.panther.organizationapi.service.MetadataStoreServiceGrpc
import io.grpc.ClientInterceptor
import io.grpc.ManagedChannel
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@ConditionalOnClass(value = [MetadataStoreServiceGrpc::class])
@Configuration(proxyBeanMethods = false)
class ApiTokenServiceConfig {

    private val logger = KotlinLogging.logger {}

    companion object Constants {
        private const val INTERFACE = "metadata"
        const val INTERFACE_PORT = 6572
        const val CHANNEL_BEAN = INTERFACE + "GrpcChannel"
        const val PROPERTIES_BEAN = INTERFACE + "Properties"
        const val PROPERTIES_PREFIX = "grpc.$INTERFACE"
    }

    @Bean(PROPERTIES_BEAN)
    @ConfigurationProperties(prefix = PROPERTIES_PREFIX)
    fun properties(@Value("\${spring.application.name}") appName: String) =
        ServiceGrpcProperties(port = INTERFACE_PORT, userAgent = appName)

    @Bean(CHANNEL_BEAN)
    @ConditionalOnMissingBean(name = [CHANNEL_BEAN])
    fun managedChannel(
        @Qualifier(PROPERTIES_BEAN) properties: ServiceGrpcProperties,
        clientInterceptors: List<ClientInterceptor>
    ) = buildChannel(properties, clientInterceptors, logger)

    @Bean
    fun metadataStoreCoroutineStub(@Qualifier(CHANNEL_BEAN) channel: ManagedChannel) =
        buildCoroutineStub<MetadataStoreServiceCoroutineGrpc, MetadataStoreServiceCoroutineStub>(channel)
}
