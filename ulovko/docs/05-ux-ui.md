# UX a UI: principy, gesta, haptika, nativnost

## Sedm principů a výzkum za nimi

| # | Princip | Proč (zdroj) | Co to znamená v aplikaci |
|---|---|---|---|
| 1 | **Rychlost je funkce** | Nielsenovy limity 0,1 / 1 / 10 s; Doherty a Thadani (IBM, 1982): odezva pod 400 ms udrží práci plynulou; Deloitte: −0,1 s = +8,4 % konverze v retailu | Kostry obrazovek s reálným rozvržením, optimistické UI, přednačtení karty při fokusu na návrh |
| 2 | **Jedno číslo, žádné překvapení** | Baymard: dodatečné náklady jsou důvod č. 1 opuštění košíku; Morwitz a kol. 1998: u rozdělené ceny si lidé pamatují nižší celek | Celkem do schránky jako hlavní číslo všude |
| 3 | **Méně, ale lépe** | Progresivní odhalování (Nielsen Norman Group); Chernev a kol. 2015: přebytek volby škodí u složitých rozhodnutí | 5 nejlepších nabídek, zbytek sbalený; 4 řádky na nabídku |
| 4 | **Ovladatelné palcem** | Hoober (2013): 49 % lidí drží telefon jednou rukou | Hlavní akce dole, listy (sheets) místo nových obrazovek |
| 5 | **Důvěra skrz poctivost** | Spiegel Research Center: pravděpodobnost nákupu vrcholí u hodnocení 4,2–4,5 hvězdy; pár negativních recenzí zvyšuje věrohodnost | Ukazovat rozložení hvězd a nejčastější výtky, nic neschovávat |
| 6 | **Odměna, ne manipulace** | Efekt darovaného pokroku (Nunes a Drèze 2006: 34 % vs. 19 % dokončení); DSA čl. 25 zakazuje temné vzorce, Digital Fairness Act se čeká ve 4. čtvrtletí 2026 | Viditelný pokrok, žádná falešná naléhavost |
| 7 | **Nativní všude** | Lidé znají ovládání svého systému | Systémové komponenty, značka žije v obsahu |

## Nativnost na každé platformě

- **iOS / iPadOS 27:** Liquid Glass. Systémové komponenty ho převezmou
  automaticky a iOS 27 už nedovolí ho vypnout. Navigace a lišty jsou systémové,
  naše identita je v typografii cen, barvě akcentu a ilustracích.
- **Android:** Material 3 Expressive (pružné animace, výraznější haptika,
  dynamické barvy). Prediktivní gesto zpět.
- **macOS:** okno s postranním panelem, panel na globální zkratku (jako
  Spotlight), Quick Look na mezerník.
- **Windows:** Fluent 2 (WinUI 3, materiál Mica), ikona v oznamovací oblasti
  a globální zkratka.
- **Rozšíření prohlížeče:** nenápadná pilulka na stránce produktu tržiště
  („Celkem se vším 1 849 Kč · jinde o 210 Kč levněji“), po klepnutí boční panel
  s kartou věci.

## Informační architektura

Spodní lišta: **Hledat** · **Hlídám** · **Úlovky** · **Profil**.

Klíčové obrazovky:
1. **Domov / Hledat:** pole hledání, hlídané ceny se sparklinem, nedávné.
2. **Hledání s našeptávačem** (viz [03](03-vyhledavani-a-karta.md)).
3. **Karta věci** s nabídkami.
4. **Detail nabídky** (list zespodu): rozpad ceny, prodejce, doručení, vrácení,
   tlačítko „Koupit na …“.
5. **Porovnání** 2–4 věcí vedle sebe se zvýrazněním rozdílů.
6. **Hlídání ceny:** cílová cena posuvníkem s haptickými zarážkami (nejnižší
   cena za 30 dní, průměr, aktuální).
7. **Recenze za 30 sekund** (viz [06](06-recenze-a-odmeny.md)).
8. **Úlovky:** body, úroveň, čekající odměny, recenze k napsání.

## Gesta

Každé gesto má viditelnou alternativu (tlačítko nebo menu): WCAG 2.2,
kritéria 2.5.1 *Pointer Gestures* a 2.5.7 *Dragging Movements*.

