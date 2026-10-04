package app.ulovko.api

import app.ulovko.api.aliexpress.AeProduct
import app.ulovko.api.offers.AliExpressFixtureSource
import app.ulovko.api.offers.AliExpressOfferMapper
import app.ulovko.api.offers.Assumption
import app.ulovko.api.rates.EcbHttpRatesProvider
import app.ulovko.api.rates.StaticRatesProvider
import app.ulovko.api.search.SearchResponse
import app.ulovko.core.fx.ExchangeRates
import app.ulovko.core.money.Currency
import app.ulovko.core.money.Money
import app.ulovko.core.pricing.Confidence
import app.ulovko.core.pricing.LineKind
import app.ulovko.core.pricing.LineStatus
import app.ulovko.core.pricing.Reason
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDate
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val TEST_RATES = ExchangeRates(LocalDate(2026, 10, 2), "test (smyšlené kurzy)", mapOf(Currency.CZK to 25.0, Currency.PLN to 4.0, Currency.HUF to 400.0))

private fun fixtureJson() = requireNotNull(SearchApiTest::class.java.getResource("/fixtures/aliexpress-product-query.json")).readText()

private fun testServices(today: LocalDate = LocalDate(2026, 10, 4)) = Services(
    source = AliExpressFixtureSource(fixtureJson()),
    rates = StaticRatesProvider(TEST_RATES),
    today = { today },
)

class SearchApiTest {

    @Test
    fun searchReturnsOffersWithFullPriceBreakdownInMarketCurrency() = testApplication {
        application { module(testServices()) }

        val response = client.get("/v1/search?q=sluchátka&market=CZ")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = apiJson.decodeFromString(SearchResponse.serializer(), response.bodyAsText())

        assertEquals("aliexpress-fixture", body.source)
        assertEquals(2, body.offers.size)
        val first = body.offers.first { it.id == "ae:9000000000000001" }
        // 56,80 € × 25 = 1420 Kč, clo 3 € = 75 Kč (doručení 16. 10. je po 1. 7. 2026 a před 1. 11. 2026)
        assertEquals(Money(142_000, Currency.CZK), first.price.line(LineKind.ITEMS).amount)
        assertEquals(Money(7_500, Currency.CZK), first.price.line(LineKind.DUTY).amount)
        assertEquals(Money(149_500, Currency.CZK), first.price.total)
        assertEquals(LineStatus.UNKNOWN, first.price.line(LineKind.SHIPPING).status)
        assertEquals(Confidence.INCOMPLETE, first.price.confidence)
        assertEquals(LocalDate(2026, 10, 16), first.expectedDelivery)
        assertEquals("101+", first.soldBucket)
        assertEquals(Money(7_990, Currency.EUR), first.originalPrice)
        assertTrue(Assumption.SHIPPING_NOT_IN_SEARCH in first.assumptions)
        assertEquals(setOf("ae:9000000000000001", "ae:9000000000000002"), body.bestChoice.toSet())
        assertTrue(body.warnings.isEmpty())
    }

    @Test
    fun offerWithoutDeliveryEstimateUsesDefaultAndCrossesIntoHandlingFee() = testApplication {
        application { module(testServices(today = LocalDate(2026, 10, 20))) }

        val body = apiJson.decodeFromString(SearchResponse.serializer(), client.get("/v1/search?q=earbuds").bodyAsText())
        val second = body.offers.first { it.id == "ae:9000000000000002" }

        assertTrue(Assumption.ETA_DEFAULT in second.assumptions)
        assertEquals(LocalDate(2026, 11, 9), second.expectedDelivery)
        assertEquals(Reason.FEE_AMOUNT_NOT_SET, second.price.line(LineKind.HANDLING_FEE).reason)
    }

    @Test
    fun expensiveItemNeedsTariffDuty() = testApplication {
        application { module(testServices()) }

        val body = apiJson.decodeFromString(SearchResponse.serializer(), client.get("/v1/search?q=hodinky&market=PL").bodyAsText())
        val watch = body.offers.single()

        assertEquals(Currency.PLN, watch.price.total.currency)
        assertEquals(Reason.DUTY_TARIFF_RATE_REQUIRED, watch.price.line(LineKind.DUTY).reason)
    }

    @Test
    fun rejectsInvalidInput() = testApplication {
        application { module(testServices()) }

        assertEquals(HttpStatusCode.BadRequest, client.get("/v1/search?q=").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/v1/search?q=x&market=XX").status)
        assertEquals(HttpStatusCode.BadRequest, client.get("/v1/search?q=x&speed=2").status)
    }

    @Test
    fun healthAndRulesAreExposed() = testApplication {
        application { module(testServices()) }

        assertTrue(client.get("/health").bodyAsText().contains("aliexpress-fixture"))
        assertTrue(client.get("/v1/rules").bodyAsText().contains("eu-low-value-flat-duty"))
    }

    @Test
    fun missingRatesAreReportedNotGuessed() = testApplication {
        application { module(Services(AliExpressFixtureSource(fixtureJson()), StaticRatesProvider(null), today = { LocalDate(2026, 10, 4) })) }

        val body = apiJson.decodeFromString(SearchResponse.serializer(), client.get("/v1/search?q=power").bodyAsText())

        assertEquals(listOf("rates.missing"), body.warnings)
        assertNull(body.rates)
        assertEquals(Reason.RATES_MISSING, body.offers.single().price.line(LineKind.ITEMS).reason)
    }
}

class OfferMappingTest {
    @Test
    fun mapsAliExpressProductAndRecordsAssumptions() {
        val offer = AliExpressOfferMapper.map(
            AeProduct(productId = "7", title = "X", targetSalePrice = "10.50", targetSalePriceCurrency = "EUR", evaluateRate = "97.5%", latestVolume = "6", shipToDays = "7 days"),
        )!!

        assertEquals("ae:7", offer.id)
        assertEquals(Money(1_050, Currency.EUR), offer.unitPrice)
        assertEquals(0.975, offer.positiveRate)
        assertEquals(6, offer.soldCount)
        assertEquals(7, offer.transitDays)
        assertTrue(Assumption.ORIGIN_NON_EU in offer.assumptions)
    }

    @Test
    fun skipsProductsWithoutUsablePrice() {
        assertNull(AliExpressOfferMapper.map(AeProduct(productId = "1", targetSalePrice = "9.99", targetSalePriceCurrency = "XYZ")))
        assertNull(AliExpressOfferMapper.map(AeProduct(productId = null, targetSalePrice = "9.99", targetSalePriceCurrency = "EUR")))
    }
}

class RatesProviderTest {
    private val xml = "<Cube><Cube time='2026-10-02'><Cube currency='CZK' rate='25.0'/></Cube></Cube>"

    @Test
    fun cachesRatesAndKeepsLastGoodValueOnFailure() {
        var calls = 0
        var available = true
        var now = Instant.parse("2026-10-04T08:00:00Z")
        val provider = EcbHttpRatesProvider(
            fetcher = { calls++; if (available) xml else null },
            ttl = Duration.ofHours(6),
            now = { now },
        )

        assertEquals(25.0, provider.current()?.rate(Currency.CZK))
        provider.current()
        assertEquals(1, calls)

        now = now.plus(Duration.ofHours(7))
        available = false
        assertEquals(25.0, provider.current()?.rate(Currency.CZK))
        assertEquals(2, calls)
    }
}
