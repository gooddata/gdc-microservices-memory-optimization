/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.error

import com.gooddata.panther.common.exception.BadRequestException
import com.gooddata.panther.common.exception.NotFoundException
import com.gooddata.panther.common.exception.ServerRuntimeException

/**
 * Use this QueryParamsViolationException when HTTP query params validation hasn't succeeded.
 */
class QueryParamsViolationException(message: String) : BadRequestException(message)

/**
 * Use this OrganizationNotFoundException if the organization hasn't been found.
 */
class OrganizationNotFoundException(message: String) : NotFoundException(message)

/**
 * Use this GitException as a wrapper for {@link GitAPIException} from the Git client library.
 */
class GitException(message: String = "Internal git error.") : ServerRuntimeException(message)

/**
 * Use this K8sException as a wrapper for {@link ApiException} from the Kubernetes client library.
 */
class K8sException(message: String = "Internal k8s error.") : ServerRuntimeException(message)
