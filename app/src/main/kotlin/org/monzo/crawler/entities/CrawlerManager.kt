package org.monzo.crawler.entities

import java.util.HashMap
import java.util.logging.Logger

class CrawlerManager {
    val logger = Logger.getLogger(CrawlerManager::class.java.name)

    suspend fun beginCrawl(
        url: String,
        concurrencyCount: Int,
        delay: Double,
    ): MutableMap<String, Set<String>> {
        val delayToLong = (delay * 1000).toLong()
        val siteData = SiteData.create(url, delayToLong)

        val crawler = Crawler()
        val queue = CrawlerQueue()
        val handler = HtmlHandler()
        val emptySiteMap = HashMap<String, Set<String>>()

        logger.info("Crawling...")
        val siteMap = crawler.crawl(siteData, queue, handler, concurrencyCount, emptySiteMap)
        return siteMap
    }
}
