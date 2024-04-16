package entities

import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.monzo.crawler.entities.HtmlHandler

class HtmlHandlerTest {
    companion object {
        val handler = spyk<HtmlHandler>(recordPrivateCalls = true)
        val url = "https://www.test.com"
        val expected = setOf("https://www.test-site.com/auth/signin", "https://www.test-site.com/community/about")
        val htmlLinkString =
            """
             div class="footer-community_buttonsContainer__7ET3f">
             <ul>
             <li>
            <a class="footer-community_loginBtn__D4WsJ" target="" href="https://www.test-site.com/auth/signin" rel="noreferrer">
            </li>
            </ul>
            <p>Sign up to get started</p></a><a class="footer-community_promoLink__8jMwF" target="_blank" href="https://www.test-site.com/community/about" rel="noreferrer">Learn more about Community</a>
            </div>
            """.trimIndent()

        val fragsAndQueriesLinkString =
            """
             div class="footer-community_buttonsContainer__7ET3f">
             <ul>
             <li>
            <a class="footer-community_loginBtn__D4WsJ" target="" href="https://www.test-site.com/auth/signin?oauth=true" rel="noreferrer">
            </li>
            </ul>
            <p>Sign up to get started</p></a><a class="footer-community_promoLink__8jMwF" target="_blank" href="https://www.test-site.com/community/about#4?variant-blue-1" rel="noreferrer">Learn more about Community</a>
            </div>
            """.trimIndent()
    }

    @BeforeEach
    fun cleanup() {
        clearMocks(
            handler,
        )
    }

    @Test
    fun`it returns absolute links from page`() =
        runTest {
            every { handler["fetchPageAsDocument"](any<String>(), any<Int>()) } answers { Jsoup.parse(htmlLinkString) }
            val scheduler = testScheduler
            val dispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")
            val results =
                async(dispatcher) {
                    handler.parseLinksFromPage(url, 0)
                }.await()
            assertTrue { results.containsAll(expected) }
        }

    @Test
    fun`it returns links with fragments and queries removed`() =
        runTest {
            every { handler["fetchPageAsDocument"](any<String>(), any<Int>()) } answers { Jsoup.parse(fragsAndQueriesLinkString) }
            val scheduler = testScheduler
            val dispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")
            val results =
                async(dispatcher) {
                    handler.parseLinksFromPage(url, 0)
                }.await()
            assertTrue { results.containsAll(expected) }
        }

    @Test
    fun`it retries 3 times when response times out`() =
        runTest {
            every {
                handler["jsoupQuery"](
                    any<String>(),
                )
            } answers { null }
            val scheduler = testScheduler
            val dispatcher = StandardTestDispatcher(scheduler, name = "IO dispatcher")
            val results =
                async(dispatcher) {
                    handler.parseLinksFromPage(url, 0)
                }.await()

            coVerify(exactly = 3) { handler["jsoupQuery"](any<String>()) }
        }
}
