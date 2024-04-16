package entities

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.spyk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.monzo.crawler.entities.Disallowed
import org.monzo.crawler.entities.PolitenessPolicy
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PolitenessPolicyTest {
    companion object {
        val testUrl1 = "https://www.test1.com"
        val testUrl2 = "https://www.test2.com"
        val fallbackDelay = 1L

        val mockkedEmptyResponse =
            mutableSetOf<String?>()

        val mockkedRobotsTxtResponse =
            mutableSetOf<String?>(
                "Crawl-delay: 5",
                "Disallow: /drafts/",
                "Disallow: *?s=bpage-next",
            )

        val crawlingProhibited =
            mutableSetOf<String?>(
                "Disallow: /",
            )
        val pPolicyCompanion = spyk<PolitenessPolicy.Companion>(recordPrivateCalls = true)
    }

    @BeforeEach
    fun cleanup() {
        clearMocks(
            pPolicyCompanion,
        )
    }

    @Test
    fun `it uses default PolitenessPolicy values when robotstxt can't be found`() {
        every { pPolicyCompanion["fetchRobotsFile"](any<String>()) } returns mockkedEmptyResponse
        every { pPolicyCompanion.create(testUrl1, testUrl1, fallbackDelay) } answers { callOriginal() }

        val policy = pPolicyCompanion.create(testUrl1, testUrl1, fallbackDelay)
        assertEquals(fallbackDelay, policy.crawlDelay)
        assertEquals(Disallowed(hashSetOf(), hashSetOf()), policy.disallow)
    }

    @Test
    fun `it uses host crawl delay value when robotstxt is found`() {
        every { pPolicyCompanion["fetchRobotsFile"](any<String>()) } returns mockkedRobotsTxtResponse
        every { pPolicyCompanion.create(testUrl2, testUrl2, fallbackDelay) } answers { callOriginal() }

        val policy = pPolicyCompanion.create(testUrl2, testUrl2, fallbackDelay)
        assertEquals(5000L, policy.crawlDelay)
    }

    @Test
    fun `it creates sets of host disallow rules as strings and regexes when robotstxt is found`() {
        every { pPolicyCompanion["fetchRobotsFile"](any<String>()) } returns mockkedRobotsTxtResponse
        every { pPolicyCompanion.create(testUrl2, testUrl2, fallbackDelay) } answers { callOriginal() }

        val policy = pPolicyCompanion.create(testUrl2, testUrl2, fallbackDelay)
        println(policy.disallow)
        val expectedString = "https://www.test2.com/drafts"
        val expectedRegex = "[a-zA-Z0-9_-]+\\?s=bpage-next"
        assertTrue(
            policy.disallow.strings.contains(expectedString),
        )
        assertEquals(
            expectedRegex,
            policy.disallow.regexes.first().toString(),
        )
    }

    @Test
    fun `it should throw an exception when site doesn't allow crawling`() {
        every { pPolicyCompanion["fetchRobotsFile"](any<String>()) } returns crawlingProhibited
        every { pPolicyCompanion.create(testUrl2, testUrl2, fallbackDelay) } answers { callOriginal() }
        assertFailsWith<Exception> { pPolicyCompanion.create(testUrl2, testUrl2, fallbackDelay) }
    }
}
