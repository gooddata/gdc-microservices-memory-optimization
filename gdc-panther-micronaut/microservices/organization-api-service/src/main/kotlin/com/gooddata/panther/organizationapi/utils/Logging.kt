/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.utils

import com.gooddata.panther.organizationapi.XAUTHHttpHeaders.X_AUTH_REQUEST_EMAIL
import com.gooddata.panther.organizationapi.XAUTHHttpHeaders.X_AUTH_REQUEST_USER
import io.micronaut.http.HttpHeaders
import mu.KLogger

fun KLogger.logUser(action: String, headers: HttpHeaders) {
    info(
        "action=$action method=log_user user={} email={}",
        headers.get(X_AUTH_REQUEST_USER) ?: "",
        headers.get(X_AUTH_REQUEST_EMAIL) ?: ""
    )
}
