package app.ulovko.api.aliexpress

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class AeApiException(
    message: String,
    val code: String? = null,
    val requestId: String? = null,
) : RuntimeException(message)

/**
 * Produkt z affiliate API. Názvy polí podle typů referenčního SDK ae_sdk.
 * Číselná pole bereme jako text a převádíme sami: API je vrací někdy jako číslo, jindy jako řetězec.
 */
@Serializable
data class AeProduct(
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_title") val title: String? = null,
    @SerialName("product_detail_url") val detailUrl: String? = null,
    @SerialName("promotion_link") val promotionLink: String? = null,
    @SerialName("product_main_image_url") val mainImageUrl: String? = null,
    @SerialName("product_small_image_urls") val smallImageUrls: JsonElement? = null,
    @SerialName("sale_price") val salePrice: String? = null,
    @SerialName("sale_price_currency") val salePriceCurrency: String? = null,
    @SerialName("original_price") val originalPrice: String? = null,
    @SerialName("original_price_currency") val originalPriceCurrency: String? = null,
    @SerialName("target_sale_price") val targetSalePrice: String? = null,
    @SerialName("target_sale_price_currency") val targetSalePriceCurrency: String? = null,
    @SerialName("target_original_price") val targetOriginalPrice: String? = null,
    @SerialName("target_original_price_currency") val targetOriginalPriceCurrency: String? = null,
    @SerialName("discount") val discount: String? = null,
    @SerialName("evaluate_rate") val evaluateRate: String? = null,
    @SerialName("lastest_volume") val latestVolume: String? = null,
    @SerialName("shop_id") val shopId: String? = null,
    @SerialName("shop_url") val shopUrl: String? = null,
    @SerialName("first_level_category_name") val firstLevelCategory: String? = null,
    @SerialName("second_level_category_name") val secondLevelCategory: String? = null,
    @SerialName("ship_to_days") val shipToDays: String? = null,
) {
    /** Obrázky chodí jako pole, nebo zabalené v objektu { "string": [...] }. */
    val imageUrls: List<String>
        get() = unwrapList(smallImageUrls, "string").mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

data class AeProductPage(
    val products: List<AeProduct>,
    val currentPage: Int?,
    val totalRecords: Int?,
)

object AeResponseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseProductQuery(body: String): AeProductPage =
        parseProductList(body, "aliexpress_affiliate_product_query_response")

    internal fun parseProductList(body: String, responseKey: String): AeProductPage {
        val root = json.parseToJsonElement(body).jsonObject
        root["error_response"]?.let { throw errorFrom(it) }
        val respResult = (root[responseKey] as? JsonObject)?.get("resp_result") as? JsonObject
            ?: throw AeApiException("Neočekávaná odpověď AliExpressu: chybí $responseKey.resp_result")
        val result = respResult["result"] as? JsonObject
            ?: return AeProductPage(emptyList(), null, 0)
        val products = unwrapList(result["products"], "product")
            .map { json.decodeFromJsonElement(AeProduct.serializer(), it) }
        return AeProductPage(
            products = products,
            currentPage = result["current_page_no"].intOrNull(),
            totalRecords = result["total_record_count"].intOrNull(),
        )
    }

    private fun errorFrom(element: JsonElement): AeApiException {
        val error = element as? JsonObject ?: return AeApiException("AliExpress vrátil chybu")
        val field = { key: String -> (error[key] as? JsonPrimitive)?.contentOrNull }
        val message = listOfNotNull(field("msg"), field("sub_msg")).joinToString(": ").ifBlank { "AliExpress vrátil chybu" }
        return AeApiException(message, code = field("code"), requestId = field("request_id"))
    }

    private fun JsonElement?.intOrNull(): Int? = (this as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
}

/** Seznamy chodí jako pole, jako objekt s klíčem (bez zjednodušení odpovědi), nebo jako jediný objekt. */
internal fun unwrapList(element: JsonElement?, key: String): List<JsonElement> = when (element) {
    is JsonArray -> element
    is JsonObject -> when (val inner = element[key]) {
        is JsonArray -> inner
        null -> emptyList()
        else -> listOf(inner)
    }
    else -> emptyList()
}
