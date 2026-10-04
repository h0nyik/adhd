# Překlad a sjednocení textů

## Cíl a realita

Cíl: všechny texty čtou, jako by je psal český copywriter. Stoprocentně
bezchybný strojový překlad neexistuje, proto stavíme na čtyřech věcech:

1. **Přepisujeme, nepřekládáme.** Z nabídky vytáhneme fakta a český text
   napíšeme znovu podle šablony kategorie.
2. **Terminologie** podle slovníku kategorie.
3. **Automatické kontroly** čísel, jednotek a kódů.
4. **Lidské doladění** tam, kde je nejvíc vidět (slovníky a šablony top
   kategorií, nahlášené chyby).

## Postup

| Krok | Co se děje | Příklad |
|---|---|---|
| 1. Čištění | Z titulku pryč SEO balast | „2026 New Hot Sale Original TWS Wireless Earbuds Bluetooth 5.3 ANC…“ |
| 2. Extrakce | Značka, model, varianta, parametry s jednotkami, obsah balení (JSON podle schématu) | `{typ: sluchátka do uší, bt: 5.3, anc: true}` |
| 3. Český obsah | Název podle šablony, krátký popis, parametry | „Bezdrátová sluchátka do uší s ANC, Bluetooth 5.3“ |
| 4. Slovník | Termíny kategorie | earbuds → sluchátka do uší (ne „ušní pecky“), power bank → powerbanka |
| 5. Invarianty | Čísla, jednotky, rozměry, kódy modelů a velikosti musí sedět se zdrojem | 6.7 inch → 6,7″ (17 cm) |
| 6. Typografie | České uvozovky „“, desetinná čárka, `5 000 mAh`, nezlomitelné mezery po v, k, s, z, o, u, a, i | „v krabici“ se nerozdělí na konci řádku |
| 7. Kontrola kvality | Hodnocení jazykovým modelem podle rubriky (věrnost, plynulost, terminologie, invarianty), zpětný překlad čísel; nízké skóre = nový pokus nebo fronta k revizi | — |
| 8. Mezipaměť | Klíč = hash zdroje + verze slovníku; při změně slovníku přegenerovat | — |

**Velikosti oblečení a bot:** převod US / EU / CN velikostí do EU s tabulkou
a upozorněním, jen když na to data ukazují (např. recenze „velikost je menší“).

**Recenze:** překládají se líně (až při zobrazení) a ukládají se. Souhrn
recenzí je česky napříč jazyky.

## Kde překlad běží

| Kde | Na co | Technologie |
|---|---|---|
| Server, dávkově | Kanonický obsah karet a nabídek | Claude Opus 5.5 přes Message Batches API (asynchronně, poloviční cena), prompt caching pro systémový prompt a slovník |
| iPhone, iPad, Mac | Okamžité a soukromé věci (recenze na vyžádání, poznámky uživatele) | Apple Foundation Models (model v zařízení od iOS 26) |
| Chrome a rozšíření | Text na stránce tržiště | Chrome Translator API (v zařízení, Chrome 138+) |
| Android | Totéž | ML Kit (překlad v zařízení) |

## Originál

V menu „…“ u nabídky je nenápadná volba **„Původní text“**. Doporučuji ji
nechat: při sporu s prodejcem se řeší originál, a když překlad u velikosti
nebo materiálu chybuje, uživatel si to ověří. Běžně ji nikdo neuvidí, ale
zvyšuje důvěru.
