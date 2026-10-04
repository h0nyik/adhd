package app.ulovko.core.rules

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Daňová a celní pravidla jako data. Každé pravidlo má platnost (od včetně, do vyjma)
 * a zdroj, takže změnu sazby nebo nový poplatek nasadíme bez vydání aplikace.
 */
@Serializable
data class RuleSet(
    val version: String,
    val verifiedOn: LocalDate,
    val vat: List<VatRule>,
    val lowValueDuty: List<DutyRule>,
    val handlingFee: List<HandlingFeeRule>,
) {
    fun vatRateBps(countryCode: String, on: LocalDate): Int? =
        vat.firstOrNull { it.country == countryCode && isActive(it.validFrom, it.validTo, on) }?.standardRateBps

    fun dutyRule(on: LocalDate): DutyRule? = lowValueDuty.firstOrNull { isActive(it.validFrom, it.validTo, on) }

    /** Platí k danému dni ještě starý režim (osvobození do 150 €), protože paušál teprve začne? */
    fun dutyRegimeNotStartedYet(on: LocalDate): Boolean =
        dutyRule(on) == null && lowValueDuty.isNotEmpty() && lowValueDuty.all { on < it.validFrom }

    /** Hranice „zásilky nízké hodnoty“ (hodnota zboží bez dopravy), v eurocentech. */
    val lowValueThresholdEurCents: Long
        get() = lowValueDuty.firstOrNull()?.consignmentThresholdEurCents ?: 15_000

    fun handlingFeeRule(on: LocalDate): HandlingFeeRule? =
        handlingFee.firstOrNull { isActive(it.validFrom, it.validTo, on) }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(text: String): RuleSet = json.decodeFromString(serializer(), text)

        /** Pravidla ověřená k [DEFAULT_RULES_VERIFIED_ON]; server je může přepsat souborem. */
        val DEFAULT: RuleSet by lazy { fromJson(DEFAULT_RULES_JSON) }
    }
}

@Serializable
data class VatRule(
    val country: String,
    /** Základní sazba v bazických bodech: 2100 = 21 %. */
    val standardRateBps: Int,
    val validFrom: LocalDate? = null,
    val validTo: LocalDate? = null,
    val source: String,
)

@Serializable
data class DutyRule(
    val id: String,
    /** Paušální clo za každou odlišnou tarifní položku v zásilce, v eurocentech. */
    val perTariffLineEurCents: Long,
    /** Do této hodnoty zboží (bez dopravy) jde o zásilku nízké hodnoty, v eurocentech. */
    val consignmentThresholdEurCents: Long,
    val validFrom: LocalDate,
    val validTo: LocalDate? = null,
    val source: String,
    val note: String? = null,
)

@Serializable
data class HandlingFeeRule(
    val id: String,
    val validFrom: LocalDate,
    val validTo: LocalDate? = null,
    /** null = sazba zatím není stanovená; cena se pak označí jako neúplná. */
    val amountEurCents: Long? = null,
    /** Účtuje se za položku zásilky (předpoklad: za tarifní položku), jinak za zásilku. */
    val perItem: Boolean = true,
    val source: String,
    val note: String? = null,
)

private fun isActive(from: LocalDate?, to: LocalDate?, on: LocalDate): Boolean =
    (from == null || on >= from) && (to == null || on < to)
