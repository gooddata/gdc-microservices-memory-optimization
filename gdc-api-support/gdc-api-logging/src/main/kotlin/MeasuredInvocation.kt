/*
 * (C) 2021 GoodData Corporation
 */

@file:OptIn(ExperimentalContracts::class, ExperimentalTime::class)

package com.gooddata.api.logging

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.ExperimentalTime
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

/**
 * Run block of code and measure time of running
 * Result<R> is packed in TimedValue, so caller can use result and duration as well
 *
 * @param[R] result of the run block
 * @param[block] block of code to measure
 * @return timed value containing the block run result and measured time
 */
inline fun <R> measureCatching(block: () -> R): TimedValue<Result<R>> {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val (result, duration) = measureTimedValue { runCatching { block() } }
    return TimedValue(result, duration)
}

/**
 * Executes given [action] if the receiver's result is success, otherwise does nothing.
 *
 * @receiver timed value of the result
 * @param[action] action to execute on receiver's result success
 * @return receiver's instance
 */
inline fun <T> TimedValue<Result<T>>.onSuccess(action: (TimedValue<T>) -> Unit): TimedValue<Result<T>> {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    value.onSuccess {
        action(TimedValue(it, duration))
    }
    return this
}

/**
 * Executes given [action] if the receiver's result is failure, otherwise does nothing.
 *
 * @receiver timed value of the result
 * @param[action] action to execute on receiver's result failure
 * @return receiver's instance
 */
inline fun <T> TimedValue<Result<T>>.onFailure(action: (TimedValue<Throwable>) -> Unit): TimedValue<Result<T>> {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    value.onFailure {
        action(TimedValue(it, duration))
    }
    return this
}
