/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.common.exception

import com.gooddata.api.exception.ClientException
import io.micronaut.http.HttpStatus

open class ClientRuntimeException(
    message: String,
    httpStatus: HttpStatus = HttpStatus.BAD_REQUEST
) : ApiRuntimeException(httpStatus, message), ClientException

/**
 * Use this exception if you want to log the exception detail, but you don't want to provide the detail to client.
 * Instead, 404 and default message will be returned.
 */
open class NotFoundException(
    message: String,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND
) : ClientRuntimeException(message, httpStatus = httpStatus)

/**
 * Use this BadRequestException exception if you want to return 400 and custom or pretty formatted error messages.
 * Include data that are safe to return to the client as entity names and IDs.
 */
open class BadRequestException(
    message: String,
    httpStatus: HttpStatus = HttpStatus.BAD_REQUEST
) : ClientRuntimeException(message, httpStatus = httpStatus)
