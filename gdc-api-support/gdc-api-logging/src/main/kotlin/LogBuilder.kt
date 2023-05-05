/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Log event [Marker] used to mark log events produced by [LogBuilder].
 */
val GD_API_MARKER: Marker = MarkerFactory.getMarker("GD_STRUCT_LOG")

/**
 * Builder for creating log events enhanced with metadata. Use `KLogger` extension functions to use it.
 */
@Suppress("TooManyFunctions")
class LogBuilder internal constructor(private var logLevel: LogLevel = LogLevel.INFO) {

    companion object {
        /**
         * Set of keys which can't be used by user (they are automatically set by underlying logging system).
         */
        val PROTECTED_KEYS = setOf(LogKey.TIMESTAMP, LogKey.LEVEL, LogKey.MESSAGE)
    }

    private val values = mutableMapOf<LogKey, Any>()

    private var message: () -> String = { "" }

    private var exception: Throwable? = null

    /**
     * Adds given [key] with given [value] to log event.
     *
     * @param[key] key to add
     * @param[value] value to add
     */
    fun withKey(key: LogKey, value: Any) {
        require(key !in PROTECTED_KEYS) { "It is prohibited to set key $key" }
        values[key] = value
    }

    /**
     * Adds given [keyValue] pair to log event.
     *
     * @param[keyValue] the first component becomes the key and the second component becomes the value
     */
    fun withKey(keyValue: Pair<LogKey, Any>) = withKey(keyValue.first, keyValue.second)

    /**
     * Adds all [keyValues] to log event.
     */
    fun withKeys(vararg keyValues: Pair<LogKey, Any>) = keyValues.forEach(this::withKey)

    /**
     * Adds given [keyName] with given [value] to log event. The [LogKey] is created using [keyName] as name and default
     * priority.
     *
     * @param[keyName] key name to add
     * @param[value] value to add
     */
    fun withSpecificKey(keyName: String, value: Any?) = withKey(LogKey(keyName), value ?: "null")

    /**
     * Adds given [message] to log event. Uses [LogKey.MESSAGE] as key.
     *
     * @param[message] message to add
     */
    fun withMessage(message: () -> String) {
        this.message = message
    }

    /**
     * Adds given [exception] to log event. Uses [LogKey.EXCEPTION] as key.
     *
     * @param[exception] exception to add
     */
    fun withException(exception: Throwable) {
        this.exception = exception
    }

    /**
     * Adds given [action] specific to log event. Uses [LogKey.ACTION] as key.
     * Typically used in more log entries in single function/class - like action="starting, action="stopping"
     *
     * @param[action] action to add
     */
    fun withAction(action: Any) = withKey(LogKey.ACTION, action)

    /**
     * Adds given [data] specific to log event. Uses [LogKey.DATA] as key.
     * Typically, data are longer text or raw structures, primarily for dumping
     *
     * @param[data] data to add
     */
    fun withData(data: Any) = withKey(LogKey.DATA, data)

    /**
     * Adds given [detail] to log event. Uses [LogKey.DETAIL] as key.
     * More descriptive text about log event (enable keep log message short).
     *
     * @param[detail] detail to add
     */
    fun withDetail(detail: Any) = withKey(LogKey.DETAIL, detail)

    /**
     * Adds given [duration] to log event. Uses [LogKey.DURATION] as key.
     * Typically used together with [withAction] to log `action` duration.
     *
     * @param[duration] detail to add
     */
    @OptIn(ExperimentalTime::class)
    fun withDuration(duration: Duration) = withKey(LogKey.DURATION, duration)

    /**
     * Adds given [durationMs] to log event. Uses [LogKey.DURATION_MS] as key.
     * Typically, used together with [withAction] to log `action` duration.
     *
     * @param[duration] duration in milliseconds
     */
    fun withDurationMs(duration: Long) = withKey(LogKey.DURATION_MS, duration)

    /**
     * Adds given [id] to log event. Uses [LogKey.ID] as key.
     * Specify any type of id according to place of usage.
     *
     * @param[id] id to add
     */
    fun withId(id: String) = withKey(LogKey.ID, id)

    /**
     * Adds given [method] to log event. Uses [LogKey.METHOD] as key.
     * Specify method where the log event happen (fog example gRPC method).
     *
     * @param[method] method to add
     */
    fun withMethod(method: Any) = withKey(LogKey.METHOD, method)

    /**
     * Adds given [state] to log event. Uses [LogKey.STATE] as key.
     * Specify state related to the log event.
     *
     * @param[state] method to add
     */
    fun withState(state: String) = withKey(LogKey.STATE, state)

    internal fun writeTo(logger: Logger) {
        if (isEnabled(logger, logLevel)) {
            log(logger, logLevel, message(), prepareArguments())
        }
    }

    private fun isEnabled(logger: Logger, logLevel: LogLevel): Boolean = when (logLevel) {
        LogLevel.TRACE -> logger.isTraceEnabled
        LogLevel.DEBUG -> logger.isDebugEnabled
        LogLevel.INFO -> logger.isInfoEnabled
        LogLevel.WARN -> logger.isWarnEnabled
        LogLevel.ERROR -> logger.isErrorEnabled
    }

    internal fun prepareArguments(): Array<Any> {
        val variables = values.flatMap { listOf(it.key, it.value) }
        val exceptionVariables = exception?.let { listOf(LogKey.EXCEPTION, it) } ?: emptyList()
        return (variables + exceptionVariables).toTypedArray()
    }

    @Suppress("SpreadOperator")
    private fun log(logger: Logger, logLevel: LogLevel, message: String, arguments: Array<Any>) = when (logLevel) {
        LogLevel.TRACE -> logger.trace(GD_API_MARKER, message, *arguments)
        LogLevel.DEBUG -> logger.debug(GD_API_MARKER, message, *arguments)
        LogLevel.INFO -> logger.info(GD_API_MARKER, message, *arguments)
        LogLevel.WARN -> logger.warn(GD_API_MARKER, message, *arguments)
        LogLevel.ERROR -> logger.error(GD_API_MARKER, message, *arguments)
    }
}
