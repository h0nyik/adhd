# Ulovko (pracovní název)

**Všechna tržiště v jedné kapse. Jedna cena se vším, až do schránky.**

Aplikace pro iOS, Android, macOS, Windows a rozšíření do Safari, Chrome, Edge,
Opery a Firefoxu. Napíšeš, co chceš, Ulovko věc rozebere, ukáže o ní všechno
podstatné a vedle toho nabídky z Allegra a AliExpressu (později Aukra) s celkovou
cenou včetně dopravy, DPH, cla a poplatků, s hodnocením prodejce a doručením na
konkrétní den.

> Stav: koncept a klikací prototyp. Produkční kód zatím ne.
> Tahle složka je nezávislá na zbytku repozitáře (`extension/`, `server/`
> patří jinému projektu).

## Dokumentace

| Soubor | O čem |
|---|---|
| [00-nazev.md](docs/00-nazev.md) | Proč ne „Alexpress“, kandidáti, doporučení Ulovko |
| [01-produkt.md](docs/01-produkt.md) | Vize, persony, pilíře, rozsah MVP, metriky |
| [02-data-a-indexace.md](docs/02-data-a-indexace.md) | Jaké jsou v roce 2026 možnosti získání dat, API tržišť, právo |
| [03-vyhledavani-a-karta.md](docs/03-vyhledavani-a-karta.md) | Našeptávač, čeština, karta věci, AI vrstva |
| [04-cena-a-nabidky.md](docs/04-cena-a-nabidky.md) | Cenový engine (DPH, clo 3 €, manipulační poplatek), řádek nabídky, řazení |
| [05-ux-ui.md](docs/05-ux-ui.md) | Principy z výzkumu, gesta, haptika, nativnost, integrace |
| [06-recenze-a-odmeny.md](docs/06-recenze-a-odmeny.md) | Recenze za 30 sekund, gamifikace, úlovky, Omnibus |
| [07-preklad.md](docs/07-preklad.md) | Přepis místo překladu, invarianty, česká typografie |
| [08-ucet-a-nakup.md](docs/08-ucet-a-nakup.md) | Účet, propojení s tržišti, nákup ve třech fázích |
| [09-architektura.md](docs/09-architektura.md) | Stack, backend, AI, výkon, struktura repozitáře |
| [10-roadmapa-a-rizika.md](docs/10-roadmapa-a-rizika.md) | Fáze, rizika, otevřené otázky |
| [zdroje.md](docs/zdroje.md) | Všechny zdroje |

## Prototyp

[`prototype/index.html`](prototype/index.html) otevři v prohlížeči (nejlépe na
telefonu). Ukazuje našeptávač, kartu věci, seřaditelný seznam nabídek s celkovou
cenou a rozpadem, swipe gesta s haptikou, recenzi za 30 sekund a úlovky.
Produkt, prodejci i ceny jsou **fiktivní ukázková data**.

## Klíčová rozhodnutí v kostce

1. **Data:** oficiální API (AliExpress Affiliate API; Allegro `offers/listing`
   jen pro ověřené aplikace, žádost podat hned), vlastní katalog věcí, žádný
   hromadný scraping.
2. **Cena:** jedno číslo „Celkem do schránky“ včetně cla 3 € za tarifní položku
   (od 1. 7. 2026) a manipulačního poplatku EU (od 1. 11. 2026, výše zatím
   nepotvrzena). Stav ověřeno / odhad a Cenová jistota.
3. **Karta věci** se tvoří jednou na věc (dávkově), ne na každý dotaz.
4. **Nativně:** SwiftUI (Apple), Jetpack Compose (Android), Kotlin
   Multiplatform pro sdílenou logiku, WinUI 3 (Windows), WXT (rozšíření).
5. **Haptika a gesta** jako jeden sémantický slovník pro všechny platformy.
6. **Recenze za 30 sekund**, odměna bez ohledu na hodnocení (Omnibus).
7. **Nákup:** v MVP chytré předání do aplikace tržiště; nákup přímo
   v aplikaci až přes partnerství nebo standardy jako Google UCP.
8. **Překlad:** přepis z extrahovaných faktů, kontrola čísel, česká typografie.
