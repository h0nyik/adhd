const $ = (id) => document.getElementById(id);

async function getSettings() {
  const s = await chrome.storage.local.get(["serverPort", "pairingToken", "allowlist", "paused", "activityLog"]);
  return {
    serverPort: s.serverPort || 8765,
    pairingToken: s.pairingToken || "",
    allowlist: s.allowlist || [],
    paused: !!s.paused,
    activityLog: s.activityLog || [],
  };
}

function render(settings) {
  $("status").textContent = settings.paused
    ? "Pozastaveno"
    : settings.pairingToken
      ? "Nakonfigurováno — viz badge ikony pro live stav (ON/…)"
      : "Chybí pairing token ze serveru";

  $("pauseBtn").textContent = settings.paused ? "Obnovit" : "Pozastavit (kill switch)";
  $("port").value = settings.serverPort;
  $("token").value = settings.pairingToken;

  const allowlistEl = $("allowlist");
  allowlistEl.innerHTML = "";
  for (const domain of settings.allowlist) {
    const li = document.createElement("li");
    const label = document.createElement("span");
    label.textContent = domain;
    const rm = document.createElement("button");
    rm.textContent = "×";
    rm.title = "Odebrat";
    rm.addEventListener("click", () => removeDomain(domain));
    li.append(label, rm);
    allowlistEl.appendChild(li);
  }

  const activityEl = $("activity");
  activityEl.innerHTML = "";
  for (const entry of settings.activityLog.slice(0, 10)) {
    const li = document.createElement("li");
    li.textContent = `[${new Date(entry.at).toLocaleTimeString()}] ${entry.text}`;
    activityEl.appendChild(li);
  }
}

async function refresh() {
  render(await getSettings());
}

function normalizeDomain(input) {
  return input.trim().replace(/^[a-z]+:\/\//i, "").replace(/\/.*$/, "").toLowerCase();
}

async function addDomain() {
  const domain = normalizeDomain($("newDomain").value);
  if (!domain) return;

  const granted = await chrome.permissions.request({
    origins: [`https://${domain}/*`, `http://${domain}/*`],
  });
  if (!granted) {
    alert("Bez povolení Chromu nelze doménu přidat.");
    return;
  }

  const { allowlist } = await getSettings();
  if (!allowlist.includes(domain)) {
    await chrome.storage.local.set({ allowlist: [...allowlist, domain] });
  }
  $("newDomain").value = "";
  refresh();
}

async function removeDomain(domain) {
  const { allowlist } = await getSettings();
  await chrome.storage.local.set({ allowlist: allowlist.filter((d) => d !== domain) });
  await chrome.permissions.remove({ origins: [`https://${domain}/*`, `http://${domain}/*`] });
  refresh();
}

async function saveConnection() {
  await chrome.storage.local.set({
    serverPort: Number($("port").value) || 8765,
    pairingToken: $("token").value.trim(),
  });
  refresh();
}

async function togglePause() {
  const { paused } = await getSettings();
  await chrome.storage.local.set({ paused: !paused });
  refresh();
}

$("addDomain").addEventListener("click", addDomain);
$("saveConn").addEventListener("click", saveConnection);
$("pauseBtn").addEventListener("click", togglePause);

refresh();
chrome.storage.onChanged.addListener(refresh);
