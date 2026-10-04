package app.ulovko.core.fx

import app.ulovko.core.money.Currency
import app.ulovko.core.money.Money
import app.ulovko.core.money.roundHalfUp
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Kurzy vztažené k euru (kolik jednotek měny za 1 EUR), jak je publikuje ECB.
 *
 * Jedna báze pro všechny trhy (CZK, PLN, HUF, EUR) zjednodušuje křížové převody.
 * Převod je vždy odhad (tržiště účtuje vlastním kurzem), proto se tu na rozdíl
 * od sčítání cen smí použít Double: výsledek se zaokrouhlí na haléře a označí jako odhad.
 */
@Serializable
data class ExchangeRates(
    val date: LocalDate,
    val source: String,
    val perEur: Map<Currency, Double>,
) {
    fun rate(currency: Currency): Double? = if (currency == Currency.EUR) 1.0 else perEur[currency]

    /** Vrátí null, když kurz některé z měn chybí. */
    fun convert(amount: Money, to: Currency): Money? {
        if (amount.currency == to) return amount
        val from = rate(amount.currency) ?: return null
        val target = rate(to) ?: return null
        val major = amount.minor.toDouble() / amount.currency.factor
        return Money(roundHalfUp(major / from * target * to.factor), to)
    }
}

/**
 * Čte denní referenční kurzy ECB (eurofxref-daily.xml): prvek
 * `<Cube time='YYYY-MM-DD'>` a v něm `<Cube currency='CZK' rate='…'/>`.
 */
object EcbRatesParser {
    const val SOURCE = "ECB euro foreign exchange reference rates"
    private val cubeElement = Regex("""<Cube\s+([^>]*?)/?>""")
    private fun attribute(name: String) = Regex("""\b$name\s*=\s*['"]([^'"]+)['"]""")
    private val timeAttr = attribute("time")
    private val currencyAttr = attribute("currency")
    private val rateAttr = attribute("rate")

    fun parse(xml: String, source: String = SOURCE): ExchangeRates {
        var date: LocalDate? = null
        val rates = mutableMapOf<Currency, Double>()
        for (match in cubeElement.findAll(xml)) {
            val attrs = match.groupValues[1]
            timeAttr.find(attrs)?.let { date = LocalDate.parse(it.groupValues[1]) }
            val code = currencyAttr.find(attrs)?.groupValues?.get(1) ?: continue
            val rate = rateAttr.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
            Currency.fromCode(code)?.let { rates[it] = rate }
        }
        val day = requireNotNull(date) { "Kurzovní lístek ECB neobsahuje datum (atribut time)" }
        require(rates.isNotEmpty()) { "Kurzovní lístek ECB neobsahuje žádnou podporovanou měnu" }
        return ExchangeRates(day, source, rates)
    }
}
