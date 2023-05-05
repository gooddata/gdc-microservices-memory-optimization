/*
 * (C) 2021 GoodData Corporation
 */
package com.gooddata.api.exception

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.KVisibility

/**
 * Interface for exceptions which are caused by the interaction with any API.
 */
interface ApiException {
    /**
     * The reason of the exception.
     */
    val message: String

    /**
     * The specific [Throwable] that caused this throwable to get thrown. `null` in case that this throwable
     * was not caused by another throwable (or causative throwable is unknown).
     */
    val cause: Throwable?
}

/**
 * Interface for exceptions which are caused by the incorrect client interaction with the API.
 *
 * This means that the client can change something to correct the error state.
 *
 * @see ApiException
 */
interface ClientException : ApiException {

    /**
     * Template used by [renderMessage] for error messages. Message format depends on used renderer. Content of
     * [message] is not overridden by default.
     *
     * @see renderMessage
     */
    val messageTemplate: String
        get() = message

    /**
     * Generates public message using message renderer.
     * @param [messageRenderer] message renderer
     */
    fun renderMessage(messageRenderer: ExceptionMessageRenderer = DefaultExceptionMessageRenderer) =
        messageTemplate.let { messageRenderer.render(it, fieldValues()) }

    private fun fieldValues() =
        this::class.memberProperties
            .filter { it.visibility == KVisibility.PUBLIC && it.name != ClientException::message.name }
            .map { it.name to it.getter.call(this) }
            .toMap()
}

/**
 * Interface for exceptions which are caused by the incorrect server state during client interaction with the API.
 * Example of such incorrect state can be invalid server configuration or some I/O error occurred on server backends
 * during processing.
 *
 * This means that the client is not able to fix this issue and must contact the server maintainer.
 *
 * @see ApiException
 */
interface ServerException : ApiException

/**
 * Exception message renderer.
 */
interface ExceptionMessageRenderer {

    /**
     * Renders message
     * @param [template] message template
     * @param [values] values to be used in message
     */
    fun render(template: String, values: Map<String, Any?>) = values.entries.fold(template) { filledTemplate, entry ->
        replacePlaceholder(filledTemplate, entry.key, convertValue(entry.key, entry.value))
    }

    /**
     * Replaces one all name placeholders with given value in template.
     * @receiver template with placeholders in format "Some text ${name}"
     * @param name placeholder name
     * @param value value to use instead of placeholder
     * @return template with replaced placeholders
     */
    fun replacePlaceholder(template: String, name: String, value: String) =
        template.replace("\${$name}", value)

    /**
     * Convert field value to its string representation. This value will be used in template.
     * @param [name] field name
     * @param [value] field value
     * @see render
     */
    fun convertValue(name: String, value: Any?) = value.toString()
}

/**
 * Default exception message generator which simply convert exception parameters to string representation.
 */
object DefaultExceptionMessageRenderer : ExceptionMessageRenderer {
    override fun convertValue(name: String, value: Any?) = when (value) {
        null -> "null"
        is String -> value
        is KClass<*> -> value.simpleName ?: "null"
        is Iterable<*> -> value.joinToString()
        else -> value.toString()
    }
}
