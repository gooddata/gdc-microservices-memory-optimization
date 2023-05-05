/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

import com.gooddata.api.logging.LogLevel.DEBUG
import com.gooddata.api.logging.LogLevel.ERROR
import com.gooddata.api.logging.LogLevel.INFO
import com.gooddata.api.logging.LogLevel.TRACE
import com.gooddata.api.logging.LogLevel.WARN
import mu.KLogger

/**
 * Log **trace** using the given [block] to build the log event.
 *
 * @param[block] log event building block
 */
fun KLogger.logTrace(block: LogBuilder.() -> Unit) = log(TRACE, block)

/**
 * Log **trace** with given [exception] using the given [block] to build the log event.
 *
 * @param[exception] exception to log
 * @param[block] log event building block
 */
fun KLogger.logTrace(exception: Throwable, block: LogBuilder.() -> Unit) = log(TRACE, exception, block)

/**
 * Log **debug** using the given [block] to build the log event.
 *
 * @param[block] log event building block
 */
fun KLogger.logDebug(block: LogBuilder.() -> Unit) = log(DEBUG, block)

/**
 * Log **debug** with given [exception] using the given [block] to build the log event.
 *
 * @param[exception] exception to log
 * @param[block] log event building block
 */
fun KLogger.logDebug(exception: Throwable, block: LogBuilder.() -> Unit) = log(DEBUG, exception, block)

/**
 * Log **info** using the given [block] to build the log event.
 *
 * @param[block] log event building block
 */
fun KLogger.logInfo(block: LogBuilder.() -> Unit) = log(INFO, block)

/**
 * Log **info** with given [exception] using the given [block] to build the log event.
 *
 * @param[exception] exception to log
 * @param[block] log event building block
 */
fun KLogger.logInfo(exception: Throwable, block: LogBuilder.() -> Unit) = log(INFO, exception, block)

/**
 * Log **warn** using the given [block] to build the log event.
 *
 * @param[block] log event building block
 */
fun KLogger.logWarn(block: LogBuilder.() -> Unit) = log(WARN, block)

/**
 * Log **warn** with given [exception] using the given [block] to build the log event.
 *
 * @param[exception] exception to log
 * @param[block] log event building block
 */
fun KLogger.logWarn(exception: Throwable, block: LogBuilder.() -> Unit) = log(WARN, exception, block)

/**
 * Log **error** using the given [block] to build the log event.
 *
 * @param[block] log event building block
 */
fun KLogger.logError(block: LogBuilder.() -> Unit) = log(ERROR, block)

/**
 * Log **error  ** with given [exception] using the given [block] to build the log event.
 *
 * @param[exception] exception to log
 * @param[block] log event building block
 */
fun KLogger.logError(exception: Throwable, block: LogBuilder.() -> Unit) = log(ERROR, exception, block)

/**
 * Build log event of given [level] using the given [block].
 *
 * @param level [LogLevel] to build event of
 * @param block set parameters of log event
 */
fun KLogger.log(level: LogLevel, block: LogBuilder.() -> Unit) {
    LogBuilder(level)
        .apply(block)
        .writeTo(this)
}

/**
 * Build log event of given [level] and [exception] using the given [block].
 *
 * @param level [LogLevel] to build event of
 * @param exception exception to log
 * @param block set parameters of log event
 */
fun KLogger.log(level: LogLevel, exception: Throwable, block: LogBuilder.() -> Unit) {
    LogBuilder(level)
        .apply(block)
        .apply { withException(exception) }
        .writeTo(this)
}
