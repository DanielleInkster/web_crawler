package org.monzo.crawler.entities

import java.net.URI
import java.net.URL

data class SiteData(
    val seedUrl: String,
    val rootUrl: String,
    val pPolicy: PolitenessPolicy,
) {
    companion object {
        fun create(
            url: String,
            delay: Long,
            pPolicy: PolitenessPolicy? = null,
        ): SiteData {
            val rootUrl = createRootUrl(URI(url).toURL())
            val policy = pPolicy ?: PolitenessPolicy.create(rootUrl, url, delay)
            return SiteData(url, rootUrl, policy)
        }

        private fun createRootUrl(url: URL): String = url.protocol + "://" + url.host
    }
}
