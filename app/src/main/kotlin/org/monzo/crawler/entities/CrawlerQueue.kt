package org.monzo.crawler.entities

import java.util.concurrent.locks.ReentrantReadWriteLock

class CrawlerQueue : LinkedHashSet<String>() {
    // set of urls that have already been crawled
    val crawledUrls = hashSetOf<String>()

    // lock for synchronization control
    val queueWriteLock = ReentrantReadWriteLock().writeLock()

    fun addToQueue(urls: List<String>) {
        queueWriteLock.lock()
        try {
            val newUrls = urls.filterNot { crawledUrls.contains(it) }
            this.addAll(newUrls)
        } finally {
            queueWriteLock.unlock()
        }
    }

    fun removeFromQueue(): String {
        queueWriteLock.lock()
        try {
            val linkToProcess = this.removeFirst()
            crawledUrls.add(linkToProcess)
            return linkToProcess
        } finally {
            queueWriteLock.unlock()
        }
    }
}
