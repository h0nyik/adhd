# Roadmapa, rizika a otevřené otázky

Časy jsou orientační pro malý tým (2 mobilní vývojáři, 1–2 backend, 1 designér).

## Fáze 0: ověření (2–4 týdny)

- Konzultace s advokátem (podmínky API a affiliate, cashback, DSA, GDPR).
- **Žádost o verifikaci aplikace u Allegra** a přístup k AliExpress Affiliate
  API. Obojí může trvat, proto hned.
- Test klikacího prototypu s 5–8 lidmi z cílovky.
- Název, domény, rešerše ochranných známek.
- Technický průzkum: limity API, kvalita párování na vzorku 1 000 nabídek.

## Fáze 1: MVP (orientačně 3–4 měsíce)

iOS, Android, rozšíření (Chrome, Edge, Opera, Firefox), web s kartami.
Trhy Česko a Polsko, Slovensko a Maďarsko hned po nich. AliExpress a Allegro
(pokud verifikace projde). Vyhledávání, karta věci,
nabídky, cenový engine, hlídání ceny, chytré předání, recenze v1, úlovky v1.

## Fáze 2

macOS, Safari rozšíření, Windows (WinUI 3), Aukro, košík napříč tržišti,
vizuální hledání, ověřené nákupy přes e-mail, výběr odměn, obohacení od
uživatelů přes rozšíření.

## Fáze 3

Další tržiště a země mimo region Allegra, nákup přímo v aplikaci, pokud to tržiště
umožní.

## Rizika

| Riziko | Dopad | Zmírnění |
|---|---|---|
| Allegro verifikaci nepovolí | Vysoký | Nabídnout partnerství (přivádíme kupující), začít AliExpressem, affiliate feedy, později obohacení od uživatelů |
| Affiliate podmínky zakážou cashback | Vysoký | Úlovky jen za recenze a komunitu, předplatné „Plus“ |
| Nepřesná cena | Vysoký (důvěra) | Ověření před předáním, stav ceny, Cenová jistota, monitoring odchylek |
| Právní spor kvůli datům | Vysoký | Žádný hromadný scraping, jen oficiální zdroje |
| Chyby v překladu | Střední | Přepis místo překladu, invarianty, „Původní text“ |
| Zamítnutí v obchodech s aplikacemi | Střední | Transparentní affiliate, žádná podobnost s cizí značkou |
| Náklady na jazykové modely | Střední | Dávkování, prompt caching, karta jednou na věc, ne na dotaz |
| Konkurence (srovnávače, Google) | Střední | Rychlost, UX, celková cena s clem, komunita |
| Změny cel a poplatků | Střední | Pravidla jako data s platností, aktualizace bez vydání aplikace |

## Rozhodnuto (4. 10. 2026)

| Otázka | Rozhodnutí |
|---|---|
| Název | Zatím otevřený. „Ulovko“ je jen pracovní název složky a balíčků. |
| Tón | Rozhodne výzkum s uživateli (A/B test tykání a vykání). |
| Trhy | Česko a Polsko od začátku, pak Slovensko a Maďarsko (trhy Allegra). |
| Tým | Nativní: SwiftUI (Apple), Kotlin + Compose (Android). |
| Další krok | Kostra monorepa, design tokeny, cenový engine, napojení na AliExpress API. **Hotovo**, viz [09-architektura.md](09-architektura.md). |

## Otevřené otázky

1. **Rozpočet a termín** prvního spuštění?
2. **Monetizace:** jen affiliate, nebo i předplatné „Plus“?
3. **Aukro:** jednat hned, aby bylo ve fázi 1?
4. **Přístupy:** registrace na AliExpress Open Platform (app key, secret, tracking ID)
   a žádost o verifikaci aplikace u Allegra. Obojí musí podat firma, která aplikaci
   provozuje.

## Další technické kroky

1. Ověřit odpovědi AliExpress API skutečným voláním (měny, `ship_to_days`, tvar odpovědi).
2. Doprava z API pro výpočet dopravy kupujícího, aby cena neměla „+ ?“.
3. Napojení na Allegro `GET /offers/listing` po verifikaci.
4. Párování nabídek na věc (GTIN/EAN, model, embeddingy) a katalog věcí.
5. Kostra iOS a Android aplikace nad sdíleným jádrem a tokeny.
