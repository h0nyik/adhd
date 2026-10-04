package app.ulovko.api.aliexpress

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.TreeMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Přihlašovací údaje z AliExpress Open Platform (openservice.aliexpress.com). Patří jen na server. */
data class AeCredentials(
    val appKey: String,
    val appSecret: String,
    /** Tracking ID z affiliate portálu; bez něj odkazy nenesou provizi. */
    val trackingId: String? = null,
    /** Access token uživatele; affiliate dotazy ho nepotřebují. */
    val session: String? = null,
)

/**
 * Podpis požadavku: HMAC-SHA256 tajným klíčem aplikace nad parametry seřazenými podle názvu,
 * spojenými jako název+hodnota bez oddělovačů, výsledek velkými hex znaky.
 * U metod s lomítkem (OAuth, `/auth/token/create`) se cesta metody dává na začátek.
 * Shodné s referenčním SDK ae_sdk (ověřeno testovacími vektory v AeSignerTest).
 */
object AeSigner {
    fun sign(params: Map<String, String>, appSecret: String): String {
        val method = params["method"]
        val apiPath = if (method != null && method.contains('/')) method else ""
        val signed = params.filterKeys { it != "sign" && !(apiPath.isNotEmpty() && it == "method") }
        val base = apiPath + signed.toSortedMap().entries.joinToString("") { it.key + it.value }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(base.toByteArray(Charsets.UTF_8)).joinToString("") { "%02X".format(it) }
    }
}

data class AeHttpResponse(val status: Int, val body: String)

/** HTTP vrstva oddělená kvůli testům (bez sítě) a kvůli případné výměně klienta. */
fun interface AeTransport {
    fun post(url: String): AeHttpResponse
}

class JdkAeTransport(private val timeout: Duration = Duration.ofSeconds(10)) : AeTransport {
    private val client = HttpClient.newBuilder().connectTimeout(timeout).build()

    override fun post(url: String): AeHttpResponse {
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return AeHttpResponse(response.statusCode(), response.body())
    }
}

/** Parametry metody aliexpress.affiliate.product.query, které používáme. */
data class AeProductQuery(
    val keywords: String,
    /** Země doručení: API podle ní filtruje dostupnost a cenu podle daňových pravidel země. */
    val shipToCountry: String,
    /** Podle typů SDK API nenabízí CZK, PLN ani HUF; bereme EUR a převádíme sami. */
    val targetCurrency: String = "EUR",
    /** Čeština mezi jazyky API není; pro Polsko je k dispozici PL. */
    val targetLanguage: String = "EN",
    val pageNo: Int = 1,
    val pageSize: Int = 20,
    /** SALE_PRICE_ASC, SALE_PRICE_DESC, LAST_VOLUME_ASC, LAST_VOLUME_DESC */
    val sort: String? = null,
) {
    init {
        require(keywords.isNotBlank()) { "Prázdný dotaz" }
        require(pageSize in 1..50) { "page_size musí být 1–50" }
        require(pageNo >= 1) { "page_no začíná od 1" }
    }

    fun toParams(trackingId: String?): Map<String, String> = buildMap {
        put("keywords", keywords)
        put("ship_to_country", shipToCountry)
        put("target_currency", targetCurrency)
        put("target_language", targetLanguage)
        put("page_no", pageNo.toString())
        put("page_size", pageSize.toString())
        sort?.let { put("sort", it) }
        trackingId?.let { put("tracking_id", it) }
    }
}

class AeClient(
    private val credentials: AeCredentials,
    private val transport: AeTransport = JdkAeTransport(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        /** Brána pro metody ve tvaru aliexpress.xxx.yyy */
        const val TOP_URL = "https://api-sg.aliexpress.com/sync"
        /** Brána pro metody s lomítkem (/auth/token/create …) */
        const val OP_URL = "https://api-sg.aliexpress.com/rest"
        const val PRODUCT_QUERY = "aliexpress.affiliate.product.query"
    }

    fun buildUrl(method: String, businessParams: Map<String, String>): String {
        val params = TreeMap(businessParams)
        params["method"] = method
        params["app_key"] = credentials.appKey
        params["sign_method"] = "sha256"
        params["timestamp"] = clock().toString()
        params["simplify"] = "true"
        credentials.session?.let { params["session"] = it }
        params["sign"] = AeSigner.sign(params, credentials.appSecret)

        val isOpApi = method.contains('/')
        val base = if (isOpApi) OP_URL + method else TOP_URL
        val query = params.entries
            .filter { !(isOpApi && it.key == "method") }
            .joinToString("&") { "${it.key}=${encode(it.value)}" }
        return "$base?$query"
    }

    suspend fun queryProducts(query: AeProductQuery): AeProductPage {
        val url = buildUrl(PRODUCT_QUERY, query.toParams(credentials.trackingId))
        val response = withContext(Dispatchers.IO) { transport.post(url) }
        if (response.status !in 200..299) {
            throw AeApiException("AliExpress vrátil HTTP ${response.status}")
        }
        return AeResponseParser.parseProductQuery(response.body)
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")
}
