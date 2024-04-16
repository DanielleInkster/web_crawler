package org.monzo.crawler.entities

import java.io.IOException
import java.net.URI
import java.net.UnknownHostException
import java.util.logging.Logger

private const val DEFAULT_USER_AGENT = "*"
private val DEFAULT_DISALLOWED = Disallowed(hashSetOf(), hashSetOf())
private val NO_CRAWLING = "/"

data class PolitenessPolicy(
    val crawlDelay: Long,
    val disallow: Disallowed,
) {
    companion object {
        private val logger = Logger.getLogger(PolitenessPolicy::class.java.name)

        fun create(
            rootUrl: String,
            seedUrl: String,
            fallbackDelay: Long,
        ): PolitenessPolicy {
            val rules = fetchRobotsFile(rootUrl).filterNotNull().toSet()

            return if (rules.isEmpty()) {
                logger.info("Crawl delay set to $fallbackDelay ms.")
                PolitenessPolicy(
                    crawlDelay = fallbackDelay,
                    disallow = DEFAULT_DISALLOWED,
                )
            } else {
                val crawlDelay = getHostCrawlDelay(rules)?.times(1000)?.toLong() ?: fallbackDelay
                logger.info("Crawl delay set to $crawlDelay ms.")
                val disallow = getDisallowedPathsAndQueries(rules, rootUrl, seedUrl)
                PolitenessPolicy(crawlDelay = crawlDelay, disallow = disallow)
            }
        }

        private fun fetchRobotsFile(url: String): MutableSet<String?> {
            return try {
                val rules: MutableSet<String?> = mutableSetOf()
                URI("$url/robots.txt").toURL().openStream().use { stream ->
                    stream.bufferedReader().useLines { lines ->
                        var insideBlock = false
                        lines.forEach { line ->
                            when {
                                line.startsWith("User-agent: $DEFAULT_USER_AGENT") -> insideBlock = true
                                insideBlock && line.startsWith("User-agent:") -> return@useLines
                                insideBlock -> rules.add(line)
                            }
                        }
                    }
                }
                rules
            } catch (e: IOException) {
                if (e is UnknownHostException) {
                    throw UnknownHostException("Unable to reach url $url.")
                } else {
                    logger.warning(
                        "Unable to locate and/or process robots.txt file for $url. " +
                                "Error: $e. Proceeding with default Politeness Policy.",
                    )
                }
                return mutableSetOf()
            }
        }

        private fun getHostCrawlDelay(rules: Set<String>): Double? {
            val prefix = "crawl-delay"
            val hostDelay = rules.find { it.startsWith(prefix, true) }
            return hostDelay?.substringAfter(":", "")?.trim()?.toDoubleOrNull()
        }

        // converts robots.txt disallowed values to regexes for matching
        // terminates if crawling is disallowed by host or seed url is on disallowed list.
        private fun getDisallowedPathsAndQueries(
            rules: Set<String>,
            rootUrl: String,
            seedUrl: String,
        ): Disallowed {
            val disallowedStrings = HashSet<String>()
            val disallowedRegexes = HashSet<Regex>()
            rules.filter { it.startsWith("disallow", true) }
                .ifEmpty { return DEFAULT_DISALLOWED }
                .forEach {
                    val rule = it.substringAfter(":").trim().removeSuffix("/")
                    if (rule == NO_CRAWLING) {
                        throw Exception("This site does not allow crawling. Terminating.")
                    }

                    when {
                        // creates regex to account for wildcard
                        rule.contains("*") -> disallowedRegexes.add(createRegex(rule, rootUrl))
                        // removes unnecessary regex symbols
                        rule.startsWith("/") -> disallowedStrings.add(rootUrl + rule.removeSuffix("$"))
                        else -> disallowedStrings.add(rule)
                    }
                }
            if (disallowedRegexes.any { it.matches(seedUrl) } || disallowedStrings.any { seedUrl.contains(it) }) {
                throw Exception("Seed url is marked disallowed for crawling by host. Terminating.")
            }
            return Disallowed(strings = disallowedStrings, regexes = disallowedRegexes)
        }

        // converts disallowed elements into regexes for matching
        private fun createRegex(
            rule: String,
            rootUrl: String,
        ): Regex {
            val rootUrlRegex = "^${rootUrl.removeSuffix("/")}".replace(".", "\\.")
            val processedPath =
                rule
                    .removeSuffix("/")
                    .replace(".", "\\.")
                    .replace("?", "\\?")
                    .replace("*", "[a-zA-Z0-9_-]+")
                    .replace("/", "\\/")

            return if (processedPath.startsWith("\\/")) {
                "$rootUrlRegex$processedPath".toRegex()
            } else {
                processedPath.toRegex()
            }
        }
    }
}

data class Disallowed(
    val strings: HashSet<String>,
    val regexes: HashSet<Regex>,
)
