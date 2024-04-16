package org.monzo.crawler.entities

import kotlinx.coroutines.*
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.net.http.HttpConnectTimeoutException
import java.util.logging.Logger


private const val DEFAULT_TRIES = 3
private const val DEFAULT_TIMEOUT = 5
private val DEFAULT_USER_AGENT = "*"

class HtmlHandler {
    private val logger = Logger.getLogger(HtmlHandler::class.java.name)

    suspend fun parseLinksFromPage(
        url: String,
        delay: Int,
    ): HashSet<String> = coroutineScope {
        withContext(Dispatchers.IO) {
            val doc = fetchPageAsDocument(url, delay)
            getAllAbsoluteLinks(doc).toHashSet()
        }
    }

    private suspend fun fetchPageAsDocument(
        url: String,
        delay: Int,
    ): Document? {
        var doc: Document? = null

        // Exponential backoff for retries
        val retryDelay = if (delay == 0) DEFAULT_TIMEOUT else delay
        val retryBackoff = { i: Int -> (retryDelay * i).toLong() }

        repeat(DEFAULT_TRIES) { attempt ->
            try {
                doc = jsoupQuery(url)
                // Break out of repeat if successful
                return@repeat
            } catch (e: SocketTimeoutException) {
                if (attempt < DEFAULT_TRIES - 1) {
                    delay(retryBackoff(attempt))
                    logger.warning("Unable to fetch $url. Timeout occurred ${attempt + 1} time(s). Retrying...")
                } else {
                    logger.warning("Unable to fetch $url. Timeout occurred ${attempt + 1} time(s). Error: $e")
                    throw HttpConnectTimeoutException(
                        "Unable to fetch $url. Timeout occurred ${attempt + 1} time(s). Terminating crawl. Error: ${e.message}",
                    )
                }
            } catch (e: HttpStatusException) {
                if (e.statusCode == 429) {
                    throw SocketTimeoutException()
                } else {
                    logger.warning("Unable to fetch $url. Terminating crawl. Error: $e")
                    throw HttpStatusException(e.message!!, e.statusCode, e.url)
                }
            } catch (e: Exception) {
                logger.warning("Terminating crawl. Error: $e")
                throw Exception(e)
            }
        }
        return doc
    }

    private fun jsoupQuery(url: String) =
        Jsoup.connect(url)
            .userAgent(DEFAULT_USER_AGENT)
            .ignoreContentType(true)
            .timeout(DEFAULT_TIMEOUT * 1000)
            .get()

    private fun getAllAbsoluteLinks(doc: Document?): Set<String> {
        val links = HashSet<String>()
        doc?.let {
            val elements = doc.select("a[href]")
            links.addAll(elements.map { removeQueriesAndFragments(it.attr("abs:href")) })
        }
        return links
    }

    private fun removeQueriesAndFragments(link: String): String = link.substringBefore('?').substringBefore('#')
}
