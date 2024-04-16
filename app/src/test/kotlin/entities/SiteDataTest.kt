package entities

import org.monzo.crawler.entities.Disallowed
import org.monzo.crawler.entities.PolitenessPolicy
import org.monzo.crawler.entities.SiteData


import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SiteDataTest {
    companion object {
        val defaultDelay = 1L
        val veryLongUrl = "https://www.test.com/aa/bb/cc?page[offset]=1"
    }

    @Test
    fun `it returns only the baseurl of any url`() {
        val siteData = SiteData.create(veryLongUrl, defaultDelay)
        assertEquals("https://www.test.com", siteData.rootUrl)
    }

    // create function
    @Test
    fun `it creates a new Site entity when url and policy are valid`() {
        val pPolicy = mockk<PolitenessPolicy>()
        every { pPolicy.crawlDelay } returns defaultDelay
        every { pPolicy.disallow } returns Disallowed(strings = hashSetOf("/not-allowed", "/definitely-not-allowed"), regexes = hashSetOf())
        val site = SiteData.create(veryLongUrl, defaultDelay, pPolicy)

        assertEquals(veryLongUrl, site.seedUrl)
        assertEquals("https://www.test.com", site.rootUrl)
        assertEquals(pPolicy, site.pPolicy)
    }
}
