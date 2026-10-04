package app.ulovko.api.rates

import app.ulovko.core.fx.EcbRatesParser
import app.ulovko.core.fx.ExchangeRates
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

interface RatesProvider {
    /** Aktuální kurzy, nebo null, když nejsou k dispozici (ceny se pak označí jako neúplné). */
    fun current(): ExchangeRates?

    /** Smyšlené kurzy pro vývoj. API to přizná ve varování. */
    val isSample: Boolean get() = false
}

class StaticRatesProvider(private val rates: ExchangeRates?, override val isSample: Boolean = false) : RatesProvider {
    override fun current() = rates
}

/** Stáhne text z URL; vrací null při chybě nebo jiném stavu než 2xx. */
fun interface TextFetcher {
    fun fetch(url: String): String?
}

class JdkTextFetcher(private val timeout: Duration = Duration.ofSeconds(10)) : TextFetcher {
    private val client = HttpClient.newBuilder().connectTimeout(timeout).build()

    override fun fetch(url: String): String? {
        val request = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body().takeIf { response.statusCode() in 200..299 }
    }
}

/** Denní referenční kurzy ECB s mezipamětí. Při výpadku vrací poslední úspěšně načtené. */
class EcbHttpRatesProvider(
    private val fetcher: TextFetcher = JdkTextFetcher(),
    private val url: String = ECB_DAILY_URL,
    private val ttl: Duration = Duration.ofHours(6),
    private val now: () -> Instant = Instant::now,
) : RatesProvider {
    companion object {
        const val ECB_DAILY_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
        private val log = LoggerFactory.getLogger(EcbHttpRatesProvider::class.java)
    }

    private var cached: ExchangeRates? = null
    private var fetchedAt: Instant = Instant.EPOCH

    @Synchronized
    override fun current(): ExchangeRates? {
        if (cached != null && now().isBefore(fetchedAt.plus(ttl))) return cached
        try {
            val body = fetcher.fetch(url)
            if (body != null) {
                cached = EcbRatesParser.parse(body)
                fetchedAt = now()
            } else {
                log.warn("Kurzy ECB nejsou dostupné ({})", url)
            }
        } catch (e: Exception) {
            log.warn("Kurzy ECB se nepodařilo načíst: {}", e.message)
        }
        return cached
    }
}
