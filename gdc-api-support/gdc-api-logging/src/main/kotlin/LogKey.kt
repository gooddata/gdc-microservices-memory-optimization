/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

/**
 * Used as key to pass some information to log event.
 *
 * @see LogBuilder
 */
data class LogKey(
    /**
     * Name of key, the same name will be used to log output.
     */
    val name: String,

    /**
     * Defines order of keys, lower numbers mean more important keys. Defaults to [NORMAL_PRIORITY]
     */
    val order: Int = NORMAL_PRIORITY,

    /**
     * Whether this key should match some key from MDC - could contain exact key or pattern. Defaults to `null`
     * meaning, there is related MDC key.
     */
    val contextMatch: String? = null
) : Comparable<LogKey> {

    companion object {
        const val LOW_PRIORITY = 10000
        const val NORMAL_PRIORITY = 1000
        const val HIGH_PRIORITY = 100

        /**
         * **PROTECTED** key representing event timestamp. Can't be used by [LogBuilder].
         */
        val TIMESTAMP = LogKey("ts", 1)

        /**
         * **PROTECTED** key representing event level. Can't be used by [LogBuilder].
         */
        val LEVEL = LogKey("level", 2)

        /**
         * **PROTECTED** key representing event message. Can't be used by [LogBuilder].
         */
        val MESSAGE = LogKey("msg", 3)
        val APPLICATION = LogKey("app", 4)
        val LOGGER = LogKey("logger", 5)
        val THREAD = LogKey("thread", 6)

        val EXCEPTION = LogKey("exc", Int.MAX_VALUE)

        val ACTION = LogKey("action")
        val DATA = LogKey("data", LOW_PRIORITY)
        val DETAIL = LogKey("detail", LOW_PRIORITY)
        val DURATION = LogKey("duration")
        val DURATION_MS = LogKey("durationMs")
        val ID = LogKey("id")
        val METHOD = LogKey("method")
        val STATE = LogKey("state")
    }

    /**
     * Check, whether has the same name as some other log key.
     * @param[other] other log key to compare
     * @return true if this log key name is equal to other log key name, false otherwise
     */
    fun hasTheSameName(other: LogKey) = name == other.name

    override operator fun compareTo(other: LogKey): Int = when {
        order != other.order -> order - other.order
        else -> name.compareTo(other.name)
    }
}
