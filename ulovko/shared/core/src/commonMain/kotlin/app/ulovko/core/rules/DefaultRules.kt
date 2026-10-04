package app.ulovko.core.rules

const val DEFAULT_RULES_VERIFIED_ON = "2026-10-04"

/**
 * Výchozí pravidla. Zdroje a poznámky jsou součástí dat, aby bylo u každého čísla
 * dohledatelné, odkud je. Před každou změnou ověřit v primárním zdroji.
 */
internal val DEFAULT_RULES_JSON = """
{
  "version": "2026-10-04",
  "verifiedOn": "$DEFAULT_RULES_VERIFIED_ON",
  "vat": [
    { "country": "CZ", "standardRateBps": 2100, "source": "https://www.vatupdate.com/vat-rates-eu/" },
    { "country": "PL", "standardRateBps": 2300, "source": "https://www.vatupdate.com/vat-rates-eu/" },
    { "country": "SK", "standardRateBps": 2300, "source": "https://www.vatupdate.com/vat-rates-eu/" },
    { "country": "HU", "standardRateBps": 2700, "source": "https://www.vatupdate.com/vat-rates-eu/" }
  ],
  "lowValueDuty": [
    {
      "id": "eu-low-value-flat-duty",
      "perTariffLineEurCents": 300,
      "consignmentThresholdEurCents": 15000,
      "validFrom": "2026-07-01",
      "validTo": "2028-07-01",
      "source": "https://www.consilium.europa.eu/en/press/press-releases/2025/12/12/customs-council-agrees-to-levy-customs-duty-on-small-parcels-as-of-1-july-2026/",
      "note": "Přechodný paušál 3 € za odlišnou tarifní položku v zásilce do 150 €. Přesné datum konce ověřit v nařízení."
    }
  ],
  "handlingFee": [
    {
      "id": "eu-handling-fee",
      "validFrom": "2026-11-01",
      "amountEurCents": null,
      "perItem": true,
      "source": "https://taxation-customs.ec.europa.eu/customs/eu-customs-reform_en",
      "note": "Členské státy ho mají vybírat nejpozději od 1. 11. 2026, výši stanoví Komise. Jednotku (položka zásilky) upřesnit."
    }
  ]
}
"""
