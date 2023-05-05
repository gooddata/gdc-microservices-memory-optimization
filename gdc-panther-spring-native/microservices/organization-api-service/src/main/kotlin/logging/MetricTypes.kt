/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.logging

private const val BASE_NAME: String = "organization-api.service.organization"

enum class MetricCounterType(val value: String) {
    CREATE_ORG("$BASE_NAME.create.sufficient"),
    CREATE_ORG_K8S("$BASE_NAME.create.k8s"),
    CREATE_ORG_REPOSITORY("$BASE_NAME.create.repository"),
    DELETE_ORG("$BASE_NAME.delete"),
    DELETE_ORG_REPOSITORY("$BASE_NAME.delete.repository"),
}

enum class MetricTimerType(val value: String) {
    CREATE_ORG_DURATION("$BASE_NAME.duration.create.all"),
    CREATE_ORG_K8S_DURATION("$BASE_NAME.duration.create.k8s"),
    CREATE_ORG_REPOSITORY_DURATION("$BASE_NAME.duration.create.repository"),
    DELETE_ORG_DURATION("$BASE_NAME.duration.delete.all"),
    DELETE_ORG_REPOSITORY_DURATION("$BASE_NAME.duration.delete.repository"),
}
