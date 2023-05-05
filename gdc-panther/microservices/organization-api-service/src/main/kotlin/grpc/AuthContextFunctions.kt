/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.grpc

import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.Metadata as GrpcMetadata

internal val TIGER_USER_ID_METADATA_KEY: GrpcMetadata.Key<String> =
    GrpcMetadata.Key.of("x-tiger-user-id", ASCII_STRING_MARSHALLER)

internal val TIGER_ORGANIZATION_ID_METADATA_KEY: GrpcMetadata.Key<String> =
    GrpcMetadata.Key.of("x-tiger-organization-id", ASCII_STRING_MARSHALLER)
