// RAGE relay server.
// Run:  npm install && WHITELIST_URL="https://raw.githubusercontent.com/YOU/REPO/main/whitelist.json" node server.js
// Put it behind HTTPS (wss://) with a reverse proxy (Caddy / nginx) or a host that gives you TLS.

const { WebSocketServer } = require('ws');

const PORT = parseInt(process.env.PORT || '8080', 10);
const WHITELIST_URL = process.env.WHITELIST_URL || '';
const REFRESH_MS = 60 * 1000;

let allowed = null; // Set of lowercase uuids, or null = whitelist not configured (anyone may connect)

async function refreshWhitelist() {
  if (!WHITELIST_URL) return;
  try {
    const res = await fetch(WHITELIST_URL, { headers: { 'Cache-Control': 'no-cache' } });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const data = await res.json();
    if (data.enabled === false) {
      allowed = new Set();
    } else {
      allowed = new Set((data.players || []).map(p => String(p.uuid).toLowerCase()));
    }
    // kick anyone who got removed from the whitelist
    for (const [ws, info] of clients) {
      if (!allowed.has(info.uuid)) {
        send(ws, { t: 'deny', reason: 'removed from whitelist' });
        ws.close();
      }
    }
    console.log('whitelist loaded:', allowed.size, 'players');
  } catch (e) {
    console.error('whitelist refresh failed:', e.message);
  }
}

const clients = new Map(); // ws -> { uuid, name, server }
const wss = new WebSocketServer({ port: PORT, maxPayload: 8 * 1024 });

function clean(v, max = 200) {
  return String(v == null ? '' : v).replace(/[\u0000-\u001f]/g, ' ').slice(0, max);
}

function send(ws, obj) {
  if (ws.readyState === 1) ws.send(JSON.stringify(obj));
}

function broadcast(obj, except) {
  for (const [ws] of clients) {
    if (ws !== except) send(ws, obj);
  }
}

function sendUsers() {
  const list = [];
  for (const [, info] of clients) list.push({ name: info.name, uuid: info.uuid, server: info.server });
  for (const [ws] of clients) send(ws, { t: 'users', list });
}

wss.on('connection', (ws) => {
  let hits = 0;
  const rate = setInterval(() => { hits = 0; }, 10_000);

  ws.on('message', (raw) => {
    if (++hits > 40) return; // simple flood protection
    let m;
    try { m = JSON.parse(raw.toString()); } catch { return; }
    const me = clients.get(ws);

    if (m.t === 'hello') {
      const uuid = clean(m.uuid, 40).toLowerCase();
      if (allowed && !allowed.has(uuid)) {
        send(ws, { t: 'deny', reason: 'not whitelisted' });
        ws.close();
        return;
      }
      clients.set(ws, { uuid, name: clean(m.name, 16), server: clean(m.server, 60) });
      sendUsers();
      return;
    }
    if (!me) return; // must say hello first

    switch (m.t) {
      case 'pm': {
        const to = clean(m.to, 16).toLowerCase();
        for (const [cws, info] of clients) {
          if (info.name.toLowerCase() === to) {
            send(cws, { t: 'pm', from: me.name, msg: clean(m.msg, 300) });
            break;
          }
        }
        break;
      }
      case 'help':
        broadcast({ t: 'help', from: me.name, msg: clean(m.msg, 200), server: clean(m.server || me.server, 60) }, ws);
        break;
      case 'war': {
        const enemies = Array.isArray(m.enemies) ? m.enemies.slice(0, 64).map(e => clean(e, 16)) : [];
        broadcast({ t: 'war', action: clean(m.action, 10), from: me.name, server: clean(m.server || me.server, 60), enemies }, ws);
        break;
      }
      case 'ping':
        break;
    }
  });

  ws.on('close', () => {
    clearInterval(rate);
    if (clients.delete(ws)) sendUsers();
  });
  ws.on('error', () => {});
});

refreshWhitelist();
setInterval(refreshWhitelist, REFRESH_MS);
console.log('RAGE relay listening on port', PORT, WHITELIST_URL ? '(whitelist enforced)' : '(NO whitelist: anyone can connect)');
