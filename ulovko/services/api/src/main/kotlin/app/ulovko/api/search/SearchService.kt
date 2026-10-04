package app.ulovko.api.search

import app.ulovko.api.offers.Assumption
import app.ulovko.api.offers.Marketplace
import app.ulovko.api.offers.OfferSource
import app.ulovko.api.rates.RatesProvider
import app.ulovko.core.market.Market
import app.ulovko.core.money.Money
import app.ulovko.core.pricing.OfferPricing
import app.ulovko.core.pricing.PriceBreakdown
import app.ulovko.core.pricing.PriceEngine
import app.ulovko.core.ranking.BestChoice
import app.ulovko.core.ranking.RankCandidate
import app.ulovko.core.trust.SoldBucket
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

@Serializable
data class RatesInfo(val date: LocalDate, val source: String)

@Serializable
data class OfferDto(
    val id: String,
    val marketplace: Marketplace,
    val title: String,
    val imageUrl: String? = null,
    val url: String? = null,
    val soldBucket: String? = null,
    val positiveRate: Double? = null,
    val expectedDelivery: LocalDate,
    val originalPrice: Money? = null,
    val assumptions: List<String>,
    val price: PriceBreakdown,
)

@Serializable
data class SearchResponse(
    val query: String,
    val market: Market,
    val source: String,
    val rates: RatesInfo? = null,
    val offers: List<OfferDto>,
    /** Pořadí nabídek podle „Nejlepší volby“ (id). */
    val bestChoice: List<String>,
    val warnings: List<String>,
)

class SearchService(
    private val source: OfferSource,
    private val rates: RatesProvider,
    private val engine: PriceEngine = PriceEngine(),
    private val today: () -> LocalDate,
) {
    companion object {
        /** Když tržiště dobu doručení neuvede, počítáme s touto (odhad pro zásilky mimo EU). */
        const val DEFAULT_TRANSIT_DAYS = 20
        const val MAX_QUERY_LENGTH = 200
    }

    suspend fun search(query: String, market: Market, page: Int = 1, speedPreference: Double = 0.4): SearchResponse {
        val q = query.trim()
        require(q.isNotEmpty()) { "Zadej, co hledáš (parametr q)." }
        require(q.length <= MAX_QUERY_LENGTH) { "Dotaz je delší než $MAX_QUERY_LENGTH znaků." }
        require(page >= 1) { "Stránka začíná od 1." }
        require(speedPreference in 0.0..1.0) { "Parametr speed musí být mezi 0 a 1." }

        val day = today()
        val currentRates = rates.current()
        val warnings = mutableListOf<String>()
        if (currentRates == null) warnings += "rates.missing"
        else if (rates.isSample) warnings += "rates.sample_not_real"

        val offers = source.search(q, market, page).map { offer ->
            val transit = offer.transitDays ?: DEFAULT_TRANSIT_DAYS
            val delivery = day.plus(DatePeriod(days = transit))
            val pricing = OfferPricing(
                unitPrice = offer.unitPrice,
                shipping = offer.shipping,
                origin = offer.origin,
                vatCollectedAtCheckout = offer.vatCollectedAtCheckout,
                expectedImportDate = delivery,
            )
            OfferDto(
                id = offer.id,
                marketplace = offer.marketplace,
                title = offer.title,
                imageUrl = offer.imageUrl,
                url = offer.url,
                soldBucket = offer.soldCount?.let { SoldBucket.of(it).label },
                positiveRate = offer.positiveRate,
                expectedDelivery = delivery,
                originalPrice = offer.originalPrice,
                assumptions = offer.assumptions + listOfNotNull(Assumption.ETA_DEFAULT.takeIf { offer.transitDays == null }),
                price = engine.calculate(pricing, market, currentRates, day),
            ) to transit
        }

        val ranking = BestChoice.rank(
            offers.map { (dto, transit) -> RankCandidate(dto.id, dto.price.total, transit, trust = null, freeReturns = null) },
            speedPreference,
        )
        return SearchResponse(
            query = q,
            market = market,
            source = source.name,
            rates = currentRates?.let { RatesInfo(it.date, it.source) },
            offers = offers.map { it.first },
            bestChoice = ranking.map { it.id },
            warnings = warnings,
        )
    }
}
