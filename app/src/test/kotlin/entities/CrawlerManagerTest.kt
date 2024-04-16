package entities

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.monzo.crawler.entities.CrawlerManager
import org.monzo.crawler.entities.CrawlerQueue
import org.monzo.crawler.entities.HtmlHandler
import kotlin.test.assertTrue


class CrawlerManagerTest {
    companion object{
        val crawlerManager = spyk<CrawlerManager>(recordPrivateCalls = true)
        val concurrency = 5
    }

    @BeforeEach
    fun cleanup() {
        clearMocks(
            crawlerManager,
        )
    }

    @Test
    fun`it creates a crawler with a queue and handler`(){
        every { crawlerManager.createCrawler(any<Int>()) } answers { callOriginal() }
        val crawler = crawlerManager.createCrawler(concurrency)
        assertTrue{ crawler.concurrencyCount == concurrency}
        assertTrue{ crawler.handler is HtmlHandler }
        assertTrue{ crawler.queue is CrawlerQueue }
    }

}