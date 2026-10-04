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
AliExpress a Allegro (pokud verifikace projde). Vyhledávání, karta věci,
nabídky, cenový engine, hlídání ceny, chytré předání, recenze v1, úlovky v1.

## Fáze 2

macOS, Safari rozšíření, Windows (WinUI 3), Aukro, košík napříč tržišti,
vizuální hledání, ověřené nákupy přes e-mail, výběr odměn, obohacení od
uživatelů přes rozšíření.

## Fáze 3

Slovensko, Polsko, Maďarsko (trhy Allegra), další tržiště, nákup přímo
v aplikaci, pokud to tržiště umožní.

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

## Otevřené otázky pro tebe

1. **Název:** Ulovko, Kupko, nebo jiný? (viz [00-nazev.md](00-nazev.md))
2. **Tón:** tykání (doporučuji, sedí k „ulov to“), nebo vykání?
3. **Trh ve fázi 1:** jen Česko, nebo hned i Slovensko?
4. **Tým a stack:** budete mít nativní vývojáře (Swift, Kotlin), nebo spíš
   JavaScript (pak React Native / Expo)?
5. **Rozpočet a termín** prvního spuštění?
6. **Monetizace:** jen affiliate, nebo i předplatné „Plus“?
7. **Aukro:** jednat hned, aby bylo ve fázi 1?
8. **Další krok:** mám založit kostru monorepa, design tokeny a první
   proof-of-concept s AliExpress Affiliate API a cenovým enginem?
