# Celková cena a seznam nabídek

## Princip

Jedno velké číslo: **Celkem do schránky.** Všechno ostatní je rozpad na jedno
klepnutí. Žádné „+ doprava“, žádné „+ clo podle země“.

Výzkum říká proč: při rozdělené ceně (cena + poplatky zvlášť) si lidé pamatují
nižší celkovou částku a krátkodobě víc nakupují (Morwitz, Greenleaf a Johnson,
*Journal of Marketing Research*, 1998). Tržiště to proto dělají. Jenže
překvapivé náklady jsou nejčastějším důvodem opuštění košíku (Baymard). My jako
srovnávač děláme opak a stavíme na tom důvěru.

## Cenový engine

```
Celkem = cena × ks
       + doprava (zvolená nejlevnější rozumná metoda, nebo uživatelova)
       + pro zásilky ze zemí mimo EU:
           DPH 21 % (pokud ji tržiště nevybírá a nezahrnuje v ceně)
           clo
           manipulační poplatek EU
           poplatek dopravce za proclení (jen nad 150 €)
       − automaticky uplatněné slevy
Kupóny a body tržiště: zvlášť řádek „s kupónem“, nikdy ne v hlavní ceně,
pokud je uplatnění nejisté.
```

### Pravidla pro zboží mimo EU (stav k 4. 10. 2026)

| Složka | Do 150 € (typicky AliExpress, IOSS) | Nad 150 € |
|---|---|---|
| DPH 21 % | Vybírá tržiště při platbě (IOSS), obvykle už v ceně pro EU | Při dovozu, často přes dopravce |
| Clo | **Od 1. 7. 2026 paušál 3 € za každou odlišnou tarifní položku** v zásilce (přechodně do 1. 7. 2028). Tržiště vystupuje jako dovozce a vybírá ho při platbě | Podle celního sazebníku |
| Manipulační poplatek EU | Členské státy ho mají vybírat **nejpozději od 1. 11. 2026**, výši stanoví Komise. Obíhající „2 €“ je odhad, ne schválená sazba | Ano |
| Poplatek dopravce | — | Podle dopravce |

**Co z toho plyne pro engine:**
- Pravidla jsou data, ne kód. Každé má platnost od–do a zdroj, aby šly změny
  nasadit bez vydání aplikace.
- Dokud není výše manipulačního poplatku potvrzená, nabídky s doručením po
  1. 11. 2026 mají stav **≈ odhad** a v rozpadu řádek „Manipulační poplatek
  EU: výše zatím nepotvrzena“.
- Zboží ze skladu v EU (AliExpress má i evropské sklady) clo nemá. Filtr
  „Bez cla (sklad v EU)“.
- **Clo-chytrý košík (fáze 2):** clo se platí za tarifní položku, ne za kus.
  Druhé tričko do stejné objednávky clo nezvýší, sluchátka k tričku ano.
  Košík to umí spočítat a poradit.

### Měna a kurz

- Účtuje-li tržiště v Kč, bereme jeho cenu.
- Jinak denní kurz ČNB plus nastavitelná přirážka karty (výchozí odhad,
  uživatel může nastavit svou banku). Takový řádek nese symbol ≈.

## Stav ceny a Cenová jistota

| Stav | Kdy | Zobrazení |
|---|---|---|
| ✓ **Ověřeno** | Načteno z API ≤ 15 min, všechny složky známé | „Ověřeno před 4 min“ |
| ≈ **Odhad** | Některá složka odhadnutá (kurz, tarifní zařazení, nepotvrzený poplatek) | „≈ 1 495 Kč“, v rozpadu označená složka |
| ! **Neověřeno** | Data starší než limit, API nedostupné | šedé, tlačítko „Ověřit teď“ |

**Cenová jistota:** pokud bude v košíku tržiště cena vyšší než naše
„✓ Ověřeno“ o víc než toleranci a uživatel to doloží (screenshot), rozdíl
vrátíme v úlovcích (s měsíčním stropem). Je to marketingový slib, který
zároveň kalibruje engine.

## Řádek nabídky

```
┌───────────────────────────────────────────────────┐
│ [AE]  1 454 Kč                       doma 14.–21. 10.│
│       ≈ se vším · clo 75 Kč v ceně                   │
│       Shenzhen Sound Lab · důvěra 74 · 101+ prodáno  │
│       Z Ulovka: 9 koupilo · 8 spokojeno   [Ohodnotit]│
└───────────────────────────────────────────────────┘
```

Pravidla:
- Sbalený řádek má nejvýš 4 krátké řádky. Celková cena je největší prvek
  (tabulkové číslice, aby se ceny pod sebou zarovnaly).
- **Doručení jako datum**, ne „7–15 dní“. Datum si člověk nemusí přepočítávat.
- Ikona tržiště vlevo, kvůli rychlému rozpoznání.
- Prodejce: jméno, skóre důvěry a jeden důvod v detailu („98 % kladných ·
  6 let · odesílá do 24 h“).
- Sociální důkaz: počet prodaných (sjednocená pásma) a náš vlastní ověřený
  údaj „Z Ulovka“.
- Max. jeden štítek na řádek: Nejlepší volba / Nejlevnější / Nejrychleji /
  Bez cla.
- Gesta: swipe doprava = hlídat cenu, swipe doleva = porovnat, klepnutí =
  detail s rozpadem ceny, dlouhý stisk = náhled. Každé gesto má i viditelnou
  alternativu v menu.

### Řazení a filtry

Segmentový přepínač: **Nejlepší volba** (výchozí) · Nejlevnější · Nejrychleji ·
Důvěra. Pod ním odkaz **„Jak řadíme?“** s vysvětlením vah (požadavek směrnice
Omnibus a nařízení P2B na transparentnost řazení).

Čipy filtrů: Doma do pátku · Bez cla (sklad v EU) · Vrácení zdarma ·
Jen ověření prodejci · (fáze 2: Nové / Použité / Aukce).

**Nejlepší volba** = vážené skóre: celková cena vůči nejnižší, rychlost
doručení, skóre důvěry, podmínky vrácení a jistota shody varianty. Uživatel si
jednou nastaví posuvník „cena ↔ rychlost“ a skóre se mu přizpůsobí.

**Proti zahlcení:** ukázat 5 nejlepších, zbytek sbalit („+ 12 dalších, dráž
o 200 Kč a víc“). Metaanalýza Cherneva a kol. (2015) ukazuje, že přebytek volby
škodí hlavně při složitém rozhodování a nejisté preferenci, což je přesně
situace srovnávání nabídek.

## Počitadlo prodaných

- Allegro API vrací popularitu jen v pásmech (0, 1–5, 6–10, 11–20, 21–50,
  51–100, 101+). **Stejná pásma použijeme pro všechna tržiště**, aby čísla
  byla srovnatelná.
- Vedle toho vlastní ověřené číslo „Z Ulovka: 23 koupilo za 30 dní“.
- Nikdy nesčítat čísla z různých zdrojů a nikdy je nezaokrouhlovat nahoru.

## Skóre důvěry prodejce (0–100)

- **Vstupy z tržišť:** Allegro (procento doporučení, počet hodnocení, status
  Super prodejce), AliExpress (hodnocení obchodu, procento kladné zpětné
  vazby, stáří obchodu).
- **Naše data:** dodržení slíbeného doručení, podíl problémů, recenze prodejce.
- **Bayesovské vyhlazení:** prodejce se 3 hodnoceními na 100 % nepředběhne
  prodejce s 10 000 hodnoceními na 98 %.
- Zobrazení: číslo + barva + ikona + slovní důvod. Nikdy jen barva
  (přístupnost).
