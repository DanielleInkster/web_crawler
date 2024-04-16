package org.monzo.crawler

import org.monzo.crawler.entities.CrawlerCLI

class Main {
    companion object{
        @JvmStatic fun main(args: Array<String>) = CrawlerCLI().main(args)
    }
}
