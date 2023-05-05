/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class LogBuilderTest {

    @Test
    fun `withException call`() {
        val logBuilder = LogBuilder()
        val exception = ArithmeticException()
        logBuilder.withException(exception)
        val expectedArguments = arrayOf<Any>(LogKey.EXCEPTION, exception)
        expectThat(logBuilder.prepareArguments()).isEqualTo(expectedArguments)
    }

    @Test
    fun `withKey call`() {
        val logBuilder = LogBuilder()
        val testKey = LogKey("test", LogKey.LOW_PRIORITY)
        val secretKey = LogKey("secret")
        logBuilder.withKey(testKey, "test")
        logBuilder.withKey(secretKey, "secret")
        val expectedArguments = arrayOf<Any>(testKey, "test", secretKey, "secret")
        expectThat(logBuilder.prepareArguments()).isEqualTo(expectedArguments)
    }

    @Test
    fun `no logger activity`() {
        val logger = mockk<Logger> {
            every { isInfoEnabled } returns false
        }
        val logBuilder = LogBuilder()
        logBuilder.writeTo(logger)
        verifyAll { logger.isInfoEnabled }
    }

    @ExperimentalTime
    @Test
    fun `complex log event`() {
        val logger = mockk<Logger> {
            every { isInfoEnabled } returns true
            every { info(GD_API_MARKER, any<String>(), *anyVararg()) } returns Unit
        }
        LogBuilder().run {
            withMessage { "simple message" }
            withDetail("detail")
            withDuration(Duration.ZERO)
            withDurationMs(10L)
            writeTo(logger)
        }
        val expectedArguments = arrayOf<Any>(
            LogKey.DETAIL,
            "detail",
            LogKey.DURATION,
            Duration.ZERO,
            LogKey.DURATION_MS,
            10L,
        )
        verify { logger.info(GD_API_MARKER, "simple message", *expectedArguments) }
    }
}
