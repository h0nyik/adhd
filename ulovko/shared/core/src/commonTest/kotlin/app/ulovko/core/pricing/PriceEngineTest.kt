package app.ulovko.core.pricing

import app.ulovko.core.fx.ExchangeRates
import app.ulovko.core.market.Market
import app.ulovko.core.money.Currency
import app.ulovko.core.money.Money
import app.ulovko.core.rules.HandlingFeeRule
import app.ulovko.core.rules.RuleSet
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Kurzy jsou smyšlené kulaté hodnoty, aby se výsledky daly přepočítat z hlavy. */
private val TEST_RATES = ExchangeRates(
    date = LocalDate(2026, 10, 2),
    source = "test (smyšlené kurzy)",
    perEur = mapOf(Currency.CZK to 25.0, Currency.PLN to 4.0, Currency.HUF to 400.0),
)
private val TODAY = LocalDate(2026, 10, 4)
private val BEFORE_FEE = LocalDate(2026, 10, 21)
private val AFTER_FEE = LocalDate(2026, 11, 5)

private fun eur(amount: String) = Money.parse(amount, Currency.EUR)
private fun czk(amount: String) = Money.parse(amount, Currency.CZK)

private fun nonEu(
    price: String,
    importDate: LocalDate = BEFORE_FEE,
    shipping: Shipping = Shipping.Known(eur("0")),
    ioss: Boolean = true,
    tariffLines: Int = 1,
) = OfferPricing(
    unitPrice = eur(price),
    shipping = shipping,
    origin = Origin.NON_EU,
    vatCollectedAtCheckout = ioss,
    tariffLines = tariffLines,
    expectedImportDate = importDate,
)

class PriceEngineTest {
    private val engine = PriceEngine()

    @Test
    fun euOfferInMarketCurrencyIsVerified() {
        val offer = OfferPricing(czk("1849"), shipping = Shipping.Known(czk("0")), origin = Origin.EU, vatCollectedAtCheckout = true)
        val result = engine.calculate(offer, Market.CZ, TEST_RATES, TODAY)

        assertEquals(czk("1849"), result.total)
        assertEquals(Confidence.VERIFIED, result.confidence)
        assertEquals(LineStatus.EXACT, result.line(LineKind.ITEMS).status)
        assertEquals(Reason.SHIPPING_FREE, result.line(LineKind.SHIPPING).reason)
        // 1849 × 21/121 = 320,9008… → 320,90 (jen informativně, nepřičítá se)
        assertEquals(LineStatus.INCLUDED, result.line(LineKind.VAT).status)
        assertEquals(czk("320.90"), result.line(LineKind.VAT).amount)
        assertEquals(LineStatus.NOT_APPLICABLE, result.line(LineKind.DUTY).status)
        assertEquals(LineStatus.NOT_APPLICABLE, result.line(LineKind.HANDLING_FEE).status)
    }

    @Test
    fun lowValueParcelFromChinaPaysFlatDutyAfterJuly2026() {
        val result = engine.calculate(nonEu("56.80"), Market.CZ, TEST_RATES, TODAY)

        assertEquals(czk("1420"), result.line(LineKind.ITEMS).amount)
        assertEquals(eur("56.80"), result.line(LineKind.ITEMS).convertedFrom)
        assertEquals(czk("75"), result.line(LineKind.DUTY).amount)
        assertEquals(LineStatus.ESTIMATED, result.line(LineKind.DUTY).status)
        assertEquals(Reason.VAT_COLLECTED_AT_CHECKOUT, result.line(LineKind.VAT).reason)
        assertEquals(Reason.FEE_NOT_YET_IN_FORCE, result.line(LineKind.HANDLING_FEE).reason)
        assertEquals(czk("1495"), result.total)
        assertEquals(Confidence.ESTIMATE, result.confidence)
        assertEquals(TEST_RATES.date, result.ratesDate)
    }

    @Test
    fun handlingFeeWithoutRateMakesPriceIncomplete() {
        val result = engine.calculate(nonEu("56.80", importDate = AFTER_FEE), Market.CZ, TEST_RATES, TODAY)

        val fee = result.line(LineKind.HANDLING_FEE)
        assertEquals(LineStatus.UNKNOWN, fee.status)
        assertEquals(Reason.FEE_AMOUNT_NOT_SET, fee.reason)
        assertEquals("eu-handling-fee", fee.ruleId)
        assertEquals(Confidence.INCOMPLETE, result.confidence)
        assertEquals(czk("1495"), result.total)
    }

    @Test
    fun beforeTheReformLowValueParcelsHaveNoDuty() {
        val result = engine.calculate(nonEu("56.80", importDate = LocalDate(2026, 6, 15)), Market.CZ, TEST_RATES, TODAY)

        assertEquals(Reason.DUTY_DE_MINIMIS, result.line(LineKind.DUTY).reason)
        assertEquals(czk("1420"), result.total)
    }

