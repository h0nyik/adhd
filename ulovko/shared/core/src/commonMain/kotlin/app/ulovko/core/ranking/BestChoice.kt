package app.ulovko.core.ranking

import app.ulovko.core.money.Money
import kotlin.math.sqrt

data class RankCandidate(
    val id: String,
    val total: Money,
    /** Za kolik dní nejpozději bude zboží doma. */
    val deliveryLatestDays: Int,
    /** Skóre důvěry 0–100, null = neznámé. */
    val trust: Int?,
    /** Vrácení zdarma, null = neznámé. */
    val freeReturns: Boolean?,
)

data class RankedOffer(val id: String, val score: Double)

/**
 * „Nejlepší volba“: vážené skóre z celkové ceny, rychlosti, důvěry a podmínek vrácení.
 * Váhy jsou veřejné (UI „Jak řadíme?“, docs/04-cena-a-nabidky.md).
 */
object BestChoice {
    const val PRICE_AND_SPEED_WEIGHT = 0.7
    const val TRUST_WEIGHT = 0.2
    const val RETURNS_WEIGHT = 0.1
    /** Neznámá důvěra nebo podmínky vrácení se počítají jako průměr, ne jako nula ani maximum. */
    private const val UNKNOWN_SCORE = 0.5

    /** [speedPreference] 0.0 = rozhoduje jen cena, 1.0 = jen rychlost doručení. */
    fun rank(candidates: List<RankCandidate>, speedPreference: Double = 0.4): List<RankedOffer> {
        if (candidates.isEmpty()) return emptyList()
        require(speedPreference in 0.0..1.0) { "speedPreference musí být mezi 0 a 1" }
        val currency = candidates.first().total.currency
        require(candidates.all { it.total.currency == currency }) { "Všechny nabídky musí být ve stejné měně" }

        val minTotal = candidates.minOf { it.total.minor }.coerceAtLeast(1)
        val minDays = candidates.minOf { it.deliveryLatestDays }.coerceAtLeast(1)
        val priceWeight = PRICE_AND_SPEED_WEIGHT * (1 - speedPreference)
        val speedWeight = PRICE_AND_SPEED_WEIGHT * speedPreference

        return candidates.map { c ->
            val price = minTotal.toDouble() / c.total.minor.coerceAtLeast(1)
            val speed = sqrt(minDays.toDouble() / c.deliveryLatestDays.coerceAtLeast(1))
            val trust = c.trust?.let { it / 100.0 } ?: UNKNOWN_SCORE
            val returns = when (c.freeReturns) { true -> 1.0; false -> 0.5; null -> UNKNOWN_SCORE }
            RankedOffer(c.id, priceWeight * price + speedWeight * speed + TRUST_WEIGHT * trust + RETURNS_WEIGHT * returns)
        }.sortedByDescending { it.score }
    }
}