| Akce | iOS / iPadOS | Android | macOS | Windows | Rozšíření / web |
|---|---|---|---|---|---|
| Otevřít hledání | klepnutí na pole, tah dolů na domovu | totéž | ⌘K, globální zkratka (nastavitelná) | Ctrl+K, globální zkratka | klíčové slovo v adresním řádku, Ctrl/⌘+Shift+U |
| Hlídat cenu | swipe doprava na řádku | swipe doprava | ⌘D, kontextové menu | Ctrl+D | tlačítko |
| Porovnat | swipe doleva, nebo dlouhý stisk a tah do lišty porovnání | totéž | kontextové menu | kontextové menu | tlačítko |
| Náhled | dlouhý stisk (kontextové menu s náhledem) | dlouhý stisk | Force Click, mezerník (Quick Look) | mezerník | najetí myší |
| Hustota seznamu | sevření prstů nad seznamem (kompaktní ↔ pohodlný) | totéž | sevření na trackpadu | Ctrl + kolečko | — |
| Obnovit ceny | tah dolů s haptikou na prahu | totéž | ⌘R | F5 | tlačítko |
| Zpět | tah od okraje (systém) | prediktivní zpět | ⌘[ | Alt+← | systém |
| Rychlá recenze | dlouhý stisk notifikace → akce | akce v notifikaci | akce v notifikaci | akce v notifikaci | — |
| Procházení nabídek | — | — | J / K, Enter, S (řazení), W (hlídat), C (porovnat) | totéž | totéž |

## Haptika: jeden slovník pro všechny platformy

Haptika je definovaná sémanticky jako design token, platforma ji jen přeloží.

| Token | Kdy | iOS (SwiftUI `.sensoryFeedback`) | Android | macOS (Force Touch trackpad) | Web |
|---|---|---|---|---|---|
| `vyber` | segment řazení, zarážka posuvníku, výběr varianty | `.selection` | `HapticFeedbackConstants.SEGMENT_TICK` | `NSHapticFeedbackManager` `.alignment` | `navigator.vibrate(8)` |
| `prah` | swipe akce „zaklapne“, tah k obnovení | `.impact(weight: .medium)` | `GESTURE_THRESHOLD_ACTIVATE` | `.levelChange` | `vibrate(15)` |
| `uspech` | přidáno do hlídání, recenze odeslána | `.success` | `CONFIRM` | `.generic` | `vibrate([10,40,10])` |
| `chyba` | akce se nepovedla | `.error` | `REJECT` | `.generic` | `vibrate([30,40,30])` |
| `varovani` | cena se před nákupem změnila | `.warning` | kompozice `VibrationEffect` | `.levelChange` | `vibrate([20,60,20])` |
| `ulovek` | cena klesla pod cíl, připsané body | vlastní vzor Core Haptics (krátké stoupání a dvojklik) | `VibrationEffect.Composition`: `PRIMITIVE_QUICK_RISE` + `PRIMITIVE_CLICK` | `.generic` ×2 | `vibrate([8,30,8,30,24])` |

Pravidla:
- Haptika jen na akci uživatele nebo důležitou událost, nikdy průběžně při
  scrollování.
- Respektovat systémové nastavení a mít vlastní přepínač v aplikaci.
- Web: Vibration API funguje v Chrome na Androidu, Safari na iOS ho nepodporuje.
- Windows: haptika jen na některých perech a touchpadech (`Windows.Devices.Haptics`).
  Nepočítat s ní, zpětná vazba musí fungovat i vizuálně.

## Pohyb

- Pružinové animace (SwiftUI springs, Compose `spring()`), žádné lineární.
- Sdílené přechody: řádek nabídky se roztáhne do listu s detailem, miniatura
  věci se promění v hlavičku karty.
- Změna ceny: číslice se „protočí“ (1 849 → 1 799) a pípne haptika `vyber`.
  Jen když se cena opravdu změnila.
- Omezení pohybu v systému (Reduce Motion) = jen prolínání.

## Typografie a čísla

- Ceny v tabulkových číslicích, česky: `1 849 Kč` (nezlomitelná mezera jako
  oddělovač tisíců i před „Kč“), desetinná čárka.
- Datum česky a konkrétně: „čt 16. 10.“, „doma 14.–21. 10.“.
- Nezlomitelné mezery po jednopísmenných předložkách a spojkách (v, k, s, z,
  o, u, a, i). Detail, podle kterého lidé poznají česky psaný text.

## Integrace: „zkratka pro všechno“

- **iOS / iPadOS:** rozšíření pro sdílení (odkaz nebo obrázek z aplikace
  tržiště či Instagramu → karta věci), Safari rozšíření, App Intents (Siri,
  Zkratky, Spotlight: „Kolik stojí … celkem?“), widgety hlídaných cen, Live
  Activities (sledování zásilky), ovládání v Ovládacím centru, akční tlačítko
  přes Zkratky, integrace s Vizuální inteligencí (ověřit API).
- **Android:** cíl sdílení, zkratky aplikace (dlouhý stisk ikony), dlaždice
  Rychlého nastavení, widgety, akce v notifikacích.
- **macOS:** aplikace v řádku nabídek, panel na globální zkratku, menu Sdílet,
  Quick Look.
- **Windows:** ikona v oznamovací oblasti, globální zkratka, cíl sdílení.
- **Prohlížeče:** pilulka na stránce produktu, boční panel, kontextové menu
  „Ulovit tento obrázek“, klíčové slovo v adresním řádku. **Affiliate odkaz jen
  po akci uživatele a s jasným přínosem** (pravidla Chrome Web Store).

## Přístupnost

- Dynamické písmo do 200 %, rozvržení se přeskládá.
- VoiceOver / TalkBack čte cenu lidsky: „Celkem tisíc čtyři sta padesát čtyři
  korun, odhad, doručení čtrnáctého až dvacátého prvního října.“
- Kontrast WCAG AA, skóre důvěry nikdy jen barvou.
- Dotykové cíle 44 × 44 pt (Apple), 48 × 48 dp (Material).

## Zakázané vzory

- Falešná naléhavost („zbývají 2 ks“, pokud to nepotvrzují data), odpočty
  bez skutečného konce.
- Zahanbující odmítnutí („Ne, nechci šetřit“), předzaškrtnuté souhlasy,
  schované odhlášení.
- Nekonečný feed na domovské obrazovce.
- Notifikace bez hodnoty. Povolené: cena klesla, zásilka, žádost o recenzi
  po doručení, týdně maximum nastavitelné.
- Tón: tykání, přátelsky a věcně (otevřená otázka, viz [10](10-roadmapa-a-rizika.md)).
