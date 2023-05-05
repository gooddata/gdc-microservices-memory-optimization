/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.common.metric

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.toJavaDuration

enum class MetricCounterName(val value: String) {
    TOTAL("total"),
    SUCCESS("success"),
    FAILURE("failure")
}

class MetricLogger(
    private val meterRegistry: MeterRegistry,
    metricCounterTypes: Map<String, String>,
    metricTimerTypes: Map<String, String>,
) {

    private val metricCounters: Map<String, Map<String, Counter>> = collectMetricCounters(metricCounterTypes)
    private val metricTimers: Map<String, Timer> = collectMetricTimer(metricTimerTypes)

    fun <T : Enum<T>> countSuccessFinish(metricCounter: Enum<T>) = countSuccessFinish(metricCounter.name)

    fun countSuccessFinish(metricCounterName: String) {
        metricCounters.getValue(metricCounterName).getValue(MetricCounterName.SUCCESS.name).increment()
        metricCounters.getValue(metricCounterName).getValue(MetricCounterName.TOTAL.name).increment()
    }

    fun <T : Enum<T>> countException(metricCounter: Enum<T>) = countException(metricCounter.name)

    fun countException(metricCounterName: String) {
        metricCounters.getValue(metricCounterName).getValue(MetricCounterName.FAILURE.name).increment()
        metricCounters.getValue(metricCounterName).getValue(MetricCounterName.TOTAL.name).increment()
    }

    fun <T, E1 : Enum<E1>, E2 : Enum<E2>> durationWithCounter(
        metricTimer: Enum<E1>,
        metricCounter: Enum<E2>,
        f: () -> T
    ): T? =
        durationWithCounter(metricTimer.name, metricCounter.name, f)

    /**
     * Record time of function passed as lambda, and count success or error.
     */
    @Suppress("TooGenericExceptionCaught")
    fun <T> durationWithCounter(metricTimerName: String, metricCounterName: String, f: () -> T): T? {
        try {
            val result = metricTimers.getValue(metricTimerName).recordCallable(f)
            countSuccessFinish(metricCounterName)
            return result
        } catch (e: Exception) {
            countException(metricCounterName)
            throw e
        }
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("TooGenericExceptionCaught")
    suspend fun <T, E1 : Enum<E1>, E2 : Enum<E2>> durationWithCounterAsync(
        metricTimer: Enum<E1>,
        metricCounter: Enum<E2>,
        block: suspend () -> T
    ): T = measureTimedValue {
        try {
            block().let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }.let {
        metricTimers.getValue(metricTimer.name).record(it.duration.toJavaDuration())
        it.value
            .onSuccess { countSuccessFinish(metricCounter.name) }
            .onFailure { countException(metricCounter.name) }
            .getOrThrow()
    }

    private fun collectMetricTimer(metricMap: Map<String, String>) = metricMap.entries.associate {
        it.key to meterRegistry.timer(it.value)
    }

    private fun collectMetricCounters(metricMap: Map<String, String>) = metricMap.entries.associate { counterType ->
        counterType.key to generateStatusCounters(counterType.value)
    }

    private fun generateStatusCounters(
        counterType: String
    ) = MetricCounterName.values().associate {
            counterName ->
        counterName.name to Counter.builder("$counterType.${counterName.value}").register(meterRegistry)
    }
}
