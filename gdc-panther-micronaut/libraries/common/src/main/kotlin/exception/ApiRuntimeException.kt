/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.common.exception

import com.gooddata.api.exception.ApiException
import io.micronaut.http.HttpStatus

abstract class ApiRuntimeException(
    val httpStatus: HttpStatus,
    override val message: String,
    override val cause: Throwable? = null,
) : ApiException, RuntimeException(message, cause) {

    override fun toString() = "errorType=${this::class.qualifiedName}, message=$message"
}
