# Produkt: vize, pro koho, rozsah MVP

## Jednou větou

Napíšeš, co chceš. Ulovko věc rozebere, ukáže o ní všechno podstatné a vedle toho
všechny nabídky z Allegra a AliExpressu (později Aukra a dalších) s **jednou
cenou se vším až do schránky**, s poctivým hodnocením prodejce a doručením
na konkrétní den.

## Problém, který řešíme

1. **Ceny nejsou srovnatelné.** U zboží mimo EU se k ceně přidává DPH, od
   1. 7. 2026 paušální clo 3 € za každou tarifní položku v zásilce do 150 €
   a nejpozději od 1. 11. 2026 nový manipulační poplatek EU (výši stanoví Komise).
   Na Allegru zase rozhoduje doprava a podmínky Smart!. Člověk porovnává
   jablka s hruškami.
2. **Skryté náklady zabíjejí nákup.** Podle Baymard Institute opouští košík
   zhruba 70 % nakupujících a nejčastějším důvodem (39 %) jsou příliš vysoké
   dodatečné náklady (doprava, daně, poplatky).
3. **Jazyk a chaos v nabídkách.** Titulky na AliExpressu jsou často snůška
   klíčových slov, parametry jsou v palcích a čínských velikostech.
4. **Nedůvěra.** Hodnocení prodejců je na každém tržišti jinak a jinak se
   počítá. U Číny se lidé bojí cla, falešných kusů a dlouhého čekání.

## Komu to slouží

| Persona | Co chce | Co jí dáme |
|---|---|---|
| **Lovec slev** (18–35) | Nejnižší celkovou cenu, rád porovnává | Seřazení podle ceny se vším, hlídání ceny, úlovky |
| **Praktik** | Mít to rychle a bez starostí, 50 Kč neřeší | „Nejlepší volba“, doručení na den, vrácení zdarma |
| **Opatrný** | Neprodělat, nenaletět | Skóre důvěry, ověřené recenze, vysvětlené clo |

## Tři pilíře hodnoty

1. **Jedna cena se vším.** Velké číslo „Celkem do schránky“ a rozpad na jedno
   klepnutí. Stav ceny: ověřeno / odhad, s časem ověření. Viz
   [04-cena-a-nabidky.md](04-cena-a-nabidky.md).
2. **Karta věci.** Všechno o hledané věci na jedné stránce: parametry, verdikt
   z recenzí napříč tržišti, vývoj ceny, alternativy. Viz
   [03-vyhledavani-a-karta.md](03-vyhledavani-a-karta.md).
3. **Důvěra.** Sjednocené skóre prodejce, recenze za 30 sekund, Cenová jistota
   (když bude v košíku dráž, než jsme slíbili, rozdíl vrátíme v úlovcích).
   Viz [06-recenze-a-odmeny.md](06-recenze-a-odmeny.md).

A průřezově: **všude po ruce.** Sdílení odkazu z aplikace tržiště, rozšíření
prohlížeče, widget, globální zkratka na počítači. Viz [05-ux-ui.md](05-ux-ui.md).

## Čím se liší od existujících služeb

- **Srovnávače (Heureka, Zboží.cz)** srovnávají hlavně české e-shopy. My
  srovnáváme tržiště včetně zahraničních a počítáme clo a poplatky.
- **Google Shopping** neřeší celkovou cenu do ČR včetně cla ani recenze
  sjednocené napříč tržišti.
- **Aplikace samotných tržišť** srovnávají jen samy se sebou.
- **Cashbackové portály** dávají odměnu, ale nepomohou s výběrem.

## Rozsah MVP (fáze 1)

**Trh:** Česko, CZK, čeština.
**Zdroje:** AliExpress (Affiliate API) a Allegro (REST API, nutná verifikace
aplikace). Aukro ve fázi 2 po dohodě o datech.
**Platformy:** iOS, Android, rozšíření prohlížeče (Chrome, Edge, Opera,
Firefox), web s veřejnými kartami věcí (také jako instalovatelná PWA pro
macOS a Windows). Nativní macOS, Windows a Safari rozšíření ve fázi 2.

**V MVP:**
- Vyhledávání s našeptávačem (slova, kategorie, věci, atributy, překlepy).
- Karta věci (konkrétní produkt) a průvodce kategorií (pro obecné dotazy).
- Seznam nabídek s celkovou cenou, doručením, skóre prodejce a počtem prodaných.
- Cenový engine s DPH, clem, poplatky a kurzy.
- Hlídání ceny s notifikací.
- Chytré předání k nákupu (deep link do aplikace tržiště s affiliate značkou).
- Recenze za 30 sekund a úlovky (bez výběru peněz).
- Účet: passkeys, Sign in with Apple, Google, e-mail; funguje i bez účtu.

**Mimo MVP:** nákup přímo v aplikaci, košík napříč tržišti, aukce, vizuální
vyhledávání, výběr odměn na účet, další země.

## Jak poznáme, že to funguje

| Metrika | Cíl (k doladění) |
|---|---|
| **Severní hvězda:** týdně aktivní uživatelé, kteří přešli k nákupu | roste týden po týdnu |
| Přesnost ceny: podíl předání, kde se cena v košíku liší od naší ověřené o méně než 1 % | ≥ 98 % |
| Úspěch hledání: klik do 3 nejlepších výsledků | ≥ 60 % |
| Rychlost: první výsledky (p95) | ≤ 1 s |
| Recenze na ověřený nákup | ≥ 25 % |
| Retence D30 | sledovat od prvního dne |
