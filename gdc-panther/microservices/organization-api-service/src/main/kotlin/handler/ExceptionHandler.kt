/*
 * (C) 2022 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.handler

import com.gooddata.panther.common.exception.ApiRuntimeException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.ErrorProperties
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.MergedAnnotation
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.result.view.ViewResolver
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono

private const val STATUS_PARAM = "status"
private const val REASON_PARAM = "reason"

@Component
class GdcErrorAttributes(
    @Value("\${spring.application.name}") private val appName: String
) : DefaultErrorAttributes() {

    override fun getErrorAttributes(
        request: ServerRequest?,
        options: ErrorAttributeOptions?
    ): MutableMap<String, Any> {
        val gdcErrorAttributes = hashMapOf<String, Any>()
        val innerAttributes = hashMapOf<String, Any>()
        val error = getError(request)
        val responseStatusAnnotation = MergedAnnotations
            .from(error.javaClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
            .get(ResponseStatus::class.java)
        val status = determineHttpStatus(error, responseStatusAnnotation)
        innerAttributes["errorClass"] = error.javaClass.simpleName
        innerAttributes["message"] = determineMessage(error, responseStatusAnnotation).orEmpty()
        innerAttributes["component"] = appName
        gdcErrorAttributes["error"] = innerAttributes
        gdcErrorAttributes[STATUS_PARAM] = status.value()
        gdcErrorAttributes[REASON_PARAM] = status.reasonPhrase

        return gdcErrorAttributes
    }

    private fun determineHttpStatus(
        error: Throwable,
        responseStatusAnnotation: MergedAnnotation<ResponseStatus>
    ) = when (error) {
        is ResponseStatusException -> error.status
        is ApiRuntimeException -> error.httpStatus
        else -> responseStatusAnnotation.getValue("code", HttpStatus::class.java)
            .orElse(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    private fun determineMessage(
        error: Throwable,
        responseStatusAnnotation: MergedAnnotation<ResponseStatus>
    ) = when (error) {
        is ServerWebInputException -> error.cause?.localizedMessage ?: error.reason
        is ApiRuntimeException -> error.message
        is ResponseStatusException -> error.reason
        else -> responseStatusAnnotation.getValue("reason", String::class.java).orElseGet { error.message }
    }
}

@Order(-1)
@Component
@Suppress("LongParameterList")
class GlobalExceptionHandler(
    errorAttributes: ErrorAttributes,
    webProperties: WebProperties,
    errorProperties: ErrorProperties,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer,
    viewResolvers: List<ViewResolver>
) : DefaultErrorWebExceptionHandler(errorAttributes, webProperties.resources, errorProperties, applicationContext) {

    init {
        super.setMessageReaders(serverCodecConfigurer.readers)
        super.setMessageWriters(serverCodecConfigurer.writers)
        super.setViewResolvers(viewResolvers)
    }

    override fun renderErrorResponse(request: ServerRequest?): Mono<ServerResponse?>? {
        val includeStackTrace = isIncludeStackTrace(request, MediaType.ALL)
        val error = getErrorAttributes(
            request,
            if (includeStackTrace) ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE)
            else ErrorAttributeOptions.defaults()
        )
        val httpStatus = getHttpStatus(error)
        // hack not to render status and reason in JSON response
        error?.remove(STATUS_PARAM)
        error?.remove(REASON_PARAM)
        return ServerResponse.status(httpStatus).contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(error))
    }
}
