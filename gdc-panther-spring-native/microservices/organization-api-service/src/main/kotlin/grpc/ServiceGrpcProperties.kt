/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.grpc

/**
 * Default properties for GRPC Service/Interface. Note that it's not necessary to have one Interface deployed
 * per host+port but in current defaults it's always single GRPC Interface per address.
 */
class ServiceGrpcProperties(
    var host: String = "localhost",
    var port: Int,
    var userAgent: String,
    var keepAliveWithoutCalls: Boolean = true,
    var keepAliveSeconds: Long = 30,
    var keepAliveTimeoutSeconds: Long = 5
)
