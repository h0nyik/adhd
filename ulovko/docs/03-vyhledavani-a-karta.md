# Vyhledávání a karta věci

## Rychlost je funkce

- Nielsenovy limity odezvy: do 0,1 s působí reakce okamžitě, do 1 s zůstává
  myšlenka nepřerušená, nad 10 s člověk ztrácí pozornost.
- Studie Deloitte pro Google (*Milliseconds Make Millions*): zrychlení
  mobilního webu o 0,1 s zvýšilo konverzi v retailu o 8,4 %.

**Rozpočty:** našeptávač ≤ 100 ms (p95 na serveru), první výsledky ≤ 800 ms,
karta z mezipaměti ≤ 1 s, AI části karty se dosypávají streamem.

## Našeptávač

Ukazuje max. 8 položek ve skupinách. Každá skupina má jiný tvar, aby šlo
skenovat okem:

| Typ | Příklad pro „sluchat“ | Tvar |
|---|---|---|
| Doplnění slova | sluchátka **bezdrátová** | text, doplněná část tučně |
| Oprava překlepu | „sluchtka“ → sluchátka | „Mysleli jste…“ jen při nízké jistotě, jinak rovnou |
| Kategorie | Sluchátka do uší · Elektronika | ikona kategorie |
| Konkrétní věc | Aurion Buds 3 Pro · od 1 454 Kč | miniatura + cena „od“ (celková) |
| Atribut jako čip | s ANC · do 1 500 Kč · doma do pátku | čipy, klepnutím se přidají do dotazu |
| Záměr | „dárek pro tátu do 500 Kč“ | otevře průvodce výběrem |
| Historie a hlídané | Powerbanka 20 000 mAh | hodiny / zvonek, swipe smaže |

Ovládání: šipky a Enter na počítači, Tab doplní slovo, swipe doleva smaže
položku historie. Haptika jen při výběru, ne při psaní.

### Čeština a překlad dotazu

- Bez ohledu na diakritiku: „sluchatka“ = „sluchátka“.
- Tvarosloví: „sluchátek“, „sluchátkům“ → lemma „sluchátko“ (český
  morfologický analyzátor, např. MorphoDiTa z ÚFAL MFF UK).
- Synonyma a hovorové tvary: mobil = telefon = smartphone, „bezdrátovky“.
- Jednotky a kódy: „2tb“ = „2 TB“ = „2000 GB“, `WH1000XM5` = `WH-1000XM5`.
- **Dotaz se překládá pro zdroj:** do AliExpressu anglicky, do Allegra česky
  (případně polsky pro polské nabídky). Uživatel to nevidí, vidí jen výsledky.

### Technika

- **Hybridní vyhledávání:** lexikální (BM25) + vektorové (vícejazyčné embeddingy)
  + učené řazení z našich kliků a nákupů.
- **Engine:** na začátek Typesense nebo Meilisearch (rychlý našeptávač,
  hybridní hledání, jednoduchý provoz), při růstu OpenSearch nebo Vespa.
- **Porozumění dotazu jazykovým modelem** běží až po odeslání, paralelně
  s lexikálními výsledky: rozpozná záměr (konkrétní věc / kategorie a potřeba /
  příslušenství a kompatibilita) a vytáhne atributy. Výsledky se ukážou
  okamžitě a pak se upřesní („Upřesnili jsme: sluchátka s ANC do 1 500 Kč“).
  Pro každé stisknutí klávesy model nevoláme, je to pomalé a drahé.
- **Vizuální hledání (fáze 2):** fotka nebo screenshot → věc. V rozšíření
  pravým tlačítkem na obrázek „Ulovit tohle“.

## Karta věci: „nejdřív věc pochopit, pak nabídky“

Podle záměru dotazu vznikne jeden ze tří typů stránky:

| Záměr | Příklad | Stránka |
|---|---|---|
| Konkrétní věc | „aurion buds 3 pro“ | **Karta věci** s nabídkami |
| Kategorie / potřeba | „tichý vysavač do bytu“ | **Průvodce výběrem**: na co se dívat (3–5 klíčových parametrů vysvětlených lidsky), tipy podle rozpočtu, pak nabídky |
| Příslušenství / díl | „baterie do …“ | **Kompatibilita**: co přesně sedí, s jistotou a zdrojem |

### Pořadí sekcí karty věci (shora dolů)

1. **Hlavička:** galerie (swipe), čistý český název, čipy variant (barva,
   velikost, kapacita). Změna varianty přepočítá nabídky.
2. **Verdikt v kostce:** „Nejlepší celková cena 1 454 Kč, doma v pátek“,
   3 plusy a 2 minusy z recenzí napříč tržišti („z 2 341 recenzí na 2 tržištích“),
   rozložení hvězd.
3. **Nabídky:** první dvě musí být vidět už na první obrazovce, protože kvůli
   nim uživatel přišel. Detail v [04-cena-a-nabidky.md](04-cena-a-nabidky.md).
4. **Klíčové parametry:** 6 nejdůležitějších pro kategorii, srovnatelné
   s alternativami. „Všechny parametry“ se rozbalí; u každého zdroj a
   upozornění na rozpory mezi zdroji.
5. **Vývoj ceny:** 30 / 90 / 365 dní, nejnižší cena za 30 dní, jednoduchý
   ukazatel „Je teď dobrá chvíle?“.
6. **Recenze:** souhrn, fotky, filtr podle tržiště, překlad do češtiny.
7. **Otázky a odpovědi:** odpovědi z parametrů a recenzí se zdrojem
   („podle 14 recenzí“).
8. **Alternativy a příslušenství:** levnější, lepší, podobná.
9. **Transparentnost:** „Jak řadíme nabídky“, zdroje dat, čas aktualizace.

### Jak karta vzniká

- **Předgenerované karty** pro katalog (dávkově, na pozadí), aktualizované
  při změně dat.
- **Nová věc na vyžádání:** nejdřív se ukážou nabídky, AI části se dosypávají
  se skutečným průběhem („čtu 1 280 recenzí…“), ne s nekonečným kolečkem.
- **Pravidla kvality:** model píše jen z načtených zdrojů, každé tvrzení
  má odkaz na zdroj, čísla se po vygenerování automaticky porovnají se
  zdrojovými parametry. Nesoulad = sekce se nezobrazí a jde do fronty.

### Jazykový model

- Výchozí model: **Claude Opus 5.5** (`claude-opus-5-5`) pro syntézu karty,
  souhrny recenzí a překlad. Strukturované výstupy (JSON schéma) pro extrakci.
- **Message Batches API** pro hromadné předgenerování a překlady: běží
  asynchronně za polovinu ceny.
- **Prompt caching** pro neměnný systémový prompt a slovníky kategorií.
- Levnější modely (Claude Sonnet 5.5 za 2 / 10 USD, Claude Haiku 4.5 za
  1 / 5 USD za milion vstupních / výstupních tokenů, oproti Opus 5.5 za
  4 / 20 USD; ceník Anthropic k 25. 9. 2026) má smysl zvážit pro klasifikaci
  dotazů, až evaluace ukážou, že kvalita drží. To je obchodní rozhodnutí.
