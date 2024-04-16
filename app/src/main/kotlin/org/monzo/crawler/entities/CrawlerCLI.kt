package org.monzo.crawler.entities

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.restrictTo
import kotlinx.coroutines.runBlocking
import org.monzo.crawler.helpers.Helpers
import kotlin.time.measureTime

class CrawlerCLI : CliktCommand() {
    val helper = Helpers
    val seedUrl: String by option().prompt(
        "Enter seed url",
    ).help("The url string to start crawl.").validate { require(helper.validateUrlString(it) == true) { "See info message." } }
    val concurrency: Int by option().int().restrictTo(min = 1, max = 100).prompt(
        "Enter concurrency per crawler",
    ).help("Rate of concurrency per crawler. Between 1 and 100")
    val delay: Double by option().double().restrictTo(min = 0.00, max = 200.00).prompt(
        "Set fallback crawl delay in seconds and milliseconds",
    ).help("Rate of delay between page crawls if not set by host. Between 0.00-200.00")

    override fun run() {
        val manager = CrawlerManager()

        fun startCrawl() =
            runBlocking {
                manager.beginCrawl(seedUrl, concurrency, delay)
            }

        val duration =
            measureTime {
                startCrawl()
            }

        return println("\nCompleted in $duration seconds.")
    }
}
