/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimedValue

@OptIn(ExperimentalTime::class)
class MeasuredInvocationTest {

    @Test
    fun `measureCatching success`() {
        val timedResult = measureCatching { "hello world" }
        expectThat(timedResult.value.isSuccess).isTrue()
        val operations = mockTimedResultBase(timedResult)

        timedResult.onSuccess { operations.onSuccess() }.onFailure { operations.onFailure() }
        verify(exactly = 1) { operations.onSuccess() }
        verify(exactly = 0) { operations.onFailure() }
    }

    @Test
    fun `measureCatching failure`() {
        val timedResult = measureCatching { throwTestException() }
        expectThat(timedResult.value.isFailure).isTrue()
        val operations = mockTimedResultBase(timedResult)

        timedResult.onSuccess { operations.onSuccess() }.onFailure { operations.onFailure() }
        verify(exactly = 0) { operations.onSuccess() }
        verify(exactly = 1) { operations.onFailure() }
    }

    private fun throwTestException(): String = throw TestException("error")

    private fun mockTimedResultBase(timedResult: TimedValue<Result<String>>): TestOperations {
        expectThat(timedResult.duration).isGreaterThan(Duration.ZERO)
        return mockk() {
            every { onSuccess() } just Runs
            every { onFailure() } just Runs
        }
    }
}

private class TestException(msg: String) : Exception(msg)

private interface TestOperations {
    fun onSuccess()
    fun onFailure()
}
