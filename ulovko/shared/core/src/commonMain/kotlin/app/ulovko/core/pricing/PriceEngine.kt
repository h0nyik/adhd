package app.ulovko.core.pricing

import app.ulovko.core.fx.ExchangeRates
import app.ulovko.core.market.Market
import app.ulovko.core.money.Currency
import app.ulovko.core.money.Money
import app.ulovko.core.money.mulDivHalfUp
import app.ulovko.core.rules.RuleSet
import kotlinx.datetime.LocalDate

/**
 * Spočítá celkovou cenu „do schránky“ v měně trhu a rozpad na složky.
 *
 * Zásady:
 * - Neznámou složku nikdy nedoplňujeme odhadem. Označí se UNKNOWN a celková cena je INCOMPLETE.
 * - Převod měny je vždy ESTIMATED (tržiště účtuje svým kurzem).
 * - Pravidla se vybírají podle očekávaného data dovozu, ne podle dneška: zásilka objednaná
 *   v říjnu se může proclívat až v listopadu, kdy už platí manipulační poplatek.
 */
class PriceEngine(private val rules: RuleSet = RuleSet.DEFAULT) {

    fun calculate(offer: OfferPricing, market: Market, rates: ExchangeRates?, today: LocalDate): PriceBreakdown {
        val target = market.currency
        val importDate = offer.expectedImportDate ?: today
        val vatBps = rules.vatRateBps(market.countryCode, importDate)
        val lines = mutableListOf<PriceLine>()

        val itemsOriginal = offer.unitPrice * offer.quantity
        val items = convert(itemsOriginal, target, rates)
        lines += when {
            items == null -> PriceLine(LineKind.ITEMS, LineStatus.UNKNOWN, null, Reason.RATES_MISSING, convertedFrom = itemsOriginal)
            itemsOriginal.currency == target -> PriceLine(LineKind.ITEMS, LineStatus.EXACT, items, Reason.PRICE_MARKET_CURRENCY)
            else -> PriceLine(LineKind.ITEMS, LineStatus.ESTIMATED, items, Reason.PRICE_CONVERTED, convertedFrom = itemsOriginal)
        }

        val shipping: Money? = when (val s = offer.shipping) {
            is Shipping.Known -> {
                val cost = convert(s.cost, target, rates)
                lines += when {
                    cost == null -> PriceLine(LineKind.SHIPPING, LineStatus.UNKNOWN, null, Reason.RATES_MISSING, convertedFrom = s.cost)
                    s.cost.minor == 0L -> PriceLine(LineKind.SHIPPING, LineStatus.EXACT, cost, Reason.SHIPPING_FREE)
                    s.cost.currency == target -> PriceLine(LineKind.SHIPPING, LineStatus.EXACT, cost, Reason.SHIPPING_KNOWN)
                    else -> PriceLine(LineKind.SHIPPING, LineStatus.ESTIMATED, cost, Reason.PRICE_CONVERTED, convertedFrom = s.cost)
                }
                cost
            }
            Shipping.Unknown -> {
                lines += PriceLine(LineKind.SHIPPING, LineStatus.UNKNOWN, null, Reason.SHIPPING_UNKNOWN)
                null
            }
        }

        if (offer.origin == Origin.EU) {
            lines += PriceLine(LineKind.VAT, LineStatus.INCLUDED, includedVat(items, vatBps), Reason.VAT_INCLUDED_EU)
            lines += PriceLine(LineKind.DUTY, LineStatus.NOT_APPLICABLE, null, Reason.DUTY_NOT_APPLICABLE_EU)
            lines += PriceLine(LineKind.HANDLING_FEE, LineStatus.NOT_APPLICABLE, null, Reason.FEE_NOT_APPLICABLE_EU)
        } else {
            val goodsEur = convert(itemsOriginal, Currency.EUR, rates)
            val lowValue = goodsEur?.let { it.minor <= rules.lowValueThresholdEurCents }
            val duty = dutyLine(offer, goodsEur, lowValue, importDate, target, rates)
            lines += vatLine(offer, lowValue, items, shipping, duty, vatBps, target)
            lines += duty
            lines += handlingFeeLine(offer, importDate, target, rates)
        }

        val total = lines
            .filter { it.status == LineStatus.EXACT || it.status == LineStatus.ESTIMATED }
            .mapNotNull { it.amount }
            .fold(Money.zero(target), Money::plus)

        val confidence = when {
            lines.any { it.status == LineStatus.UNKNOWN } -> Confidence.INCOMPLETE
            lines.any { it.status == LineStatus.ESTIMATED } -> Confidence.ESTIMATE
            else -> Confidence.VERIFIED
        }
        return PriceBreakdown(market, total, confidence, lines, rates?.date)
    }

