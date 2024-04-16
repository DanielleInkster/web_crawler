package helpers

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.monzo.crawler.helpers.Helpers


class HelpersTest {
    companion object {
        val helper = Helpers
        val validUrl = "https://www.test.com"
        val randomString = "abcde12345"
        val malformedUrl = "https://www.test com"
        val missingProtocols = "www.test.com"
    }

    @Test
    fun`validateUrlString returns true when url is valid`() {
        val response = helper.validateUrlString(validUrl)
        assertTrue { response }
    }

    @Test
    fun`validateUrlString returns false when url is invalid`() {
        val response1 = helper.validateUrlString(randomString)
        val response2 = helper.validateUrlString(malformedUrl)
        val response3 = helper.validateUrlString(missingProtocols)
        assertFalse { response1 }
        assertFalse { response2 }
        assertFalse { response3 }
    }
}
