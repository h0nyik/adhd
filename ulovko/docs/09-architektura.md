# Architektura a technologie

## Principy

1. **Chytrý server, tenký nativní klient.** Vyhledávání, katalog, cenový
   engine, karty a překlady běží na serveru. Klient vykresluje a ukládá do
   mezipaměti.
2. **Jedna pravda o ceně.** Cenový engine je jeden a dává stejný výsledek
   v aplikaci, na webu i v rozšíření.
3. **Offline jako výchozí stav** pro hlídané ceny, historii a rozepsané recenze.
4. **Soukromí od návrhu:** hosting v EU, minimum osobních dat, historie
   hledání primárně v zařízení.

## Přehled

```mermaid
flowchart TB
  subgraph Klienti
    IOS["iOS · iPadOS · macOS<br/>SwiftUI + Safari rozšíření"]
    AND["Android<br/>Kotlin + Jetpack Compose"]
    WIN["Windows<br/>WinUI 3 (fáze 2)"]
    WEB["Web + PWA<br/>veřejné karty věcí"]
    EXT["Rozšíření prohlížečů<br/>WXT: Chrome, Edge, Opera, Firefox, Safari"]
  end
  CORE["Sdílené jádro (Kotlin Multiplatform)<br/>modely, API klient, mezipaměť, offline sync"]
  IOS --- CORE
  AND --- CORE
  GW["API brána<br/>REST/GraphQL + SSE stream pro postupné načítání karty"]
  Klienti --> GW
  subgraph Služby
    S["Vyhledávání<br/>hybridní index"]
    K["Katalog věcí<br/>PostgreSQL + pgvector"]
    O["Nabídky a ceny<br/>stahovače podle tržišť, Redis, časové řady"]
    P["Cenový engine<br/>DPH, clo, poplatky, kurzy (pravidla jako data)"]
    C["Tvorba karet a překladů<br/>Claude API, Batches"]
    R["Recenze a úlovky"]
    A["Účty, notifikace (APNs, FCM), affiliate odkazy"]
  end
  GW --> S & K & O & P & C & R & A
  subgraph Zdroje
    AE["AliExpress Affiliate API"]
    AL["Allegro REST API"]
    IC["Open Icecat, GS1"]
    CNB["Kurzy ČNB"]
  end
  O --> AE & AL
  K --> IC
  P --> CNB
```

## Klienti: doporučení a proč

| Platforma | Doporučení | Proč |
|---|---|---|
| iOS, iPadOS, macOS | **SwiftUI** | Liquid Glass zdarma, App Intents, widgety, Live Activities, hostitel Safari rozšíření, jeden kód pro tři platformy |
| Android | **Kotlin + Jetpack Compose** | Material 3 Expressive, nejlepší haptika a gesta |
| Sdílená logika | **Kotlin Multiplatform** | Modely, API klient, mezipaměť a offline režim jednou; UI zůstává nativní |
| Windows | **WinUI 3** (fáze 2), do té doby instalovatelná PWA | Fluent 2, Mica, nativní chování |
| Web | Framework se server-side renderingem (např. Next.js) | Každá karta věci má veřejnou URL: sdílení a návštěvy z Googlu |
| Rozšíření | **WXT** | Jeden kód pro Chrome, Edge, Opera, Firefox i Safari |

**Zvážené alternativy:**
- *React Native / Expo:* jeden tým a rychlejší start, nativní komponenty,
  haptika a gesta přes knihovny. Dobrá volba, pokud bude tým javascriptový.
- *Compose Multiplatform pro iOS* je stabilní od verze 1.8.0 (květen 2025),
  ale na iOS nekreslí systémové komponenty, takže Liquid Glass nedostaneme
  automaticky.
- *Flutter:* vlastní vykreslování, stejný problém s nativním vzhledem.
  Kvůli požadavku „působit všude nativně“ ho nedoporučuji.

## Backend

- **Jazyk:** Kotlin (Ktor), aby modely a cenový engine sdílely kód s KMP
  jádrem v aplikacích. Alternativa TypeScript, pokud tým preferuje.
- **Data:** PostgreSQL (+ pgvector), vyhledávací engine (Typesense nebo
  Meilisearch, později OpenSearch nebo Vespa), Redis (horké ceny, limity),
  objektové úložiště (obrázky, překlady), fronta a workflow pro stahování
  (např. Temporal), analytická databáze (např. ClickHouse).

## AI vrstva

| Úloha | Model a režim |
|---|---|
| Syntéza karty věci, souhrny recenzí | Claude Opus 5.5 (`claude-opus-5-5`), strukturované výstupy, dávkově přes Message Batches API |
| Překlad a přepis nabídek | Claude Opus 5.5 přes Batches, prompt caching pro systémový prompt a slovník kategorie |
| Extrakce parametrů a párování variant | Claude Opus 5.5 se strukturovaným výstupem (JSON schéma) |
| Porozumění dotazu po odeslání | Claude Opus 5.5 s nízkým nastavením effort; levnější model jen po evaluaci (rozhodnutí na tobě) |
| Okamžité a soukromé úlohy v zařízení | Apple Foundation Models, Chrome Translator API, ML Kit |

**Evaluace:** zlatá sada (např. 500 nabídek na kategorii) pro kvalitu překladu
a kontrolu číselných invariantů. Každá změna promptu nebo modelu se změří.

## Výkonnostní rozpočty

| Operace | Cíl (p95) |
|---|---|
| Našeptávač (server) | ≤ 100 ms |
| První výsledky | ≤ 800 ms |
| Karta z mezipaměti, první vykreslení | ≤ 1 s |
| Ověření ceny při otevření karty | ≤ 2 s |

## Bezpečnost

- Žádné přihlašovací údaje k tržištím.
- Rozšíření: jen volitelná oprávnění pro podporovaná tržiště a `activeTab`.
- Analytika šetrná k soukromí, šifrování, pravidelné mazání.

## Návrh struktury repozitáře

```
ulovko/
  apps/
    apple/        SwiftUI: iOS, iPadOS, macOS, hostitel Safari rozšíření
    android/      Jetpack Compose
    windows/      WinUI 3 (fáze 2)
    web/          SSR web + PWA
    extension/    WXT
  shared/core/    Kotlin Multiplatform: modely, API klient, cenový engine
  services/       api, fetchers, cards, search
  design/tokens/  barvy, typografie, pohyb a haptika jako jeden zdroj pravdy
                  (generuje Swift, Kotlin, C# a CSS)
  docs/
  prototype/
```

Haptika jako design token (viz [05-ux-ui.md](05-ux-ui.md)) znamená, že
`ulovek` je definovaný jednou a každá platforma ho přeloží po svém.
