/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

/**
 * Gathers constant [LogKey]s used in MDC (meaning they are not intended for usage within [LogBuilder])
 */
object MdcLogKey {
    // Keys for tracing
    val TRACE_ID = LogKey(name = "traceId", contextMatch = "traceId")
    val SPAN_ID = LogKey(name = "spanId", contextMatch = "spanId")
    val PARENT_ID = LogKey(name = "parentId", contextMatch = "parentId")
    val SAMPLED = LogKey(name = "sampled", contextMatch = "sampled")

    val USER_ID = LogKey(name = "userId", contextMatch = "userId")
    val ORGANIZATION_ID = LogKey(name = "orgId", contextMatch = "orgId")
}
