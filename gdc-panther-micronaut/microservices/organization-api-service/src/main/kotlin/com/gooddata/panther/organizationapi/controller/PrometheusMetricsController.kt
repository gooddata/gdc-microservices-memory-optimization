/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.controller

import io.micrometer.prometheus.PrometheusMeterRegistry
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller
class PrometheusMetricsController(private val registry: PrometheusMeterRegistry) {

    @Get("/prometheus")
    fun getMetrics() = registry.scrape()
}
