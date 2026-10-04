package app.ulovko.api

import app.ulovko.api.aliexpress.AeApiException
import app.ulovko.api.aliexpress.AeClient
import app.ulovko.api.aliexpress.AeCredentials
import app.ulovko.api.offers.AliExpressFixtureSource
import app.ulovko.api.offers.AliExpressLiveSource
import app.ulovko.api.offers.OfferSource
import app.ulovko.api.rates.EcbHttpRatesProvider
import app.ulovko.api.rates.RatesProvider
import app.ulovko.api.rates.StaticRatesProvider
import app.ulovko.api.search.SearchService
import app.ulovko.core.fx.EcbRatesParser
import app.ulovko.core.market.Market
import app.ulovko.core.rules.RuleSet
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.time.ZoneId

@Serializable
data class ApiError(val error: String, val message: String, val requestId: String? = null)

@Serializable
data class Health(val status: String, val source: String, val rulesVersion: String)

/** Vše, co API potřebuje; v testech se skládá ručně. */
class Services(
    val source: OfferSource,
    val rates: RatesProvider,
    val rules: RuleSet = RuleSet.DEFAULT,
    val today: () -> LocalDate = { java.time.LocalDate.now(ZoneId.of("Europe/Prague")).let { LocalDate(it.year, it.monthValue, it.dayOfMonth) } },
) {
    val search = SearchService(source, rates, app.ulovko.core.pricing.PriceEngine(rules), today)

    companion object {
        private val log = LoggerFactory.getLogger(Services::class.java)

        /**
         * Bez přístupových údajů k AliExpressu běží vývojový režim: smyšlené produkty a kurzy,
         * obojí v odpovědi přiznané (source = aliexpress-fixture, varování rates.sample_not_real).
         */
        fun fromEnvironment(env: Map<String, String> = System.getenv()): Services {
            val appKey = env["AE_APP_KEY"]?.takeIf { it.isNotBlank() }
            val appSecret = env["AE_APP_SECRET"]?.takeIf { it.isNotBlank() }
            val source: OfferSource = if (appKey != null && appSecret != null) {
                AliExpressLiveSource(AeClient(AeCredentials(appKey, appSecret, env["AE_TRACKING_ID"], env["AE_SESSION"])))
            } else {
                log.warn("AE_APP_KEY / AE_APP_SECRET nejsou nastavené: běží vývojový režim se smyšlenými produkty")
                AliExpressFixtureSource(resource("/fixtures/aliexpress-product-query.json"))
            }
            val rates: RatesProvider = when (val setting = env["ULOVKO_RATES"] ?: if (appKey == null) "sample" else "ecb") {
                "ecb" -> EcbHttpRatesProvider()
                "sample" -> StaticRatesProvider(
                    EcbRatesParser.parse(resource("/dev/ecb-sample-not-real.xml"), source = "SAMPLE – smyšlené kurzy pro vývoj"),
                    isSample = true,
                )
                else -> StaticRatesProvider(EcbRatesParser.parse(File(setting).readText()))
            }
            val rules = env["ULOVKO_RULES"]?.let { RuleSet.fromJson(File(it).readText()) } ?: RuleSet.DEFAULT
            return Services(source, rates, rules)
        }

        private fun resource(path: String): String =
            requireNotNull(Services::class.java.getResource(path)) { "Chybí resource $path" }.readText()
    }
}

val apiJson = Json {
    encodeDefaults = true
    explicitNulls = false
}

fun Application.module(services: Services) {
    install(ContentNegotiation) { json(apiJson) }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ApiError("bad_request", cause.message ?: "Neplatný požadavek"))
        }
        exception<AeApiException> { call, cause ->
            call.respond(HttpStatusCode.BadGateway, ApiError("aliexpress", cause.message ?: "Chyba AliExpressu", cause.requestId))
        }
        exception<Throwable> { call, cause ->
            this@module.environment.log.error("Neošetřená chyba", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError("internal", "Něco se pokazilo na naší straně."))
        }
    }
    routing {
        get("/health") {
            call.respond(Health("ok", services.source.name, services.rules.version))
        }
        get("/v1/rules") {
            call.respond(services.rules)
        }
        get("/v1/search") {
            val params = call.request.queryParameters
            val market = Market.fromCode(params["market"] ?: "CZ")
                ?: throw IllegalArgumentException("Neznámý trh. Podporované: ${Market.entries.joinToString { it.countryCode }}")
            val page = params["page"]?.toIntOrNull() ?: 1
            val speed = params["speed"]?.toDoubleOrNull() ?: 0.4
            call.respond(services.search.search(params["q"].orEmpty(), market, page, speed))
        }
    }
}

fun main() {
    val services = Services.fromEnvironment()
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) { module(services) }.start(wait = true)
}
