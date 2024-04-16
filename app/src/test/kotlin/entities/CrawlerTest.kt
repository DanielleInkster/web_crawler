package entities

import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.monzo.crawler.entities.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrawlerTest {
    companion object {
        val delay = 1L
        val url = "https://www.test.com"
        val disallowed = Disallowed(hashSetOf("https://www.test.com/5"), hashSetOf("([a-zA-Z0-9_-]+\\?=doNotQuery)".toRegex()))
        val concurrency = 5
        val policy = mockk<PolitenessPolicy>()

        val handler = mockk<HtmlHandler>()
        val queue = CrawlerQueue()
        val crawler = Crawler(queue, handler, concurrency)

        val siteData = SiteData.create(url, delay, policy)

        @JvmStatic
        @BeforeAll
        fun mockResponses() {
            every { policy.crawlDelay } returns delay
            every { policy.disallow } returns disallowed
        }
    }

    @BeforeEach
    fun cleanup() {
        clearMocks(
            handler,
        )
    }

    @Test
    fun`it creates a siteMap`() {
        runTest {
            val links =
                hashSetOf(
                    "https://www.test.com/1",
                    "https://www.test.com/4",
                )
            coEvery { handler.parseLinksFromPage(any<String>(), any<Int>()) } answers {
                links
            }
            val scheduler = testScheduler
            val dispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")
            val results =
                async(dispatcher) {
                    crawler.crawl(siteData)
                }.await()

            // function terminates after all links have been processed
            assertTrue { results.keys.count() == 3 }
            assertTrue {
                results.keys.containsAll(
                    listOf(
                        "https://www.test.com/1",
                        "https://www.test.com/4",
                        "https://www.test.com",
                    ),
                )
            }
            assertEquals(results["https://www.test.com"], links)
        }
    }

    @Test
    fun`All links are processed before terminations`() {
        val newUrl = "https://www.test.com/3"
        val links =
            hashSetOf(
                "https://www.test.com/1",
                "https://www.test.com/4",
            )
        runTest {
            queue.add(newUrl)
            coEvery { handler.parseLinksFromPage(any<String>(), any<Int>()) } answers {
                links
            }
            val scheduler = testScheduler
            val dispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")
            async(dispatcher) {
                crawler.crawl(siteData)
            }.await()

            assertTrue { queue.isEmpty() }
            assertTrue { queue.crawledUrls.containsAll(links.plus(newUrl)) }
        }
    }

    @Test
    fun`it will not crawl disallowed links`() {
        runTest {
            coEvery { handler.parseLinksFromPage(any<String>(), any<Int>()) } answers {
                hashSetOf(
                    "https://www.test.com/1?=doNotQuery",
                    "https://www.test.com/5",
                    "https://www.test.com/2",
                )
            }
            val scheduler = testScheduler
            val dispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")
            val results =
                async(dispatcher) {
                    crawler.crawl(siteData)
                }.await()

            assertTrue { results.keys.count() == 2 }
            assertTrue {
                results.keys.containsAll(
                    listOf(
                        "https://www.test.com",
                        "https://www.test.com/2",
                    ),
                )
            }
            coVerify(exactly = 2) { handler.parseLinksFromPage(any<String>(), any<Int>()) }
        }
    }

    @Test
    fun`it will not crawl external links`() {
        runTest {
            coEvery { handler.parseLinksFromPage(any<String>(), any<Int>()) } answers {
                hashSetOf(
                    "https://www.test2.com",
                )
            }
            val scheduler = testScheduler
            val dispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")
            val results =
                async(dispatcher) {
                    crawler.crawl(siteData)
                }.await()

            assertTrue { results.keys.count() == 1 }
            assertFalse { results.containsKey("https://www.test2.com") }
            assertTrue {
                results.containsKey("https://www.test.com")
            }
            coVerify(exactly = 1) { handler.parseLinksFromPage(any<String>(), any<Int>()) }
        }
    }
}
