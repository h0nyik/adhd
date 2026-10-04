package app.ulovko.core.money

import kotlinx.serialization.Serializable
import kotlin.math.floor

/** Měny trhů, na kterých chceme působit, a měny, ve kterých vracejí ceny tržiště. */
@Serializable
enum class Currency(val minorDigits: Int) {
    CZK(2), PLN(2), EUR(2), HUF(2), USD(2);

    /** Kolik nejmenších jednotek má jedna celá jednotka (pro dvě desetinná místa 100). */
    val factor: Long get() = pow10(minorDigits)

    companion object {
        fun fromCode(code: String): Currency? = entries.firstOrNull { it.name == code.trim().uppercase() }
    }
}

/**
 * Částka v nejmenších jednotkách měny (haléře, grosze, centy, fillér).
 *
 * Ceny se nikdy nedrží v Double: sčítání plovoucích čísel by u „100% přesné“
 * celkové ceny dřív nebo později ukázalo o haléř jinou částku.
 */
@Serializable
data class Money(val minor: Long, val currency: Currency) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minor + other.minor, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(minor - other.minor, currency)
    }

    operator fun times(factor: Int): Money = Money(minor * factor, currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minor.compareTo(other.minor)
    }

    /** Kanonický zápis bez lokalizace ("1849.00"). Pro UI formátuje klient podle jazyka trhu. */
    fun toPlainString(): String {
        val sign = if (minor < 0) "-" else ""
        val abs = if (minor < 0) -minor else minor
        if (currency.minorDigits == 0) return sign + abs
        val whole = abs / currency.factor
        val fraction = (abs % currency.factor).toString().padStart(currency.minorDigits, '0')
        return "$sign$whole.$fraction"
    }

    override fun toString(): String = "${toPlainString()} ${currency.name}"

    private fun requireSameCurrency(other: Money) =
        require(currency == other.currency) { "Nelze kombinovat $currency a ${other.currency}" }

    companion object {
        private val DECIMAL = Regex("""^-?\d+([.,]\d+)?$""")

        fun zero(currency: Currency) = Money(0, currency)

        /**
         * Převede desetinný řetězec ("12.34", "12,3", "12") přesně na nejmenší jednotky.
         * Víc desetinných míst, než měna má, se zaokrouhlí matematicky (half-up).
         */
        fun parse(amount: String, currency: Currency): Money {
            val text = amount.trim()
            require(DECIMAL.matches(text)) { "Neplatná částka: '$amount'" }
            val negative = text.startsWith('-')
            val unsigned = text.removePrefix("-").replace(',', '.')
            val whole = unsigned.substringBefore('.')
            val fraction = unsigned.substringAfter('.', "")
            val digits = currency.minorDigits
            var minor = whole.toLong() * currency.factor
            if (digits > 0) minor += fraction.take(digits).padEnd(digits, '0').toLong()
            if (fraction.length > digits && fraction[digits] >= '5') minor += 1
            return Money(if (negative) -minor else minor, currency)
        }
    }
}

internal fun pow10(n: Int): Long {
    var result = 1L
    repeat(n) { result *= 10 }
    return result
}

/** Zaokrouhlení half-up (od nuly), shodné na všech platformách. */
internal fun roundHalfUp(value: Double): Long =
    if (value >= 0) floor(value + 0.5).toLong() else -floor(-value + 0.5).toLong()

/** a × b ÷ c zaokrouhleno half-up, pro nezáporná čísla, bez Double. */
internal fun mulDivHalfUp(a: Long, b: Long, c: Long): Long {
    require(a >= 0 && b >= 0 && c > 0) { "mulDivHalfUp očekává nezáporné vstupy" }
    return (a * b * 2 + c) / (c * 2)
}
