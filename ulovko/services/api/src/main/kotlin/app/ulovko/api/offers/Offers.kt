package app.ulovko.api.offers

import app.ulovko.api.aliexpress.AeClient
import app.ulovko.api.aliexpress.AeProduct
import app.ulovko.api.aliexpress.AeProductQuery
import app.ulovko.api.aliexpress.AeResponseParser
import app.ulovko.core.market.Market
import app.ulovko.core.money.Currency
import app.ulovko.core.money.Money
import app.ulovko.core.pricing.Origin
import app.ulovko.core.pricing.Shipping
import kotlinx.serialization.Serializable
import java.text.Normalizer

@Serializable
enum class Marketplace { ALIEXPRESS, ALLEGRO }

/** Nabídka převedená z formátu tržiště do společného tvaru. */
data class SourceOffer(
    val id: String,
    val marketplace: Marketplace,
    val title: String,
    val imageUrl: String?,
    val url: String?,
    val unitPrice: Money,
    val originalPrice: Money?,
    val shipping: Shipping,
    val origin: Origin,
    val vatCollectedAtCheckout: Boolean,
    /** Dny doručení podle tržiště, null = neznámé. */
    val transitDays: Int?,
    val soldCount: Int?,
    /** Podíl kladných hodnocení produktu 0–1. */
    val positiveRate: Double?,
    /** Kódy předpokladů, které jsme museli udělat (zobrazí se v detailu a v API). */
    val assumptions: List<String>,
)

interface OfferSource {
    /** Identifikace zdroje v odpovědi API, např. "aliexpress-live". */
    val name: String
    suspend fun search(query: String, market: Market, page: Int): List<SourceOffer>
}

object Assumption {
    const val ORIGIN_NON_EU = "origin.assumed_non_eu"
    const val VAT_AT_CHECKOUT = "vat.assumed_collected_at_checkout"
    const val SHIPPING_NOT_IN_SEARCH = "shipping.not_in_affiliate_search"
    const val ETA_DEFAULT = "eta.assumed_default"
}

/**
 * Převod produktu z AliExpress Affiliate API.
 *
 * Co hledání v affiliate API neřekne, a proto to označujeme jako předpoklad:
 * odkud zboží jde (sklad v Číně, nebo v EU), zda je DPH vybraná při platbě (IOSS)
 * a kolik stojí doprava. Doprava se doplní jiným API (výpočet dopravy pro kupujícího).
 */
object AliExpressOfferMapper {
    fun map(product: AeProduct): SourceOffer? {
        val id = product.productId?.takeIf { it.isNotBlank() } ?: return null
        val price = money(product.targetSalePrice, product.targetSalePriceCurrency)
            ?: money(product.salePrice, product.salePriceCurrency)
            ?: return null
        val original = money(product.targetOriginalPrice, product.targetOriginalPriceCurrency)
            ?: money(product.originalPrice, product.originalPriceCurrency)
        return SourceOffer(
            id = "ae:$id",
            marketplace = Marketplace.ALIEXPRESS,
            title = product.title.orEmpty(),
            imageUrl = product.mainImageUrl ?: product.imageUrls.firstOrNull(),
            url = product.promotionLink ?: product.detailUrl,
            unitPrice = price,
            originalPrice = original?.takeIf { it.currency == price.currency && it > price },
            shipping = Shipping.Unknown,
            origin = Origin.NON_EU,
            vatCollectedAtCheckout = true,
            transitDays = product.shipToDays?.filter(Char::isDigit)?.toIntOrNull(),
            soldCount = product.latestVolume?.trim()?.toIntOrNull(),
            positiveRate = product.evaluateRate?.removeSuffix("%")?.trim()?.toDoubleOrNull()?.let { it / 100.0 },
            assumptions = listOf(Assumption.ORIGIN_NON_EU, Assumption.VAT_AT_CHECKOUT, Assumption.SHIPPING_NOT_IN_SEARCH),
        )
    }

    private fun money(amount: String?, currency: String?): Money? {
        val c = currency?.let(Currency::fromCode) ?: return null
        return amount?.let { runCatching { Money.parse(it, c) }.getOrNull() }
    }
}

/** Živé hledání přes AliExpress Affiliate API. */
class AliExpressLiveSource(private val client: AeClient) : OfferSource {
    override val name = "aliexpress-live"

    override suspend fun search(query: String, market: Market, page: Int): List<SourceOffer> {
        val request = AeProductQuery(
            keywords = query,
            shipToCountry = market.countryCode,
            targetLanguage = if (market == Market.PL) "PL" else "EN",
            pageNo = page,
        )
        return client.queryProducts(request).products.mapNotNull(AliExpressOfferMapper::map)
    }
}

/**
 * Vývojový zdroj bez přístupu k API: smyšlené produkty ve skutečném tvaru odpovědi.
 * Hledá podle slov v názvu bez ohledu na diakritiku a s několika ukázkovými překlady
 * dotazu (skutečné porozumění dotazu viz docs/03-vyhledavani-a-karta.md).
 */
class AliExpressFixtureSource(fixtureJson: String) : OfferSource {
    override val name = "aliexpress-fixture"
    private val products = AeResponseParser.parseProductQuery(fixtureJson).products

    private val synonyms = mapOf(
        "sluchatka" to "earbuds", "sluchawki" to "earbuds", "fulhallgato" to "earbuds",
        "powerbanka" to "power", "powerbank" to "power",
        "hodinky" to "smartwatch", "zegarek" to "smartwatch",
    )

    override suspend fun search(query: String, market: Market, page: Int): List<SourceOffer> {
        if (page > 1) return emptyList()
        val words = normalize(query).split(' ').filter { it.length >= 2 }.map { synonyms[it] ?: it }
        return products
            .filter { p -> val title = normalize(p.title.orEmpty()); words.any { title.contains(it) } }
            .mapNotNull(AliExpressOfferMapper::map)
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ").trim()
}
