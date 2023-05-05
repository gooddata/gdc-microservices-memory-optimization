/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi

import com.gooddata.panther.common.metric.MetricLogger
import com.gooddata.panther.organizationapi.logging.MetricCounterType
import com.gooddata.panther.organizationapi.logging.MetricTimerType
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton

@Factory
class BeanFactoryConfig {

    @Singleton
    fun metricLogger(meterRegistry: MeterRegistry): MetricLogger = MetricLogger(
        meterRegistry,
        MetricCounterType.values().associate { it.name to it.value },
        MetricTimerType.values().associate { it.name to it.value }
    )
}
