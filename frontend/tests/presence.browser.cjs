const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const { spawn } = require("node:child_process");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../..");
const artifacts = path.join(root, ".artifacts/presence-browser");
fs.mkdirSync(artifacts, { recursive: true });
const port = 5186;
const me = "11111111-1111-4111-8111-111111111111";
const otherAccount = "22222222-2222-4222-8222-222222222222";
const friend = "33333333-3333-4333-8333-333333333333";
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const html = `<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="root"></div>
<script type="module">import RefreshRuntime from '/@react-refresh';RefreshRuntime.injectIntoGlobalHook(window);window.$RefreshReg$=()=>{};window.$RefreshSig$=()=>type=>type;window.__vite_plugin_react_preamble_installed__=true;</script>
<script type="module">
import React from '/node_modules/.vite/deps/react.js';
import ReactDOM from '/node_modules/.vite/deps/react-dom_client.js';const {createRoot}=ReactDOM;
import '/src/index.css';
const {loadRuntimeConfig}=await import('/src/config/runtime-config.ts');await loadRuntimeConfig();
const {MemoryRouter}=await import('/node_modules/.vite/deps/react-router.js');
const {AuthContext}=await import('/src/stores/authStore.ts');
const {default:PresenceProvider,usePresence}=await import('/src/providers/PresenceProvider.tsx');
const {default:RightSidebar}=await import('/src/components/sidebar/RightSidebar.tsx');
const {default:FriendCard}=await import('/src/components/friends/FriendCard.tsx');
const {default:ProfileLayout}=await import('/src/components/profile/ProfileLayout.tsx');
const {default:keycloak}=await import('/src/services/keycloak.ts');
function Harness(){const [id,setId]=React.useState('${me}');const [show,setShow]=React.useState(true);
 window.changeAccount=id=>{keycloak.token=id;setId(id)};window.setVisible=setShow;
 const auth={status:id?'authenticated':'unauthenticated',user:id?{id}:null};
 return React.createElement(AuthContext.Provider,{value:auth},React.createElement(PresenceProvider,{key:id},React.createElement(MemoryRouter,null,
   show&&React.createElement(React.Fragment,null,
    React.createElement(RightSidebar),
    React.createElement('main',{style:{maxWidth:760,paddingTop:60}},
     React.createElement('div',{'data-testid':'friend-card',style:{width:240}},React.createElement(FriendCard,{user:{id:'${friend}',displayName:'Bạn kiểm thử'},cardType:'friend'})),
     React.createElement('div',{'data-testid':'profile'},React.createElement(ProfileLayout,{user:{name:'Bạn kiểm thử',avatar:'/default-avatar.svg'},isOwn:false,presenceUserId:'${friend}',posts:[],actionButtons:null})))))));
}
createRoot(document.getElementById('root')).render(React.createElement(React.StrictMode,null,React.createElement(Harness)));
</script></body></html>`;

