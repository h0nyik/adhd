import { randomBytes, randomUUID } from "node:crypto";
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import type { IncomingMessage } from "node:http";
import { homedir } from "node:os";
import { join } from "node:path";
import { WebSocket, WebSocketServer } from "ws";
import type { BridgeResponse, PageExtract } from "./types.js";

const CONFIG_DIR = join(homedir(), ".browser-connector");
const TOKEN_PATH = join(CONFIG_DIR, "token");

// Only the extension's own background page origin may pair; a webpage's
// `new WebSocket(...)` call runs from a different origin and is rejected here.
const ALLOWED_ORIGIN_PREFIX = "chrome-extension://";

function loadOrCreateToken(): string {
  if (!existsSync(CONFIG_DIR)) mkdirSync(CONFIG_DIR, { recursive: true });
  if (existsSync(TOKEN_PATH)) return readFileSync(TOKEN_PATH, "utf8").trim();
  const token = randomBytes(24).toString("hex");
  writeFileSync(TOKEN_PATH, token, { mode: 0o600 });
  return token;
}

interface PendingTask {
  resolve: (data: PageExtract) => void;
  reject: (err: Error) => void;
  timer: ReturnType<typeof setTimeout>;
}

/**
 * Local-only bridge to the browser extension. Talks to exactly one paired
 * extension instance over a loopback websocket; never listens beyond 127.0.0.1.
 */
export class ExtensionBridge {
  private readonly wss: WebSocketServer;
  private socket: WebSocket | null = null;
  private readonly pending = new Map<string, PendingTask>();
  readonly token: string;
  readonly port: number;

  constructor(port = 8765) {
    this.port = port;
    this.token = loadOrCreateToken();
    this.wss = new WebSocketServer({ host: "127.0.0.1", port });
    this.wss.on("connection", (ws, req) => this.handleConnection(ws, req));

    console.error(`[bridge] listening on ws://127.0.0.1:${port} (loopback only)`);
    console.error(`[bridge] pairing token (paste once into the extension popup): ${this.token}`);
  }

  private handleConnection(ws: WebSocket, req: IncomingMessage): void {
    const origin = req.headers.origin ?? "";
    const url = new URL(req.url ?? "/", "http://localhost");
    const suppliedToken = url.searchParams.get("token");

    if (!origin.startsWith(ALLOWED_ORIGIN_PREFIX) || suppliedToken !== this.token) {
      ws.close(4001, "unauthorized");
      return;
    }

    // Only one browser should drive this connector at a time.
    this.socket?.close(4002, "replaced by new connection");
    this.socket = ws;
    console.error("[bridge] extension connected");

    ws.on("message", (raw) => this.handleMessage(raw.toString()));
    ws.on("close", () => {
      if (this.socket === ws) this.socket = null;
      console.error("[bridge] extension disconnected");
    });
  }

  private handleMessage(raw: string): void {
    let msg: BridgeResponse;
    try {
      msg = JSON.parse(raw);
    } catch {
      return;
    }
    const pending = this.pending.get(msg.id);
    if (!pending) return;

    clearTimeout(pending.timer);
    this.pending.delete(msg.id);
    if (msg.ok && msg.data) {
      pending.resolve(msg.data);
    } else {
      pending.reject(new Error(msg.error ?? "Neznámá chyba v rozšíření."));
    }
  }

  async runTask(task: { type: "search"; query: string } | { type: "fetch"; url: string }, timeoutMs = 30_000): Promise<PageExtract> {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      throw new Error(
        "Prohlížeč není připojený. Otevři Chrome s nainstalovaným rozšířením a zkontroluj stav v popupu."
      );
    }

    const id = randomUUID();
    const socket = this.socket;

    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.pending.delete(id);
        reject(new Error("Požadavek na prohlížeč vypršel (timeout)."));
      }, timeoutMs);

      this.pending.set(id, { resolve, reject, timer });
      socket.send(JSON.stringify({ id, ...task }));
    });
  }
}
