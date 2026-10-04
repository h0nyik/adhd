# Účet, propojení s tržišti a nákup

## Náš účet

- **Přihlášení:** passkeys jako hlavní cesta, dále Sign in with Apple, Google
  a e-mail s odkazem. Pokud nabízíme přihlášení přes třetí strany, Apple
  vyžaduje i soukromí šetrnou variantu (pravidlo 4.8 *Login Services*), což
  Sign in with Apple splňuje.
- **Bez povinné registrace:** hledat, porovnávat a předávat k nákupu jde bez
  účtu. Povinná registrace je podle Baymardu důvodem opuštění košíku u 19 %
  lidí. Účet nabídneme až ve chvíli, kdy dává smysl (hlídání ceny, úlovky).
- Jeden účet pro všechna zařízení: hlídané ceny, historie, úlovky, recenze.

## Propojení s tržišti: co jde a co ne

- **Jeden login ke všem tržištím vytvořit nemůžeme.** Účty na Allegru a
  AliExpressu patří uživateli a řídí se podmínkami tržišť.
- **Nikdy nežádáme hesla k tržištím a neukládáme jejich cookies.** Je to
  bezpečnostní riziko a porušení podmínek.
- **Dobrovolné propojení pro ověření nákupů:** přeposílání potvrzení
  objednávek na osobní adresu v Ulovku, nebo přístup jen ke čtení e-mailů
  s objednávkami (OAuth, filtr na odesílatele). Přínos pro uživatele: ověřené
  recenze, sledování zásilek, potvrzení úlovků.
- Podle veřejné dokumentace jsou API obou tržišť zaměřená na prodejce a
  affiliate partnery. Veřejné API pro nákup jménem kupujícího jsme nenašli
  (ověřit při jednání s tržišti).

## Nákup ve třech fázích

### Fáze 1: chytré předání (MVP)

1. Klepnutí na „Koupit na Allegru za 1 849 Kč“.
2. Těsně předtím se cena znovu ověří. Pokud se změnila, uživatel vidí novou
   cenu a cítí haptiku `varovani`.
3. Otevře se **přesná nabídka a varianta v aplikaci tržiště** (Universal
   Links na iOS, App Links na Androidu) s affiliate značkou. Uživatel je
   v aplikaci tržiště přihlášený, nákup dokončí na dvě klepnutí.
   Bez nainstalované aplikace: Safari na iOS, Custom Tabs na Androidu
   (sdílejí přihlášení s Chromem).
4. Po návratu: „Koupil(a) jsi?“ jedním klepnutím. Ověření přes e-mail nebo
   affiliate data.

### Fáze 2: košík napříč tržišti

Plán nákupu přes víc tržišť najednou: výpočet na úrovni košíku (clo podle
tarifních položek, hranice dopravy zdarma), pak předání po jednotlivých
tržištích s kontrolním seznamem.

### Fáze 3: nákup přímo v aplikaci

Jen přes oficiální partnerství nebo standardy pro nákup přes agenty:
- **Universal Commerce Protocol (Google, leden 2026):** pokladna v AI Mode
  a Gemini; zatím pro vybrané obchodníky v USA, globální rozšíření plánované
  během roku 2026.
- **Agentic Commerce Protocol (OpenAI a Stripe):** otevřený standard, stabilní
  specifikace z 17. 4. 2026. Okamžitou pokladnu v ChatGPT ale OpenAI v březnu
  2026 ukončilo, trh je zatím nezralý.
- Programy karetních společností pro platby agentů (Visa Intelligent Commerce,
  Mastercard Agent Pay).

Veřejné informace, že by Allegro nebo AliExpress tyto standardy podporovaly
pro aplikace třetích stran, jsme nenašli. Sledovat a jednat.

Varianta „být prodejcem“ přes AliExpress Dropshipping API znamená převzít
reklamace, záruky, DPH a spotřebitelské povinnosti prodávajícího. Pro náš
model nedoporučuji.