(async () => {
  const vite = spawn(process.execPath, ["node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", String(port), "--strictPort"],
    { cwd: path.join(root, "frontend"), windowsHide: true, stdio: "pipe" });
  let browser;
  try {
    await new Promise((resolve, reject) => {
      vite.stdout.on("data", data => { if (data.toString().includes("Local:")) resolve(); });
      vite.on("error", reject); vite.on("exit", code => reject(Error("Vite exited " + code)));
    });
    browser = await chromium.launch({ channel: process.env.BROWSER_CHANNEL || "chrome", headless: true });
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, reducedMotion: "reduce" });
    await page.clock.install();
    const errors = []; page.on("pageerror", error => { errors.push(error.message); console.error("Browser error:", error.message); });
    const sockets = []; let tickets = 0;
    await page.route("https://fonts.googleapis.com/**", route => route.fulfill({ contentType: "text/css", body: "" }));
    await page.route("https://fonts.gstatic.com/**", route => route.abort());
    // Inline fixtures must share the same optimized React module URL as source imports.
    await page.route(`http://127.0.0.1:${port}/**`, async route => {
      const response = await fetch(route.request().url());
      const type = response.headers.get("content-type") || "application/octet-stream";
      const body = Buffer.from(await response.arrayBuffer());
      await route.fulfill({ status: response.status, contentType: type, body: /javascript|text\/html/.test(type)
        ? body.toString().replace(/(\/node_modules\/\.vite\/deps\/[\w.-]+\.js)\?v=[\da-f]+/g, "$1") : body });
    });
    await page.route("**/presence-fixture", route => route.fulfill({ contentType: "text/html", body: html }));
    await page.route("**/src/services/keycloak.ts", route => route.fulfill({ contentType: "application/javascript",
      body: `export default {authenticated:true,token:'${me}',updateToken:async()=>true};` }));
    await page.route("**/api/presence/tickets", async route => {
      tickets++;
      const account = route.request().headers().authorization?.replace("Bearer ", "");
      await route.fulfill({ json: { ticket: account, expiresAt: new Date(Date.now() + 30000).toISOString(), authExpiresAt: new Date(Date.now() + 300000).toISOString() } });
    });
    await page.route("**/services/userservice/api/friendships/me?*", route => route.fulfill({ json: [{ user: { id: me }, friend: { id: friend, displayName: "Bạn kiểm thử" } }] }));
    await page.routeWebSocket("**/api/presence/ws", ws => {
      const socket = { ws, ids: new Set(), sequence: 0, epoch: String(sockets.length), account: null, closed: false };
      sockets.push(socket);
      socket.snapshot = (status = "online", overrides = {}) => ws.send(JSON.stringify({ type: "PRESENCE_SNAPSHOT", epoch: socket.epoch,
        sequence: ++socket.sequence, entries: [...socket.ids].map(userId => ({ userId, status, lastSeenAt: new Date(Date.now() - 120000).toISOString() })), removed: [], ...overrides }));
      ws.onMessage(raw => {
        const message = JSON.parse(raw);
        if (message.type === "AUTH") { socket.account = message.ticket; ws.send(JSON.stringify({ type: "READY", epoch: socket.epoch, authExpiresAt: new Date(Date.now() + 300000).toISOString() })); }
        if (message.type === "REAUTH") ws.send(JSON.stringify({ type: "REAUTHENTICATED", epoch: socket.epoch, authExpiresAt: new Date(Date.now() + 300000).toISOString() }));
        if (message.type === "UNSUBSCRIBE") message.userIds.forEach(id => socket.ids.delete(id));
        if (message.type === "SUBSCRIBE") { message.userIds.forEach(id => socket.ids.add(id)); socket.snapshot(socket.account === me ? "online" : "offline"); }
      });
      ws.onClose(() => { socket.closed = true; });
    });
    await page.goto(`http://127.0.0.1:${port}/presence-fixture`, { waitUntil: "domcontentloaded", timeout: 60000 });
    const card = page.getByTestId("friend-card");
    await card.getByText("Đang hoạt động", { exact: true }).waitFor().catch(async error => {
      console.error({ errors, sockets: sockets.length, tickets, body: await page.locator("body").innerText() });
      await page.screenshot({path:path.join(artifacts,"failure.png"),fullPage:true});
      throw error;
    });
    assert.equal(sockets.length, 1, "One socket per tab despite StrictMode and multiple consumers");
    assert(!sockets[0].ws.url().includes("?"), "No token in URL");
    await page.screenshot({ path: path.join(artifacts, "desktop.png"), fullPage: true });
    const first = sockets[0]; first.snapshot("offline");
    await card.getByText("Hoạt động 2 phút trước", { exact: true }).waitFor();
    assert.equal(await page.getByTestId("profile").getByText("Hoạt động 2 phút trước", { exact: true }).count(), 1);
    first.snapshot("online", { sequence: first.sequence - 1 });
    await sleep(100);
    assert.equal(await card.getByText("Đang hoạt động", { exact: true }).count(), 0, "Old sequence ignored");
    first.ws.send(JSON.stringify({ type: "PRESENCE_REMOVED", userIds: [friend] }));
    await card.getByText("Chưa xác định trạng thái", { exact: true }).waitFor();
    first.snapshot(); await card.getByText("Đang hoạt động", { exact: true }).waitFor();
    first.ws.send(JSON.stringify({type:"ERROR",code:"PRESENCE_UNAVAILABLE"}));
    await card.getByText("Chưa xác định trạng thái", { exact: true }).waitFor();
    first.snapshot(); await card.getByText("Đang hoạt động", { exact: true }).waitFor();
    first.ws.send(JSON.stringify({ type: "REAUTH_REQUIRED" }));
    await page.waitForTimeout(100); assert(tickets >= 2, "Fresh ticket used for renewal");
    first.ws.close({ code: 1012, reason: "Controlled reconnect" });
    await card.getByText("Chưa xác định trạng thái", { exact: true }).waitFor();
    await card.getByText("Đang hoạt động", { exact: true }).waitFor();
    assert.equal(sockets.length, 2, "Reconnect creates a fresh authenticated socket");
    const old = sockets[1];
    await page.evaluate(account => window.changeAccount(account), otherAccount);
    await card.getByText("Hoạt động 2 phút trước", { exact: true }).waitFor();
    assert(old.closed, "Old account socket closed");
    await page.evaluate(() => window.setVisible(false));
    await page.waitForFunction(() => !document.querySelector('[data-testid="friend-card"]'));
    await sleep(100); assert.equal(sockets.at(-1).ids.size, 0, "Unmount releases shared subscriptions");
    await page.evaluate(() => window.setVisible(true));
    await card.getByText("Hoạt động 2 phút trước", { exact: true }).waitFor();
    await page.setViewportSize({ width: 390, height: 844 });
    await page.screenshot({ path: path.join(artifacts, "mobile.png"), fullPage: true });
    assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, "No horizontal overflow");
    const beforeTimeout = sockets.length;
    await page.clock.fastForward(76000);
    await card.getByText("Chưa xác định trạng thái", { exact: true }).waitFor();
    await page.clock.runFor(1800);
    await page.waitForFunction(() => document.querySelector('[data-testid="friend-card"]').textContent.includes("Hoạt động"));
    assert.equal(sockets.length, beforeTimeout + 1, "Half-open socket watchdog reconnects");
    await page.clock.resume();
    await page.evaluate(() => window.changeAccount(null));
    await card.getByText("Chưa xác định trạng thái", { exact: true }).waitFor();
    const count = sockets.length; await sleep(1700); assert.equal(sockets.length, count, "No reconnect after logout");
    assert.deepEqual(errors, []);
    fs.writeFileSync(path.join(artifacts, "result.json"), JSON.stringify({ pass: true, sockets: count, checks: ["shared socket", "snapshot", "last seen", "sequence", "permission removal", "error", "reauth", "reconnect", "account switch", "unsubscribe", "mobile", "watchdog", "logout"] }, null, 2));
    console.log("Presence browser checks PASS");
  } finally { await browser?.close(); vite.kill(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
