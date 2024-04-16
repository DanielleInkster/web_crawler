package entities

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.monzo.crawler.entities.CrawlerQueue

class CrawlerQueueTest {
    companion object {
        val queue = CrawlerQueue()
        val crawlerQueue = queue.crawledUrls
    }

    @BeforeEach
    fun resetQueues() {
        queue.clear()
        crawlerQueue.clear()
        queue.add("www.test.com")
    }

    @Test
    fun`urls can be added to the queue`() {
        assertTrue { queue.size == 1 }
        assertTrue { queue.contains("www.test.com") }
    }

    @Test
    fun`it creates hashSet for processed urls`() {
        queue.removeFromQueue()
        assertTrue { queue.crawledUrls.count() == 1 }
        assertTrue { queue.crawledUrls.contains("www.test.com") }
    }

    @Test
    fun`urls are removed from the queue but not from processedUrls set `() {
        assertTrue { queue.size == 1 }
        assertTrue { queue.crawledUrls.size == 0 }
        assertTrue { queue.contains("www.test.com") }
        assertFalse { queue.crawledUrls.contains("www.test.com") }

        queue.removeFromQueue()
        queue.add("www.test.com/1")
        queue.removeFromQueue()

        assertTrue { queue.size == 0 }
        assertFalse { queue.contains("www.test.com") }
        assertFalse { queue.contains("www.test.com/1") }
        assertTrue { queue.crawledUrls.count() == 2 }
        assertTrue { queue.crawledUrls.containsAll(listOf("www.test.com", "www.test.com/1")) }
    }
}
