import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { ExtensionBridge } from "./bridge.js";
import type { PageExtract } from "./types.js";

const port = Number(process.env.BROWSER_CONNECTOR_PORT) || 8765;
const bridge = new ExtensionBridge(port);

function formatResult(result: PageExtract) {
  return {
    content: [
      {
        type: "text" as const,
        text: `# ${result.title}\n${result.url}\n\n${result.text}`,
      },
    ],
  };
}

const server = new McpServer({
  name: "browser-connector",
  version: "0.1.0",
});

server.tool(
  "browser_search",
  "Vyhledá dotaz na webu skrze uživatelův vlastní, přihlášený prohlížeč (ne přes cizí bot klienta) a vrátí extrahovaný text výsledků. " +
    "Funguje jen pro domény, které uživatel explicitně povolil v popupu rozšíření (výchozí je google.com pro vyhledávání).",
  { query: z.string().min(1) },
  async ({ query }) => formatResult(await bridge.runTask({ type: "search", query })),
);

server.tool(
  "browser_fetch",
  "Otevře konkrétní URL v reálném prohlížeči uživatele (s jeho cookies a přihlášenou session) a vrátí extrahovaný text stránky. " +
    "Funguje jen pro domény, které uživatel explicitně povolil v popupu rozšíření.",
  { url: z.string().url() },
  async ({ url }) => formatResult(await bridge.runTask({ type: "fetch", url })),
);

const transport = new StdioServerTransport();
await server.connect(transport);
console.error("[mcp] browser-connector MCP server running on stdio");
