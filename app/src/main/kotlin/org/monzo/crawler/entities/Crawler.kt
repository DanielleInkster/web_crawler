package org.monzo.crawler.entities

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.math.min

class Crawler(
    val queue: CrawlerQueue,
    val handler: HtmlHandler,
    val concurrencyCount: Int,
) {
    suspend fun crawl(siteData: SiteData): MutableMap<String, Set<String>> =
        coroutineScope {
            //set thread pool
            val executor = Executors.newFixedThreadPool(min(concurrencyCount, 30))
            val dispatcher = executor.asCoroutineDispatcher()
            // Using ConcurrentHashMap for thread safety
            val siteMap = ConcurrentHashMap<String, Set<String>>()
            queue.add(siteData.seedUrl)
            while (queue.isNotEmpty()) {
                // Batching jobs
                val jobs = mutableListOf<Deferred<Unit>>()
                repeat(min(queue.size, concurrencyCount)) {
                    val link = queue.removeFromQueue()
                    jobs.add(
                        async(Dispatchers.IO) {
                            delay(siteData.pPolicy.crawlDelay)
                            val pageResults = handler.parseLinksFromPage(link, siteData.pPolicy.crawlDelay.toInt())
                            siteMap[link] = pageResults
                            if (pageResults.isEmpty()) return@async
                            val queueEntries =
                                validateQueueEntries(siteData.rootUrl, siteData.pPolicy.disallow, pageResults)
                            if (queueEntries.isEmpty()) return@async
                            queue.addToQueue(queueEntries)
                        },
                    )
                }
                // Waiting for the batch to complete
                jobs.awaitAll()
            }
            executor.shutdown()
            siteMap
        }

    private fun validateQueueEntries(
        rootUrl: String,
        disallowed: Disallowed,
        links: HashSet<String>,
    ): List<String> =
        links.filterNot { link ->
            !link.startsWith(rootUrl) ||
                    queue.crawledUrls.contains(link) ||
                    disallowed.strings.any { link.contains(it) } ||
                    disallowed.regexes.any { it.containsMatchIn(link) }
        }
}
