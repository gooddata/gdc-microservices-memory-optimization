/*
 * (C) 2021 GoodData Corporation
 */

package com.gooddata.api.logging

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mu.KLogger
import org.junit.jupiter.api.Test

class KLoggerExtTest {

    @Test
    fun `test build log`() {
        val logger = mockk<KLogger> {
            every { isInfoEnabled } returns true
            every { info(GD_API_MARKER, any(), *anyVararg()) } returns Unit
        }

        logger.logInfo {
            withMessage { "test message" }
            withDetail("detail")
        }

        verify {
            logger.info(
                GD_API_MARKER,
                "test message",
                *varargAll {
                    when (position) {
                        0 -> it == LogKey.DETAIL
                        1 -> it == "detail"
                        else -> false
                    }
                }
            )
        }
    }
}
