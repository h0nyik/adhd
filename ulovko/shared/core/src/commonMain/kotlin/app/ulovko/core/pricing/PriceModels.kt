package app.ulovko.core.pricing

import app.ulovko.core.market.Market
import app.ulovko.core.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Odkud zboží do země doručení jde. Rozhoduje o clu, DPH při dovozu a poplatcích. */
@Serializable
enum class Origin { EU, NON_EU }

@Serializable
sealed interface Shipping {
    @Serializable
    @SerialName("known")
    data class Known(val cost: Money) : Shipping

    /** Tržiště cenu dopravy nevrátilo (např. AliExpress Affiliate API ji v hledání nemá). */
    @Serializable
    @SerialName("unknown")
    data object Unknown : Shipping
}

/** Vstup cenového enginu: jedna nabídka tak, jak ji vrátilo tržiště. */
@Serializable
data class OfferPricing(
    val unitPrice: Money,
    val quantity: Int = 1,
    val shipping: Shipping,
    val origin: Origin,
    /** U zboží mimo EU: vybírá tržiště DPH už při platbě (IOSS)? U nabídek z EU se ignoruje. */
    val vatCollectedAtCheckout: Boolean,
    /** Počet odlišných tarifních položek v zásilce; paušální clo se platí za každou. */
    val tariffLines: Int = 1,
    /** Očekávané datum dovozu (nejzazší doručení). Podle něj se vybírají platná pravidla. */
    val expectedImportDate: LocalDate? = null,
) {
    init {
        require(quantity >= 1) { "Množství musí být aspoň 1" }
        require(tariffLines >= 1) { "Zásilka má aspoň jednu tarifní položku" }
    }
}

@Serializable
enum class LineKind { ITEMS, SHIPPING, VAT, DUTY, HANDLING_FEE }

@Serializable
enum class LineStatus {
    /** Částka přesně podle tržiště, v měně trhu. */
    EXACT,
    /** Částka spočítaná, ale s odhadem (převod kurzem, sazba dle pravidel). */
    ESTIMATED,
    /** Už obsaženo v ceně zboží; částka je jen informativní a nepřičítá se. */
    INCLUDED,
    NOT_APPLICABLE,
    /** Složku neznáme; celková cena je proto neúplná. */
    UNKNOWN,
}

/** Jak moc celkové ceně věříme. Promítá se do UI jako „✓ ověřeno“, „≈ odhad“ a „+ ?“. */
@Serializable
enum class Confidence { VERIFIED, ESTIMATE, INCOMPLETE }

@Serializable
data class PriceLine(
    val kind: LineKind,
    val status: LineStatus,
    val amount: Money? = null,
    /** Stabilní kód důvodu ([Reason]); klient ho přeloží do jazyka trhu. */
    val reason: String,
    val ruleId: String? = null,
    /** Původní částka před převodem do měny trhu. */
    val convertedFrom: Money? = null,
)

@Serializable
data class PriceBreakdown(
    val market: Market,
    /** Součet známých složek (EXACT a ESTIMATED). Při INCOMPLETE se v UI zobrazí „+ ?“. */
    val total: Money,
    val confidence: Confidence,
    val lines: List<PriceLine>,
    val ratesDate: LocalDate? = null,
) {
    fun line(kind: LineKind): PriceLine = lines.first { it.kind == kind }
}

/** Kódy důvodů. Jsou součástí API, proto se nemění, jen přibývají. */
object Reason {
    const val PRICE_MARKET_CURRENCY = "price.market_currency"
    const val PRICE_CONVERTED = "price.converted"
    const val RATES_MISSING = "price.rates_missing"

    const val SHIPPING_FREE = "shipping.free"
    const val SHIPPING_KNOWN = "shipping.known"
    const val SHIPPING_UNKNOWN = "shipping.unknown"

    const val VAT_INCLUDED_EU = "vat.included_eu"
    const val VAT_COLLECTED_AT_CHECKOUT = "vat.collected_at_checkout"
    const val VAT_IMPORT = "vat.import"
    const val VAT_IMPORT_PARTIAL_BASE = "vat.import_partial_base"
    const val VAT_RATE_MISSING = "vat.rate_missing"

    const val DUTY_NOT_APPLICABLE_EU = "duty.not_applicable_eu"
    const val DUTY_FLAT_PER_TARIFF_LINE = "duty.flat_per_tariff_line"
    const val DUTY_DE_MINIMIS = "duty.de_minimis_before_reform"
    const val DUTY_TARIFF_RATE_REQUIRED = "duty.tariff_rate_required"
    const val DUTY_REGIME_UNKNOWN = "duty.regime_unknown"
    const val DUTY_VALUE_UNKNOWN = "duty.value_unknown"

    const val FEE_NOT_APPLICABLE_EU = "fee.not_applicable_eu"
    const val FEE_NOT_YET_IN_FORCE = "fee.not_yet_in_force"
    const val FEE_AMOUNT_NOT_SET = "fee.amount_not_set"
    const val FEE_APPLIED = "fee.applied"
}
