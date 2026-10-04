package app.ulovko.api.aliexpress

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Testovací vektory byly spočítané referenčním SDK ae_sdk 0.6.0 (AEBaseClient.sign/assemble)
 * se smyšleným klíčem "test-secret-not-real".
 */
class AeSignerTest {
    private val secret = "test-secret-not-real"

    private val productQueryParams = mapOf(
        "method" to "aliexpress.affiliate.product.query",
        "app_key" to "123456",
        "sign_method" to "sha256",
        "timestamp" to "1791100000000",
        "simplify" to "true",
        "keywords" to "wireless earbuds",
        "target_currency" to "EUR",
        "target_language" to "EN",
        "ship_to_country" to "CZ",
        "page_no" to "1",
        "page_size" to "20",
        "tracking_id" to "ulovko_test",
    )

    @Test
    fun signsTopApiMethodsLikeReferenceSdk() {
        assertEquals(
            "89CA3A619E7A1A1BE19DE077E776565B602D3411A18CDBA55BEDCD7DF3AB4514",
            AeSigner.sign(productQueryParams, secret),
        )
    }

    @Test
    fun prefixesPathForMethodsWithSlash() {
        val params = mapOf(
            "method" to "/auth/token/create",
            "app_key" to "123456",
            "sign_method" to "sha256",
            "timestamp" to "1791100000000",
            "code" to "abc",
        )
        assertEquals("07939A51BD7AC09CDA8F1AC4F86F12175B03CE48829008DBFB9442BE479D6D39", AeSigner.sign(params, secret))
    }

    @Test
    fun ignoresExistingSignParameter() {
        assertEquals(
            AeSigner.sign(productQueryParams, secret),
            AeSigner.sign(productQueryParams + ("sign" to "ANYTHING"), secret),
        )
    }
}

class AeClientTest {
    private val credentials = AeCredentials(appKey = "123456", appSecret = "test-secret-not-real", trackingId = "ulovko_test")
    private val query = AeProductQuery(keywords = "wireless earbuds", shipToCountry = "CZ")

    private fun fixture() = requireNotNull(javaClass.getResource("/fixtures/aliexpress-product-query.json")).readText()

    @Test
    fun buildsTheSameUrlAsReferenceSdk() {
        val client = AeClient(credentials, transport = { error("bez sítě") }, clock = { 1_791_100_000_000 })
        assertEquals(
            "https://api-sg.aliexpress.com/sync?app_key=123456&keywords=wireless%20earbuds" +
                "&method=aliexpress.affiliate.product.query&page_no=1&page_size=20&ship_to_country=CZ" +
                "&sign=89CA3A619E7A1A1BE19DE077E776565B602D3411A18CDBA55BEDCD7DF3AB4514" +
                "&sign_method=sha256&simplify=true&target_currency=EUR&target_language=EN" +
                "&timestamp=1791100000000&tracking_id=ulovko_test",
            client.buildUrl(AeClient.PRODUCT_QUERY, query.toParams(credentials.trackingId)),
        )
    }

    @Test
    fun parsesProductsFromWrappedResponse() = runBlocking {
        var requested = ""
        val client = AeClient(credentials, transport = { url -> requested = url; AeHttpResponse(200, fixture()) })

        val page = client.queryProducts(query)

        assertTrue(requested.startsWith(AeClient.TOP_URL))
        assertEquals(4, page.products.size)
        assertEquals(4, page.totalRecords)
        val first = page.products.first()
        assertEquals("9000000000000001", first.productId)
        assertEquals("56.80", first.targetSalePrice)
        assertEquals("1840", first.latestVolume)
        assertEquals(2, first.imageUrls.size)
        assertEquals("9000000000000002", page.products[1].productId)
    }

    @Test
    fun parsesSimplifiedResponseWithPlainArray() {
        val body = """
            {"aliexpress_affiliate_product_query_response":{"resp_result":{"resp_code":200,"result":{
              "current_page_no":1,"total_record_count":1,
              "products":[{"product_id":"42","target_sale_price":"9.99","target_sale_price_currency":"EUR",
                           "product_small_image_urls":["https://example.com/a.jpg"]}]}}}}
        """.trimIndent()
        val page = AeResponseParser.parseProductQuery(body)

        assertEquals(1, page.products.size)
        assertEquals(listOf("https://example.com/a.jpg"), page.products.single().imageUrls)
    }

    @Test
    fun returnsEmptyPageWhenResultIsMissing() {
        val body = """{"aliexpress_affiliate_product_query_response":{"resp_result":{"resp_code":405,"resp_msg":"no result"}}}"""
        assertEquals(0, AeResponseParser.parseProductQuery(body).products.size)
    }

    @Test
    fun turnsErrorResponseIntoException() = runBlocking {
        val body = """{"error_response":{"code":"IncompleteSignature","msg":"signature mismatch","request_id":"req-1"}}"""
        val client = AeClient(credentials, transport = { AeHttpResponse(200, body) })

        val error = assertFailsWith<AeApiException> { client.queryProducts(query) }
        assertEquals("IncompleteSignature", error.code)
        assertEquals("req-1", error.requestId)
        assertEquals("signature mismatch", error.message)
    }

    @Test
    fun failsOnHttpError() {
        val client = AeClient(credentials, transport = { AeHttpResponse(503, "") })
        assertFailsWith<AeApiException> { runBlocking { client.queryProducts(query) } }
    }

    @Test
    fun validatesQuery() {
        assertFailsWith<IllegalArgumentException> { AeProductQuery(keywords = " ", shipToCountry = "CZ") }
        assertFailsWith<IllegalArgumentException> { AeProductQuery(keywords = "x", shipToCountry = "CZ", pageSize = 51) }
    }
}
