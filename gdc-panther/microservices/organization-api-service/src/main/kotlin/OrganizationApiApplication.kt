/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi

import com.gooddata.panther.common.metric.MetricLogger
import com.gooddata.panther.organizationapi.logging.MetricCounterType
import com.gooddata.panther.organizationapi.logging.MetricTimerType
import com.gooddata.panther.organizationapi.service.KubernetesService
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.ErrorProperties
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {
    @Bean
    fun errorProperties() = ErrorProperties()

    @Bean
    fun kubernetesService() = KubernetesService()

    @Bean
    fun metricLogger(meterRegistry: MeterRegistry) = MetricLogger(
        meterRegistry,
        MetricCounterType.values().associate { it.name to it.value },
        MetricTimerType.values().associate { it.name to it.value }
    )
}

@SpringBootApplication
@ConfigurationPropertiesScan("com.gooddata.panther.organizationapi.config")
class OrganizationApiApplication
fun main(args: Array<String>) { runApplication<OrganizationApiApplication>(*args) }
