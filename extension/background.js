// MV3 service worker: bridges the local MCP server to the user's real browser.
// The server never talks HTTP directly — it only ever sends a task over this
// socket, and this file is the only place that touches real tabs/cookies.

const DEFAULT_PORT = 8765;
const SEARCH_URL = (q) => `https://www.google.com/search?q=${encodeURIComponent(q)}`;

let socket = null;
let reconnectTimer = null;

async function getSettings() {
  const s = await chrome.storage.local.get(["serverPort", "pairingToken", "allowlist", "paused"]);
  return {
    serverPort: s.serverPort || DEFAULT_PORT,
    pairingToken: s.pairingToken || "",
    allowlist: s.allowlist || [],
    paused: !!s.paused,
  };
}

function setBadge(text, color) {
  chrome.action.setBadgeText({ text });
  if (color) chrome.action.setBadgeBackgroundColor({ color });
}

async function logActivity(text) {
  const { activityLog } = await chrome.storage.local.get(["activityLog"]);
  const log = activityLog || [];
  log.unshift({ text, at: Date.now() });
  await chrome.storage.local.set({ activityLog: log.slice(0, 50) });
}

async function connect() {
  const { serverPort, pairingToken, paused } = await getSettings();

  if (paused) {
    setBadge("II", "#888888");
    return;
  }
  if (!pairingToken) {
    setBadge("!", "#cc0000");
    return;
  }
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
    return;
  }

  socket = new WebSocket(`ws://127.0.0.1:${serverPort}/?token=${encodeURIComponent(pairingToken)}`);

  socket.addEventListener("open", () => {
    setBadge("ON", "#2e7d32");
    logActivity("Připojeno k lokálnímu MCP serveru.");
  });

  socket.addEventListener("close", () => {
    setBadge("", null);
    scheduleReconnect();
  });

  socket.addEventListener("error", () => {
    // "close" fires right after; reconnect is handled there.
  });

  socket.addEventListener("message", (event) => handleTask(event.data));
}

function scheduleReconnect() {
  if (reconnectTimer) return;
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect();
  }, 3000);
}

function send(msg) {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(msg));
  }
}

function hostnameOf(url) {
  try {
    return new URL(url).hostname;
  } catch {
    return "";
  }
}

async function isAllowed(url) {
  const { allowlist } = await getSettings();
  const host = hostnameOf(url);
  return allowlist.some((d) => host === d || host.endsWith(`.${d}`));
}

async function handleTask(raw) {
  let task;
  try {
    task = JSON.parse(raw);
  } catch {
    return;
  }
  const { id, type } = task;

  try {
    const { paused } = await getSettings();
    if (paused) throw new Error("Konektor je pozastavený (viz popup rozšíření).");

    const targetUrl = type === "search" ? SEARCH_URL(task.query) : type === "fetch" ? task.url : null;
    if (!targetUrl) throw new Error(`Neznámý typ úkolu: ${type}`);

    if (!(await isAllowed(targetUrl))) {
      throw new Error(`Doména "${hostnameOf(targetUrl)}" není v allowlistu. Přidej ji v popupu rozšíření.`);
    }

    const result = await runInTab(targetUrl);
    await logActivity(`${type === "search" ? "Hledání" : "Načtení"}: ${targetUrl}`);
    send({ id, ok: true, data: result });
  } catch (err) {
    send({ id, ok: false, error: String(err?.message || err) });
  }
}

async function runInTab(url) {
  const tab = await chrome.tabs.create({ url, active: false });
  setBadge("...", "#f57c00");
  try {
    await waitForTabComplete(tab.id);
    const [{ result }] = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: extractPageContent,
    });
    return result;
  } finally {
    chrome.tabs.remove(tab.id).catch(() => {});
    setBadge("ON", "#2e7d32");
  }
}

function waitForTabComplete(tabId, timeoutMs = 20000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      chrome.tabs.onUpdated.removeListener(listener);
      reject(new Error("Stránka se nenačetla včas."));
    }, timeoutMs);

    function listener(updatedTabId, info) {
      if (updatedTabId === tabId && info.status === "complete") {
        clearTimeout(timer);
        chrome.tabs.onUpdated.removeListener(listener);
        resolve();
      }
    }
    chrome.tabs.onUpdated.addListener(listener);
  });
}

// Runs inside the target page via chrome.scripting.executeScript — no access
// to the service worker's closure, so it must be fully self-contained.
function extractPageContent() {
  const clone = document.body ? document.body.cloneNode(true) : null;
  clone?.querySelectorAll("script,style,noscript").forEach((el) => el.remove());
  const text = (clone ? clone.innerText : "").replace(/\n{3,}/g, "\n\n").trim().slice(0, 20000);
  return { url: location.href, title: document.title, text };
}

// MV3 service workers get killed after ~30s idle, which would silently drop
// the socket; a periodic alarm wakes this worker so it can reconnect.
chrome.alarms.create("keepalive", { periodInMinutes: 0.5 });
chrome.alarms.onAlarm.addListener((alarm) => {
  if (alarm.name === "keepalive") connect();
});

chrome.runtime.onStartup.addListener(connect);
chrome.runtime.onInstalled.addListener(connect);
chrome.storage.onChanged.addListener((changes) => {
  if (changes.pairingToken || changes.serverPort || changes.paused) {
    socket?.close();
    connect();
  }
});

connect();
