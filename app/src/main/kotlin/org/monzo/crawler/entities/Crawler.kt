package org.monzo.crawler.entities

import kotlinx.coroutines.*
import kotlin.math.min

class Crawler() {
    suspend fun crawl(
        siteData: SiteData,
        queue: CrawlerQueue,
        handler: HtmlHandler,
        concurrencyCount: Int,
        siteMap: HashMap<String, Set<String>>
    ): MutableMap<String, Set<String>> =
        coroutineScope {
            // Using ConcurrentHashMap for thread safety
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
                            // for the purposes of this tech test so results can be seen in the CLI
                            println(
                                "\nUrl: $link\nUnique links found:\n • ${pageResults.joinToString("\n • ")}",
                            )
                            if (pageResults.isEmpty()) return@async
                            val queueEntries =
                                validateQueueEntries(siteData.rootUrl, siteData.pPolicy.disallow, pageResults, queue)
                            if (queueEntries.isEmpty()) return@async
                            queue.addToQueue(queueEntries)
                        },
                    )
                }
                // Waiting for the batch to complete
                jobs.awaitAll()
            }
            siteMap
        }

    private fun validateQueueEntries(
        rootUrl: String,
        disallowed: Disallowed,
        links: HashSet<String>,
        queue: CrawlerQueue,
    ): List<String> =
        links.filterNot { link ->
            !link.startsWith(rootUrl) ||
                    queue.crawledUrls.contains(link) ||
                    disallowed.strings.any { link.contains(it) } ||
                    disallowed.regexes.any { it.containsMatchIn(link) }
        }
}
