/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi

object PathParams {
    const val ORGANIZATION_ID = "organizationId"
    const val TIER_TYPE = "tierType"
}

object QueryParams {
    const val CLUSTER = "cluster"
    const val DEPLOYMENT = "deployment"
}

object XAUTHHttpHeaders {
    const val X_AUTH_REQUEST_EMAIL = "x-auth-request-email"
}

object Kubernetes {

    object Organization {
        const val GROUP = "controllers.gooddata.com"
        const val OBJECT = "organizations"
        const val VERSION = "v1"
    }
}