    @Test
    fun afterTheInterimPeriodTheDutyRegimeIsUnknown() {
        val result = engine.calculate(nonEu("56.80", importDate = LocalDate(2028, 7, 1)), Market.CZ, TEST_RATES, TODAY)

        assertEquals(Reason.DUTY_REGIME_UNKNOWN, result.line(LineKind.DUTY).reason)
        assertEquals(Confidence.INCOMPLETE, result.confidence)
    }

    @Test
    fun parcelsAbove150EurNeedTariffDutyAndImportVat() {
        val offer = nonEu("200", shipping = Shipping.Known(eur("10")))
        val result = engine.calculate(offer, Market.CZ, TEST_RATES, TODAY)

        assertEquals(Reason.DUTY_TARIFF_RATE_REQUIRED, result.line(LineKind.DUTY).reason)
        val vat = result.line(LineKind.VAT)
        // (5000 + 250) × 21 % = 1102,50; clo neznáme, proto je základ neúplný
        assertEquals(czk("1102.50"), vat.amount)
        assertEquals(Reason.VAT_IMPORT_PARTIAL_BASE, vat.reason)
        assertEquals(czk("6352.50"), result.total)
        assertEquals(Confidence.INCOMPLETE, result.confidence)
    }

    @Test
    fun dutyIsChargedPerTariffLineAndIsExactInEuroMarkets() {
        val result = engine.calculate(nonEu("20", tariffLines = 3), Market.SK, TEST_RATES, TODAY)

        assertEquals(eur("9"), result.line(LineKind.DUTY).amount)
        assertEquals(LineStatus.EXACT, result.line(LineKind.DUTY).status)
        assertEquals(eur("3.74"), result.line(LineKind.VAT).amount)
        assertEquals(eur("29"), result.total)
        assertEquals(Confidence.VERIFIED, result.confidence)
    }

    @Test
    fun unknownShippingIsNeverGuessed() {
        val result = engine.calculate(nonEu("56.80", shipping = Shipping.Unknown), Market.CZ, TEST_RATES, TODAY)

        val shipping = result.line(LineKind.SHIPPING)
        assertEquals(LineStatus.UNKNOWN, shipping.status)
        assertNull(shipping.amount)
        assertEquals(Confidence.INCOMPLETE, result.confidence)
        assertEquals(czk("1495"), result.total)
    }

    @Test
    fun missingRatesMakeConvertedPartsUnknown() {
        val result = engine.calculate(nonEu("56.80"), Market.CZ, rates = null, today = TODAY)

        assertEquals(Reason.RATES_MISSING, result.line(LineKind.ITEMS).reason)
        assertEquals(Reason.RATES_MISSING, result.line(LineKind.DUTY).reason)
        assertEquals(Confidence.INCOMPLETE, result.confidence)
    }

    @Test
    fun importVatWithoutIossUsesHungarianRateOnGoodsShippingAndDuty() {
        val offer = nonEu("10", shipping = Shipping.Known(eur("2")), ioss = false)
        val result = engine.calculate(offer, Market.HU, TEST_RATES, TODAY)
        val huf = { amount: String -> Money.parse(amount, Currency.HUF) }

        assertEquals(huf("4000"), result.line(LineKind.ITEMS).amount)
        assertEquals(huf("800"), result.line(LineKind.SHIPPING).amount)
        assertEquals(huf("1200"), result.line(LineKind.DUTY).amount)
        // (4000 + 800 + 1200) × 27 % = 1620
        assertEquals(huf("1620"), result.line(LineKind.VAT).amount)
        assertEquals(Reason.VAT_IMPORT, result.line(LineKind.VAT).reason)
        assertEquals(huf("7620"), result.total)
    }

    @Test
    fun polishMarketConvertsToZloty() {
        val result = engine.calculate(nonEu("25"), Market.PL, TEST_RATES, TODAY)

        assertEquals(Money.parse("100", Currency.PLN), result.line(LineKind.ITEMS).amount)
        assertEquals(Money.parse("12", Currency.PLN), result.line(LineKind.DUTY).amount)
        assertEquals(Money.parse("112", Currency.PLN), result.total)
    }

    @Test
    fun handlingFeeIsAppliedPerItemOnceTheRateIsKnown() {
        val withFee = RuleSet.DEFAULT.copy(
            handlingFee = listOf(
                HandlingFeeRule(id = "test-fee", validFrom = LocalDate(2026, 11, 1), amountEurCents = 200, source = "test"),
            ),
        )
        val result = PriceEngine(withFee).calculate(
            nonEu("20", importDate = LocalDate(2026, 11, 10), tariffLines = 2), Market.SK, TEST_RATES, TODAY,
        )

        assertEquals(eur("4"), result.line(LineKind.HANDLING_FEE).amount)
        assertEquals(eur("30"), result.total)
        assertEquals(Confidence.VERIFIED, result.confidence)
    }
}