    private fun dutyLine(
        offer: OfferPricing,
        goodsEur: Money?,
        lowValue: Boolean?,
        importDate: LocalDate,
        target: Currency,
        rates: ExchangeRates?,
    ): PriceLine {
        if (goodsEur == null || lowValue == null) {
            return PriceLine(LineKind.DUTY, LineStatus.UNKNOWN, null, Reason.DUTY_VALUE_UNKNOWN)
        }
        if (!lowValue) return PriceLine(LineKind.DUTY, LineStatus.UNKNOWN, null, Reason.DUTY_TARIFF_RATE_REQUIRED)
        val rule = rules.dutyRule(importDate)
        return when {
            rule != null -> {
                val dutyEur = Money(rule.perTariffLineEurCents * offer.tariffLines, Currency.EUR)
                eurAmountLine(LineKind.DUTY, dutyEur, target, rates, Reason.DUTY_FLAT_PER_TARIFF_LINE, rule.id)
            }
            rules.dutyRegimeNotStartedYet(importDate) ->
                PriceLine(LineKind.DUTY, LineStatus.NOT_APPLICABLE, null, Reason.DUTY_DE_MINIMIS)
            else -> PriceLine(LineKind.DUTY, LineStatus.UNKNOWN, null, Reason.DUTY_REGIME_UNKNOWN)
        }
    }

    private fun vatLine(
        offer: OfferPricing,
        lowValue: Boolean?,
        items: Money?,
        shipping: Money?,
        duty: PriceLine,
        vatBps: Int?,
        target: Currency,
    ): PriceLine {
        // Zásilky do 150 € od tržiště registrovaného v IOSS: DPH je zaplacená v ceně při objednávce.
        if (lowValue == true && offer.vatCollectedAtCheckout) {
            return PriceLine(LineKind.VAT, LineStatus.INCLUDED, includedVat(items, vatBps), Reason.VAT_COLLECTED_AT_CHECKOUT)
        }
        if (vatBps == null) return PriceLine(LineKind.VAT, LineStatus.UNKNOWN, null, Reason.VAT_RATE_MISSING)
        if (items == null) return PriceLine(LineKind.VAT, LineStatus.UNKNOWN, null, Reason.RATES_MISSING)
        // DPH při dovozu se počítá ze zboží, dopravy a cla.
        val dutyAmount = duty.amount.takeIf { duty.status == LineStatus.EXACT || duty.status == LineStatus.ESTIMATED }
        val dutyKnown = duty.status != LineStatus.UNKNOWN
        val base = listOfNotNull(items, shipping, dutyAmount).fold(Money.zero(target), Money::plus)
        val vat = Money(mulDivHalfUp(base.minor, vatBps.toLong(), 10_000), target)
        val reason = if (shipping != null && dutyKnown) Reason.VAT_IMPORT else Reason.VAT_IMPORT_PARTIAL_BASE
        return PriceLine(LineKind.VAT, LineStatus.ESTIMATED, vat, reason)
    }

    private fun handlingFeeLine(offer: OfferPricing, importDate: LocalDate, target: Currency, rates: ExchangeRates?): PriceLine {
        val rule = rules.handlingFeeRule(importDate)
            ?: return PriceLine(LineKind.HANDLING_FEE, LineStatus.NOT_APPLICABLE, null, Reason.FEE_NOT_YET_IN_FORCE)
        val cents = rule.amountEurCents
            ?: return PriceLine(LineKind.HANDLING_FEE, LineStatus.UNKNOWN, null, Reason.FEE_AMOUNT_NOT_SET, rule.id)
        val feeEur = Money(cents * (if (rule.perItem) offer.tariffLines else 1), Currency.EUR)
        return eurAmountLine(LineKind.HANDLING_FEE, feeEur, target, rates, Reason.FEE_APPLIED, rule.id)
    }

    private fun eurAmountLine(
        kind: LineKind,
        amountEur: Money,
        target: Currency,
        rates: ExchangeRates?,
        reason: String,
        ruleId: String,
    ): PriceLine {
        val converted = convert(amountEur, target, rates)
            ?: return PriceLine(kind, LineStatus.UNKNOWN, null, Reason.RATES_MISSING, ruleId, amountEur)
        return if (target == Currency.EUR) {
            PriceLine(kind, LineStatus.EXACT, converted, reason, ruleId)
        } else {
            PriceLine(kind, LineStatus.ESTIMATED, converted, reason, ruleId, amountEur)
        }
    }

    private fun convert(amount: Money, to: Currency, rates: ExchangeRates?): Money? =
        if (amount.currency == to) amount else rates?.convert(amount, to)

    /** DPH obsažená v ceně s daní: cena × sazba ÷ (1 + sazba). Jen informativně. */
    private fun includedVat(gross: Money?, vatBps: Int?): Money? {
        if (gross == null || vatBps == null || gross.minor < 0) return null
        return Money(mulDivHalfUp(gross.minor, vatBps.toLong(), 10_000L + vatBps), gross.currency)
    }
}
