package app.ulovko.core.market

import app.ulovko.core.money.Currency
import kotlinx.serialization.Serializable

/**
 * Trh = země doručení. Všechny čtyři jsou trhy Allegra (allegro.pl, .cz, .sk, .hu).
 * Sazby DPH tu záměrně nejsou: patří do pravidel (RuleSet), aby šly měnit bez vydání aplikace.
 */
@Serializable
enum class Market(val countryCode: String, val currency: Currency, val language: String) {
    CZ("CZ", Currency.CZK, "cs"),
    PL("PL", Currency.PLN, "pl"),
    SK("SK", Currency.EUR, "sk"),
    HU("HU", Currency.HUF, "hu");

    companion object {
        fun fromCode(code: String): Market? = entries.firstOrNull { it.countryCode == code.trim().uppercase() }
    }
}
