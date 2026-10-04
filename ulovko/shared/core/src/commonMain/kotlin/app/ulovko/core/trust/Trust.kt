package app.ulovko.core.trust

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Skóre důvěry prodejce 0–100 jako bayesovský průměr podílu kladných hodnocení.
 * Prodejce se 3 hodnoceními na 100 % tak nepředběhne prodejce s 10 000 hodnoceními na 98 %:
 * dokud hodnocení nepřibývají, táhne se skóre k priorové hodnotě.
 */
object TrustScore {
    const val DEFAULT_PRIOR = 0.90
    const val DEFAULT_PRIOR_WEIGHT = 50

    fun score(
        positiveRatio: Double,
        ratingCount: Int,
        prior: Double = DEFAULT_PRIOR,
        priorWeight: Int = DEFAULT_PRIOR_WEIGHT,
    ): Int {
        require(positiveRatio in 0.0..1.0) { "positiveRatio musí být 0–1" }
        require(ratingCount >= 0 && priorWeight > 0)
        val smoothed = (prior * priorWeight + positiveRatio * ratingCount) / (priorWeight + ratingCount)
        return (smoothed * 100).roundToInt()
    }
}

/**
 * Počet prodaných kusů ve sjednocených pásmech. Allegro API vrací popularitu jen v těchto
 * rozmezích, proto je použijeme pro všechna tržiště, aby čísla byla srovnatelná.
 */
@Serializable
enum class SoldBucket(val label: String, val min: Int, val max: Int?) {
    NONE("0", 0, 0),
    FROM_1("1–5", 1, 5),
    FROM_6("6–10", 6, 10),
    FROM_11("11–20", 11, 20),
    FROM_21("21–50", 21, 50),
    FROM_51("51–100", 51, 100),
    FROM_101("101+", 101, null);

    companion object {
        fun of(count: Int): SoldBucket {
            require(count >= 0) { "Počet prodaných nemůže být záporný" }
            return entries.first { count >= it.min && (it.max == null || count <= it.max) }
        }
    }
}
