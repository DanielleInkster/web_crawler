
# Web Crawler

### Task description ###

<i>We'd like you to write a simple web crawler in a programming language you're familiar with. Given a starting URL, the crawler should visit each URL it finds on the same domain. It should print each URL visited, and a list of links found on that page. The crawler should be limited to one subdomain - so when you start with https://monzo.com/, it would crawl all pages on the monzo.com website, but not follow external links, for example to facebook.com or community.monzo.com.

We would like to see your own implementation of a web crawler. Please do not use frameworks like scrapy or go-colly which handle all the crawling behind the scenes or someone else's code. You are welcome to use libraries to handle things like HTML parsing.

Ideally, write it as you would a production piece of code. This exercise is not meant to show us whether you can write code – we are more interested in how you design software. This means that we care less about a fancy UI or sitemap format, and more about how your program is structured: the trade-offs you've made, what behaviour the program exhibits, and your use of concurrency, test coverage, and so on.
</i>
---
### Dependencies ###
 - Gradle 8.7
 - Kotlin 1.9.23
 - Kotlin Coroutines Core 1.8.0
 - Clikt 4.2.2
 - JSoup 1.1.72

#### Testing Dependencies ####
 - JUnit 5
 - Mockk 1.1.30

### Setup ###

 - Run `gradle build` to create JAR
 - Run `java -jar app.jar` in `app/build/libs`


### Usage ###

- When prompted, enter a URL.
- When prompted, enter concurrency (Int, max. 100)
- When prompted, enter fallback delay value if crawl delay cannot be obtained from robots.txt (Double, max. 200.00)
- Sitemap will be printed in command line upon completion, as well execution time in seconds. 

*** Note that a high amount of concurrency with a low amount of delay can result in reduced performance due to issues like server overload. 

---

### How it works ###

1. User sets seed url, concurrency and fallback delay. Urls are validated to ensure they are syntactically correct and well-formed. 


2. If found, robots.txt file is fetched and parsed for User-Agent * rules to create the site's politeness policy. If present, crawl-delay is set and rules are parsed into strings or regexes if they include `*`.
   - If not found, a default politeness policy will be set.
   - If crawling is forbidden by host, program will terminate
   - If seed url is in disallowed, program will terminate.


3. Crawler is created with a CrawlerQueue, HtmlHandler and concurrencyCount(Int). Seed url is added to queue.
      - HtmlHandler uses Jsoup for parsing.
      - Queue is a linkedHashSet for urls to be crawled with a secondary hashSet for urls that have been crawled.


4. Crawler begins crawling site; current thread pool is limited to 30 or the concurrency value, whichever is less. The url is removed from the queue and passed to the HtmlHandler. This queries urls and returns links - link fragments and queries are removed. 
   - Crawler uses an asynchronous coroutine to query.
   - For timeouts, HtmlHandler uses exponential backoff for retries; program will terminate after 3 failed tries and error will be logged. 
   - If the url returns an HttpStatus exception, program will terminate and error will be logged. 
   

5. Links returned by the HtmlHandler are stored as values in the Site Map (HashMap) with the queried url as key. 


6. Links are filtered to internal links that are not on the disallowed list and haven't already been processed. If the links set is empty, the cycle terminates


7. Queue uses a ReentrantReadWriteLock as a synchronization tool for adding and removing links. Links are added to queue and crawledUrl queue. The crawledUrl queue is used for fast lookup of links already crawled.


8. Link is removed from the queue and the cycle is repeated.


9. When the queue is empty, the crawl function is terminated.


10. The site map is returned to the CrawlerManager.


11. Due to the nature of this test, the site map is formatted and printed to the console, including execution time. 