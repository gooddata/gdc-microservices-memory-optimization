/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.utils

import com.gooddata.panther.organizationapi.error.QueryParamsViolationException
import io.micronaut.http.HttpRequest
import mu.KLogger

object Validator {

    /**
     * Validate query params of http request against passed keys argument.
     *
     * @param request server request to validate
     * @param logger
     * @param keys expected query param keys
     * @throws QueryParamsViolationException if violation occurred
     */
    @Throws(QueryParamsViolationException::class)
    fun validateQueryParams(request: HttpRequest<*>, logger: KLogger, vararg keys: String) {

        val queryParamsViolation: MutableMap<String, String> = mutableMapOf()

        keys.forEach { k ->
            val queryParam = request.parameters.getFirst(k)
            if (queryParam.isEmpty) {
                queryParamsViolation[k] = "Query param is missing"
            } else if (queryParam.get().isEmpty()) {
                queryParamsViolation[k] = "Query param is empty"
            }
        }
        val message = "Http request query params violation in: " +
            queryParamsViolation.entries.joinToString { "${it.key}=${it.value}" }
        if (queryParamsViolation.isNotEmpty()) {
            logger.error {
                "action=processing_request status=ERROR path=${request.uri.rawPath} " +
                    "message=$message"
            }
            throw QueryParamsViolationException(message)
        }
    }
}
