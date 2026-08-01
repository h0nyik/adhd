# Browser Connector (MCP)

Osobní MCP konektor, který dovolí AI agentovi (Claude Desktop/Code) hledat a
číst webové stránky přes **tvůj vlastní, už přihlášený prohlížeč** — místo
přes samostatného bot klienta.

## Co to je a co ne

- **Je to:** jeden uživatel, jeden prohlížeč, jedna session. Agent dostane
  přístup jen na domény, které mu explicitně povolíš, a jen v rozsahu, který
  už máš v prohlížeči sám odemčený (přihlášení, cookies).
- **Není to:** anti-detect/bypass služba pro cizí bot provoz. Nic tu
  nemaskuje automatizovaný klient za člověka a neobchází ochrany webů — místo
  toho požadavek reálně vznikne ve tvém prohlížeči, takže žádné maskování ani
  není potřeba. Weby, které blokují i běžné prohlížeče (CAPTCHA, aktivní
  bot-fight mód), jsou záměrně mimo rozsah tohoto nástroje.

## Architektura

```
Claude (Desktop/Code)
   │  MCP (stdio)
   ▼
server/  — Node/TS MCP server
   │  WebSocket, 127.0.0.1 only, token + origin auth
   ▼
extension/  — Chrome MV3 rozšíření (běží ve TVÉM prohlížeči)
   │  chrome.tabs / chrome.scripting
   ▼
reálná stránka, s tvými cookies a session
```

MCP server i websocket most běží lokálně na tvém stroji vedle Claude Desktop/
Code — nic neopouští localhost, není potřeba žádný VPN ani veřejně
dostupný port.

## Nástroje (MCP tools)

- `browser_search(query)` — otevře vyhledávání (výchozí Google) a vrátí
  extrahovaný text výsledků.
- `browser_fetch(url)` — otevře konkrétní URL a vrátí extrahovaný text
  stránky.

Obě fungují jen pro domény v allowlistu (viz níže) — jinak nástroj vrátí
chybu s vysvětlením, ne tichý pád.

## Setup

1. **Server**
   ```bash
   cd server
   npm install
   npm run build
   npm run start
   ```
   Při startu se vypíše pairing token a port — zkopíruj si token, budeš ho
   potřebovat v kroku 3.

2. **Rozšíření**
   - Otevři `chrome://extensions`, zapni Developer mode.
   - „Load unpacked" → vyber složku `extension/`.

3. **Spárování**
   - Klikni na ikonu rozšíření → vlož port (výchozí `8765`) a token ze
     startu serveru → Uložit.
   - Badge ikony by měl přejít na `ON`.

4. **Allowlist**
   - V popupu přidej domény, které smí agent navštěvovat (např.
     `wikipedia.org`, `google.com` pro vyhledávání). Chrome si při každém
     přidání vyžádá potvrzení oprávnění.

5. **Claude Desktop/Code**

   Přidej do MCP konfigurace (`claude_desktop_config.json` nebo ekvivalent):
   ```json
   {
     "mcpServers": {
       "browser-connector": {
         "command": "node",
         "args": ["/absolutní/cesta/k/adhd/server/dist/index.js"]
       }
     }
   }
   ```

## Bezpečnostní model

- **Jen loopback.** Websocket server poslouchá výhradně na `127.0.0.1`,
  nikdy není dostupný ze sítě.
- **Token + origin kontrola.** Připojit se může jen rozšíření se správným
  párovacím tokenem a s origin `chrome-extension://…` — webová stránka,
  která by si sama zkusila otevřít websocket na stejný port, je odmítnuta.
- **Domain allowlist vynucená Chromem.** Rozšíření startuje bez jakýchkoli
  host permissions (`optional_host_permissions`); každá doména se přidává
  přes `chrome.permissions.request`, takže povolení potvrzuje sám Chrome,
  ne jen aplikační logika.
- **Viditelný indikátor.** Badge ikony ukazuje `ON` / `...` (aktivní úkol) /
  `II` (pozastaveno) — nikdy tiché ovládání na pozadí.
- **Kill switch.** Tlačítko „Pozastavit" v popupu okamžitě přestane
  zpracovávat úkoly.
- **Cookies nikdy neopouští prohlížeč.** Přes most se posílá jen extrahovaný
  text stránky, ne cookies ani jiné přihlašovací údaje.

## Známá omezení (MVP)

- Vyžaduje otevřený Chrome s načteným rozšířením; agent nemůže hledat, když
  je prohlížeč zavřený.
- MV3 service worker může po ~30 s nečinnosti usnout — zmírněno periodickým
  `chrome.alarms` keepalive, ale krátký výpadek při probouzení je možný.
- Extrakce obsahu je jednoduchá (`innerText` bez scriptů/stylů), ne plnohodnotný
  readability parser.
- `browser_search` má natvrdo Google (`extension/background.js`, `SEARCH_URL`) —
  změna vyhledávače je jeden řádek.
- Žádné řešení CAPTCHA ani obcházení bot-detekce — to je záměrně mimo scope.

## Možné další kroky (ne v tomto MVP)

- Lokální companion app (Playwright nad reálným profilem prohlížeče) pro
  úlohy na pozadí, kdy nemáš Chrome otevřený.
- Podpora Firefoxu (WebExtensions API je téměř shodné).
- Kvalitnější extrakce obsahu (Readability.js) a podpora vyplňování formulářů.
