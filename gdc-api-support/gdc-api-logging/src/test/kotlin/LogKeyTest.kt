/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue

class LogKeyTest {

    @Test
    fun `key equality`() {
        expectThat(LogKey("detail", LogKey.LOW_PRIORITY)).isEqualTo(LogKey.DETAIL)
        expectThat(LogKey("new")).isEqualTo(LogKey("new"))
        expectThat(LogKey("new")).get { hasTheSameName(LogKey("new", 100)) }.isTrue()
    }

    @Test
    fun `key non equality`() {
        expectThat(LogKey("Detail")).isNotEqualTo(LogKey.DETAIL)
        expectThat(LogKey("new", 100)).isNotEqualTo(LogKey("new", 500))
        expectThat(LogKey("new")).get { hasTheSameName(LogKey("new2")) }.isFalse()
    }

    @Test
    fun `key order`() {
        val action2 = LogKey("detail2", LogKey.DETAIL.order)
        val keys = listOf(LogKey.MESSAGE, LogKey.TIMESTAMP, action2, LogKey.EXCEPTION, LogKey.DETAIL)
        expectThat(keys.sorted())
            .containsExactly(LogKey.TIMESTAMP, LogKey.MESSAGE, LogKey.DETAIL, action2, LogKey.EXCEPTION)
    }
}
