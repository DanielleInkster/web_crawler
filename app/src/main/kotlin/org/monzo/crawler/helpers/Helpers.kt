package org.monzo.crawler.helpers

import java.net.URI
import java.util.logging.Logger

object Helpers {
    private val logger = Logger.getLogger(Helpers::class.java.name)

    fun validateUrlString(potentialUrl: String): Boolean {
        try {
            URI(potentialUrl).toURL()
        } catch (e: Exception) {
            logger.info("$potentialUrl is invalid. Please try again. Error: ${e.message}")
            return false
        }
        return true
    }
}