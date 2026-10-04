package app.ulovko.core

import app.ulovko.core.fx.EcbRatesParser
import app.ulovko.core.fx.ExchangeRates
import app.ulovko.core.money.Currency
import app.ulovko.core.money.Money
import app.ulovko.core.ranking.BestChoice
import app.ulovko.core.ranking.RankCandidate
import app.ulovko.core.rules.RuleSet
import app.ulovko.core.trust.SoldBucket
import app.ulovko.core.trust.TrustScore
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun parsesDecimalStringsExactly() {
        assertEquals(1234, Money.parse("12.34", Currency.CZK).minor)
        assertEquals(1230, Money.parse("12,3", Currency.CZK).minor)
        assertEquals(1200, Money.parse("12", Currency.CZK).minor)
        assertEquals(1, Money.parse("0.005", Currency.EUR).minor)
        assertEquals(0, Money.parse("0.004", Currency.EUR).minor)
        assertEquals(-101, Money.parse("-1.005", Currency.EUR).minor)
    }

    @Test
    fun rejectsFormattedOrInvalidInput() {
        assertFailsWith<IllegalArgumentException> { Money.parse("1 849", Currency.CZK) }
        assertFailsWith<IllegalArgumentException> { Money.parse("abc", Currency.CZK) }
    }

    @Test
    fun neverMixesCurrencies() {
        assertFailsWith<IllegalArgumentException> { Money(100, Currency.CZK) + Money(100, Currency.EUR) }
    }

    @Test
    fun printsCanonicalForm() {
        assertEquals("1849.00 CZK", Money(184_900, Currency.CZK).toString())
        assertEquals("-0.05", Money(-5, Currency.CZK).toPlainString())
    }
}

class ExchangeRatesTest {
    private val rates = ExchangeRates(LocalDate(2026, 10, 2), "test", mapOf(Currency.CZK to 25.0, Currency.PLN to 4.0))

    @Test
    fun convertsFromAndAcrossEuro() {
        assertEquals(Money.parse("1420", Currency.CZK), rates.convert(Money.parse("56.80", Currency.EUR), Currency.CZK))
        assertEquals(Money.parse("16", Currency.PLN), rates.convert(Money.parse("100", Currency.CZK), Currency.PLN))
        assertNull(rates.convert(Money.parse("1", Currency.EUR), Currency.HUF))
    }

    @Test
    fun parsesEcbDailyReferenceRates() {
        // Struktura podle eurofxref-daily.xml, hodnoty jsou smyšlené.
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
              <gesmes:subject>Reference rates</gesmes:subject>
              <Cube>
                <Cube time='2026-10-02'>
                  <Cube currency='USD' rate='1.1000'/>
                  <Cube currency='JPY' rate='160.00'/>
                  <Cube currency='CZK' rate='25.000'/>
                  <Cube currency='HUF' rate='400.00'/>
                  <Cube currency='PLN' rate='4.0000'/>
                </Cube>
              </Cube>
            </gesmes:Envelope>
        """.trimIndent()
        val parsed = EcbRatesParser.parse(xml)

        assertEquals(LocalDate(2026, 10, 2), parsed.date)
        assertEquals(25.0, parsed.rate(Currency.CZK))
        assertEquals(400.0, parsed.rate(Currency.HUF))
        assertEquals(1.0, parsed.rate(Currency.EUR))
        assertEquals(setOf(Currency.USD, Currency.CZK, Currency.HUF, Currency.PLN), parsed.perEur.keys)
    }

    @Test
    fun rejectsRatesWithoutDate() {
        assertFailsWith<IllegalArgumentException> { EcbRatesParser.parse("<Cube currency='CZK' rate='25'/>") }
    }
}

class RuleSetTest {
    private val rules = RuleSet.DEFAULT

    @Test
    fun defaultRulesHaveVatForAllAllegroMarkets() {
        val day = LocalDate(2026, 10, 4)
        assertEquals(2100, rules.vatRateBps("CZ", day))
        assertEquals(2300, rules.vatRateBps("PL", day))
        assertEquals(2300, rules.vatRateBps("SK", day))
        assertEquals(2700, rules.vatRateBps("HU", day))
    }

    @Test
    fun flatDutyIsValidFromJuly2026UntilJuly2028() {
        assertNull(rules.dutyRule(LocalDate(2026, 6, 30)))
        assertTrue(rules.dutyRegimeNotStartedYet(LocalDate(2026, 6, 30)))
        assertNotNull(rules.dutyRule(LocalDate(2026, 7, 1)))
        assertNotNull(rules.dutyRule(LocalDate(2028, 6, 30)))
        assertNull(rules.dutyRule(LocalDate(2028, 7, 1)))
        assertTrue(!rules.dutyRegimeNotStartedYet(LocalDate(2028, 7, 1)))
    }

    @Test
    fun handlingFeeStartsInNovemberWithoutAKnownRate() {
        assertNull(rules.handlingFeeRule(LocalDate(2026, 10, 31)))
        val fee = assertNotNull(rules.handlingFeeRule(LocalDate(2026, 11, 1)))
        assertNull(fee.amountEurCents)
    }
}

class BestChoiceTest {
    private fun c(id: String, total: Int, days: Int, trust: Int, freeReturns: Boolean) =
        RankCandidate(id, Money(total * 100L, Currency.CZK), days, trust, freeReturns)

    // Stejné nabídky jako v klikacím prototypu.
    private val offers = listOf(
        c("al1", 1849, 2, 96, true),
        c("ae1", 1495, 17, 91, true),
        c("ae2", 1454, 29, 74, false),
        c("ae3", 1590, 9, 91, true),
        c("al2", 1858, 4, 93, true),
        c("al3", 1978, 3, 88, false),
    )

    @Test
    fun defaultPreferenceFavoursFastTrustedOffer() {
        assertEquals("al1", BestChoice.rank(offers).first().id)
    }

    @Test
    fun priceOnlyPreferenceFavoursCheapTrustedOffer() {
        assertEquals("ae1", BestChoice.rank(offers, speedPreference = 0.0).first().id)
    }

    @Test
    fun rejectsMixedCurrencies() {
        val mixed = offers + RankCandidate("x", Money(100, Currency.EUR), 5, null, null)
        assertFailsWith<IllegalArgumentException> { BestChoice.rank(mixed) }
    }
}

class TrustTest {
    @Test
    fun fewPerfectRatingsDoNotBeatManyGoodOnes() {
        val newcomer = TrustScore.score(positiveRatio = 1.0, ratingCount = 3)
        val veteran = TrustScore.score(positiveRatio = 0.98, ratingCount = 10_000)
        assertEquals(91, newcomer)
        assertEquals(98, veteran)
        assertTrue(newcomer < veteran)
        assertEquals(90, TrustScore.score(positiveRatio = 0.0, ratingCount = 0))
    }

    @Test
    fun soldCountsMapToAllegroBuckets() {
        assertEquals(SoldBucket.NONE, SoldBucket.of(0))
        assertEquals(SoldBucket.FROM_1, SoldBucket.of(5))
        assertEquals(SoldBucket.FROM_6, SoldBucket.of(6))
        assertEquals(SoldBucket.FROM_51, SoldBucket.of(100))
        assertEquals(SoldBucket.FROM_101, SoldBucket.of(101))
        assertEquals("101+", SoldBucket.of(5000).label)
        assertFailsWith<IllegalArgumentException> { SoldBucket.of(-1) }
    }
}
