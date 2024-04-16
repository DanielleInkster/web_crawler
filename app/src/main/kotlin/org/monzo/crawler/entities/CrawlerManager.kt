package org.monzo.crawler.entities

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

        val crawler = createCrawler(concurrencyCount)

        logger.info("Crawling...")
        val siteMap = crawler.crawl(siteData)
        return siteMap
    }

    fun createCrawler(
        concurrencyCount: Int
    ):Crawler {
        val queue = CrawlerQueue()
        val handler = HtmlHandler()
        return Crawler(queue, handler, concurrencyCount)
    }
}
