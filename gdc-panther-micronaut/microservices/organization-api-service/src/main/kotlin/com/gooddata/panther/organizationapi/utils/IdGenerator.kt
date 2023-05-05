/*
 * (C) 2023 GoodData Corporation
 */
package com.gooddata.panther.organizationapi.utils

import com.gooddata.panther.common.exception.ServerRuntimeException
import mu.KotlinLogging
import org.apache.commons.codec.digest.DigestUtils
import java.time.Instant
import java.util.Base64

private val logger = KotlinLogging.logger { }

object IdGenerator {
    private const val MAX_RETRIES_ID_GENERATION = 10
    fun generateOrganizationId(
        company: String,
        email: String,
        cluster: String,
        deployment: String
    ): String = retry(
        {
            generateId(
                "${company}${email}${cluster}${deployment}${Instant.now().toEpochMilli()}"
            )
                .let {
                    when (it) {
                        null -> {
                            val e = GenerationException(
                                message = "Failed to generate organization id."
                            )
                            logger.error(e) { "action=organization_id_generation status=failed" }
                            throw e
                        }
                        else -> it
                    }
                }
        }
    )

    private fun generateId(stringForHash: String): String? =
        Base64.getUrlEncoder().encodeToString(
            DigestUtils.md5(stringForHash.toByteArray())
        )
            .lowercase()
            .let { base64 -> "([a-z0-9]([-a-z0-9]{8}[a-z0-9]))".toRegex().find(base64) }?.value

    @Suppress("TooGenericExceptionCaught")
    private fun <T> retry(block: () -> T, maxRetries: Int = MAX_RETRIES_ID_GENERATION): T {
        assert(maxRetries > 0)
        for (i in 1..maxRetries) {
            try {
                return block()
            } catch (e: Exception) {
                logger.info { "action=retry attempt=$i/$maxRetries" }
                logger.debug(e) { "action=retry attempt=$i/$maxRetries" }
                if (i == maxRetries) {
                    logger.error(e) { "action=retry status=failed attempt=$i/$maxRetries" }
                    throw e
                }
            }
        }
        throw IllegalStateException("Unresolved return value!")
    }
}

class GenerationException(message: String) : ServerRuntimeException(message)
