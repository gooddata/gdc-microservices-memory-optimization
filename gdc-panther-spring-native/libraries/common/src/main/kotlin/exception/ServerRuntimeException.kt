/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.common.exception

import com.gooddata.api.exception.ServerException
import org.springframework.http.HttpStatus

open class ServerRuntimeException(
    message: String,
    httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
) : ApiRuntimeException(httpStatus, message), ServerException
