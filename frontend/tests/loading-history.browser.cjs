const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright")

const { spawn } = require("node:child_process")

const assert = require("node:assert/strict")

const fs = require("node:fs")

const repo = require("node:path").resolve(__dirname, "../..")

const artifacts = repo + "/.artifacts/comment-loading"

fs.mkdirSync(artifacts, { recursive: true })

const port = Number(process.env.TEST_PORT || 5193),
  origin = `http://127.0.0.1:${port}`

const api = `
const delay=ms=>new Promise(r=>setTimeout(r,ms));
export default {
 get: async(path, options={})=> {
  const f=window.fixture; f.calls.push(['get',path]);
  if(path.includes('/history')) { await delay(600); if(f.historyError) throw Error('history'); const before=options.params?.before??999; return {data:f.history.filter(r=>r.revision<before).slice(0,options.params?.size||20)}; }
  if(path.includes('/comments')) { await delay(f.commentDelay); if(f.commentError) throw Error('comments'); return {data:f.comments.filter(c=>c.postId===options.params?.['postId.equals']),headers:{'x-total-count':f.comments.length}}; }
  if(path.includes('/search')) {const query=options.params.query; await delay(query==='slow'?1200:300); if(query==='error') throw Error('search'); return {data:{content:query==='empty'?[]:[{id:query,displayName:query,username:query,mutualFriendsCount:0,friendshipStatus:'NONE'}]}};}
  if(path.includes('/user-profiles/public')) return {data:[{id:'me',displayName:'Cùng tên'},{id:'other',displayName:'Cùng tên'}]};
  if(path.includes('/user-profiles/me')) {const id=window.fixtureAuth.user.id;await delay(f.profileDelay||0);return {data:{id,displayName:id==='me'?'Người kiểm thử':'Tài khoản khác'}};}
  if(path.includes('/posts/profile/')) {await delay(200);return {data:{items:[],hasMore:false,nextCursor:null}};}
  if(path.includes('/saved')) {await delay(500); if(f.savedError)throw Error('saved'); return {data:[]};}
  if(path.includes('/mediaservice/')) {await delay(900); if(f.mediaError)throw Error('image'); return {data:new Blob(['<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="blue"/></svg>'],{type:'image/svg+xml'})};}
  if(path.includes('/friendships/')) {await delay(f.friendDelay||700); if(f.friendsError)throw Error('friends'); return {data:[]};}
  if(path.includes('/feed/')) return {data:{items:[],hasMore:false}};
  return {data:[],headers:{'x-total-count':'0'}};
 },
 put: async(path,body)=>{const f=window.fixture; f.calls.push(['put',path]); await delay(500); if(f.editError)throw Error('edit'); const c=f.comments.find(c=>path.endsWith('/'+c.id)); Object.assign(c,{content:body.content,editedAt:new Date().toISOString(),editCount:(c.editCount||0)+1}); return {data:{...c}};},
 post: async(path,body)=>{const f=window.fixture;f.calls.push(['post',path]);await delay(500);if(f.createError)throw Error('create'); const c={id:'new-'+f.comments.length,postId:body.postId||'post',parentComment:body.parentCommentId?{id:body.parentCommentId}:null,authorId:'me',content:body.content,createdAt:new Date().toISOString()};f.comments.push(c);return {data:c};},
 delete:async path=>{window.fixture.calls.push(['delete',path]);await delay(300);return {data:null};}
};`

function html(mode) {
  return `<!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head><body><div id="root"></div>
<script type="module">import RefreshRuntime from '/@react-refresh';RefreshRuntime.injectIntoGlobalHook(window);window.$RefreshReg$=()=>{};window.$RefreshSig$=()=>type=>type;window.__vite_plugin_react_preamble_installed__=true;</script>
<script type="module">
 import React from '/node_modules/.vite/deps/react.js';import ReactDOM from '/node_modules/.vite/deps/react-dom_client.js';const {createRoot}=ReactDOM;import '/src/index.css';
 window.fixture={calls:[],profileDelay:700,commentDelay:900,comments:[{id:'mine',postId:'post',authorId:'me',content:'Bình luận gốc',createdAt:'2026-10-01T00:00:00Z'},{id:'other',postId:'post',authorId:'other',content:'Bình luận người khác',createdAt:'2026-10-02T00:00:00Z'},{id:'reply',postId:'post',authorId:'me',content:'Trả lời gốc',parentComment:{id:'mine'},createdAt:'2026-10-03T00:00:00Z'}],history:Array.from({length:23},(_,i)=>({revision:22-i,content:i===22?'Bản gốc':'Nội dung lần '+(22-i),editorId:'me',editedAt:i===22?null:'2026-10-16T00:00:00Z'}))};
 const {loadRuntimeConfig}=await import('/src/config/runtime-config.ts');await loadRuntimeConfig();
 const {MemoryRouter,useNavigate}=await import('/node_modules/.vite/deps/react-router.js');
 const {AuthContext}=await import('/src/stores/authStore.ts');const {UserContext}=await import('/src/stores/userStore.ts');
 const noop=async()=>{};const auth={status:'authenticated',user:{id:'me'},login:noop,logout:noop};const user={profile:{name:'Cùng tên',avatar:'/default-avatar.svg',cover:'/default-cover.svg'},loading:false,error:null,retry:noop,updateProfile:noop};
 let el;
 if('${mode}'==='comments'){ const {default:Provider}=await import('/src/providers/CommentProvider.tsx');const {default:Section}=await import('/src/components/feed/CommentSection.tsx');el=React.createElement(Provider,null,React.createElement(Section,{postId:'post',initialVisible:true,isModal:true}));}
 if('${mode}'==='search'){const {default:Page}=await import('/src/pages/SearchPage.tsx');const Navigation=()=>{const nav=useNavigate();window.searchNavigate=q=>nav('/search?q='+q);return React.createElement(Page)};el=React.createElement(MemoryRouter,{initialEntries:['/search?q=slow']},React.createElement(Navigation));}
 if('${mode}'==='saved'){window.fixture.savedError=true;const {default:Page}=await import('/src/pages/SavedPage.tsx');el=React.createElement(Page);}
 if('${mode}'==='media'){const {default:Image}=await import('/src/components/feed/MediaImage.tsx');el=React.createElement('div',{style:{padding:20}},React.createElement(Image,{src:'/services/mediaservice/api/media/avatar/file',alt:'avatar',className:'w-8 h-8 rounded-full object-cover'}),React.createElement(Image,{src:'/slow-image.svg',alt:'cover',className:'h-48 w-full object-cover'}));}
 if('${mode}'==='profile'){window.fixture.friendDelay=1400;const {default:Provider}=await import('/src/providers/UserProvider.tsx');const {default:Page}=await import('/src/pages/ProfilePage.tsx');el=React.createElement(Provider,null,React.createElement(Page));}
 if('${mode}'==='friends'){const {default:Page}=await import('/src/pages/FriendsPage.tsx');el=React.createElement('div',{style:{paddingTop:56}},React.createElement(Page));}
 if('${mode}'==='skeletons'){const s=await import('/src/components/ui/Skeleton.tsx');el=React.createElement('div',null,...['PostSkeleton','ProfileSkeleton','FriendsSkeleton','CommentSkeleton','HistorySkeleton','UserRowsSkeleton'].map(k=>React.createElement(s[k],{key:k})));}
 if('${mode}'==='create'){const {default:Page}=await import('/src/components/feed/CreatePostModal.tsx');el=React.createElement(Page,{onClose:noop});}
 if('${mode}'!=='search')el=React.createElement(MemoryRouter,null,el);
 const root=createRoot(document.getElementById('root'));const render=()=>root.render(React.createElement(React.StrictMode,null,React.createElement(AuthContext.Provider,{value:auth},React.createElement(UserContext.Provider,{value:user},React.cloneElement(el,{key:auth.user.id})))));window.fixtureAuth=auth;window.fixtureRender=render;render();
</script></body></html>`
}
;(async () => {
  const vite = spawn(
    process.execPath,

    [
      "node_modules/vite/bin/vite.js",

      "--host",

      "127.0.0.1",

      "--port",

      String(port),

      "--strictPort",
    ],

    { cwd: repo + "/frontend", windowsHide: true, stdio: "pipe" },
  )

  let browser

  process.on("exit", () => vite.kill())

  try {
    await new Promise((resolve, reject) => {
      vite.stdout.on("data", (d) => {
        if (d.toString().includes("Local:")) resolve()
      })

      vite.on("error", reject)

      vite.on("exit", (c) => reject(Error("vite " + c)))
    })

    browser = await chromium.launch({
      channel: process.env.BROWSER_CHANNEL || "chrome",

      headless: true,
    })

    const page = await browser.newPage({
      viewport: { width: 1000, height: 800 },
    })

    const errors = []

    page.on("pageerror", (e) => {
      errors.push(e.message)

      console.log("PAGEERROR", e.message)
    })

    page.on("console", (m) => {
      if (
        m.type() === "error" &&
        !/Vite|WebSocket|websocket|vite/.test(m.text())
      )
        console.log("CONSOLE", m.text())
    })

    await page.route(origin + "/**", async (r) => {
      const res = await fetch(r.request().url())

      await r.fulfill({
        status: res.status,

        contentType: res.headers.get("content-type") || "text/plain",

        body: Buffer.from(
          (await res.text()).replace(
            /(\/node_modules\/\.vite\/deps\/[\w.-]+\.js)\?v=[\da-f]+/g,

            "$1",
          ),
        ),
      })
    })

    await page.route("**/src/services/apis.ts*", (r) =>
      r.fulfill({ contentType: "application/javascript", body: api }),
    )

    await page.route("**/src/providers/RealtimeProvider.tsx*", (r) =>
      r.fulfill({
        contentType: "application/javascript",

        body: 'const listeners=[];export const useRealtime=()=>({subscribe:fn=>{listeners.push(fn);return ()=>{}},subscribeConnection:()=>()=>{}});window.emitComment=postId=>listeners.forEach(fn=>fn({eventType:"COMMENT_CHANGED",postId}));export default ({children})=>children;',
      }),
    )

    await page.route("**/config/app-config.json", (r) =>
      r.fulfill({
        json: {
          environment: "fixture",

          apiBaseUrl: "",

          keycloakBaseUrl: origin,
        },
      }),
    )

    await page.route("**/slow-image.svg", async (r) => {
      await new Promise((x) => setTimeout(x, 1200))

      await r.fulfill({
        contentType: "image/svg+xml",

        body: '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="blue"/></svg>',
      })
    })

    await page.route("**/fixture-*", (r) =>
      r.fulfill({
        contentType: "text/html",

        body: html(r.request().url().split("fixture-")[1].split("?")[0]),
      }),
    )

    await page.goto(origin + "/fixture-comments")

    await page

      .getByRole("status", { name: "Đang tải bình luận", exact: true })

      .waitFor()

    await page.getByText("Bình luận gốc", { exact: true }).waitFor()

    assert.equal(
      await page

        .getByRole("button", { name: "(đã chỉnh sửa)", exact: true })

        .count(),

      0,
    )

    assert.equal(
      await page

        .getByRole("button", {
          name: "Tùy chọn bình luận của Cùng tên",

          exact: true,
        })

        .count(),

      2,

      "ownership uses IDs for root and reply",
    )

    const rootComment = page

      .locator("div.relative.flex.gap-2")

      .filter({ has: page.getByText("Bình luận gốc", { exact: true }) })

      .first()

    await rootComment

      .getByRole("button", {
        name: "Tùy chọn bình luận của Cùng tên",

        exact: true,
      })

      .first()

      .click({ force: true })

    await page.getByRole("button", { name: "Chỉnh sửa", exact: true }).click()

    await page.locator("textarea").fill("Nội dung sửa")

    await page.evaluate(() => (window.fixture.editError = true))

    await page.getByRole("button", { name: "Lưu", exact: true }).click()

    await page

      .getByRole("alert")

      .filter({ hasText: "Không thể hoàn tất" })

      .waitFor()

    assert.equal(await page.locator("textarea").inputValue(), "Nội dung sửa")

    await page.evaluate(() => (window.fixture.editError = false))

    await page.getByRole("button", { name: "Lưu", exact: true }).click()

    await page

      .getByRole("button", { name: "(đã chỉnh sửa)", exact: true })

      .waitFor()

    await page.evaluate(() => (window.fixture.historyError = true))

    await page

      .getByRole("button", { name: "(đã chỉnh sửa)", exact: true })

      .click()

    await page

      .getByRole("alert")

      .filter({ hasText: "Không tải được lịch sử bình luận" })

      .waitFor()

    await page.evaluate(() => (window.fixture.historyError = false))

    await page.getByRole("button", { name: "Thử lại", exact: true }).click()

    await page

      .getByRole("status", { name: "Đang tải lịch sử", exact: true })

      .waitFor()

    await page.getByText("Lần chỉnh sửa 22", { exact: false }).waitFor()

    await page.getByRole("button", { name: "Xem thêm", exact: true }).click()

    await page.getByText("Không xác định thời gian", { exact: false }).waitFor()

    assert.equal(await page.locator("dialog article").count(), 23)

    await page.screenshot({ path: artifacts + "/comment-history-desktop.png" })

    await page.setViewportSize({ width: 360, height: 740 })

    await page.screenshot({ path: artifacts + "/comment-history-mobile.png" })

    const bounds = await page.getByRole("dialog").boundingBox()

    assert(bounds.x >= 0 && bounds.x + bounds.width <= 360)

    await page.keyboard.press("Escape")

    assert.equal(await page.getByRole("dialog").count(), 0)

    await page.evaluate(() => (window.fixture.createError = true))

    await page.getByPlaceholder("Viết bình luận...").fill("Giữ lại nội dung")

    await page.getByPlaceholder("Viết bình luận...").press("Enter")

    await page

      .getByRole("alert")

      .filter({ hasText: "Không gửi được" })

      .waitFor()

    assert.equal(
      await page.getByPlaceholder("Viết bình luận...").inputValue(),

      "Giữ lại nội dung",
    )

    await page.evaluate(() => {
      window.fixture.createError = false

      window.fixture.commentDelay = 800

      window.fixture.comments[0].content = "Đã làm mới realtime"

      window.emitComment("post")
    })

    await page.waitForTimeout(450)

    assert.equal(
      await page

        .getByRole("status", { name: "Đang tải bình luận", exact: true })

        .count(),

      0,

      "refresh keeps content",
    )

    await page.getByText("Đã làm mới realtime", { exact: true }).waitFor()

    console.log(
      "PASS comments: skeleton, ID ownership, failed edit draft, edited label, history pagination, mobile fit, Escape, failed create draft, realtime refresh.",
    )

    await page.goto(origin + "/fixture-comments")

    await page.waitForFunction(() => !!window.fixture)

    await page.evaluate(() => (window.fixture.commentError = true))

    await page

      .getByRole("alert")

      .filter({ hasText: "Không tải được bình luận" })

      .waitFor()

    assert.equal(
      await page.getByText("Chưa có bình luận.", { exact: true }).count(),

      0,
    )

    await page.evaluate(() => {
      window.fixture.commentError = false

      window.fixture.comments = []
    })

    await page.getByRole("button", { name: "Thử lại", exact: true }).click()

    await page.getByText("Chưa có bình luận.", { exact: true }).waitFor()

    console.log(
      "PASS comments error/empty: retry recovers without false empty.",
    )

    await page.goto(origin + "/fixture-search")

    await page.waitForFunction(() => !!window.searchNavigate)

    await page

      .getByRole("status", { name: "Đang tìm kiếm", exact: true })

      .waitFor()

    await page.evaluate(() => window.searchNavigate("fast"))

    await page.getByText("fast", { exact: true }).waitFor()

    await page.waitForTimeout(1400)

    assert.equal(await page.getByText("slow", { exact: true }).count(), 0)

    await page.evaluate(() => window.searchNavigate("error"))

    await page.getByRole("alert").waitFor()

    await page.evaluate(() => window.searchNavigate("empty"))

    await page

      .getByText('Không tìm thấy kết quả nào cho "empty"', { exact: true })

      .waitFor()

    console.log(
      "PASS search: stale responses ignored, error and empty distinguished.",
    )

    await page.goto(origin + "/fixture-media")

    await page

      .getByRole("status", { name: "Đang tải ảnh avatar", exact: true })

      .waitFor()

    let size = await page

      .getByRole("status", { name: "Đang tải ảnh avatar", exact: true })

      .boundingBox()

    assert(size.width === 32 && size.height === 32)

    await page.getByRole("img", { name: "avatar", exact: true }).waitFor()

    await page.waitForFunction(() =>
      [...document.querySelectorAll("img")].every((i) => i.complete),
    )

    assert.equal(await page.locator('[aria-busy="true"]').count(), 0)

    console.log(
      "PASS media: avatar size during blob load, browser image load completes skeleton.",
    )

    await page.goto(origin + "/fixture-skeletons")

    await page.getByRole("status").first().waitFor()

    await page.emulateMedia({ reducedMotion: "reduce" })

    assert.equal(
      await page

        .locator(".skeleton")

        .first()

        .evaluate((e) => getComputedStyle(e).animationName),

      "none",
    )

    assert.equal(
      await page.evaluate(
        () => document.documentElement.scrollWidth > innerWidth,
      ),

      false,
    )

    await page.screenshot({ path: artifacts + "/skeletons-mobile.png" })

    console.log("PASS skeleton templates: mobile fit and reduced motion.")

    await page.goto(origin + "/fixture-saved")

    await page

      .getByRole("status", { name: "Đang tải bài viết", exact: true })

      .waitFor()

    await page

      .getByRole("alert")

      .filter({ hasText: "Không tải được bài viết đã lưu" })

      .waitFor()

    assert.equal(
      await page

        .getByText("Chưa có bài viết nào được lưu", { exact: true })

        .count(),

      0,
    )

    await page.evaluate(() => (window.fixture.savedError = false))

    await page

      .getByRole("button", { name: "Thử lại", exact: true })

      .first()

      .click()

    await page

      .getByText("Chưa có bài viết nào được lưu", { exact: true })

      .waitFor()

    console.log(
      "PASS saved: skeleton, error distinct from empty, retry to empty.",
    )

    await page.goto(origin + "/fixture-create")

    await page.getByTitle("Tag friends").click()

    await page.getByText("Chưa có bạn bè.", { exact: true }).waitFor()

    console.log("PASS tagging: real empty state ends loading.")

    await page.route("https://nominatim.openstreetmap.org/**", async (r) => {
      await new Promise((x) => setTimeout(x, 500))

      await r.fulfill({ json: [] })
    })

    await page.getByTitle("Check in").click()

    await page.getByPlaceholder("Tìm kiếm vị trí...").fill("Hanoi")

    await page

      .getByRole("status", { name: "Đang tìm địa điểm", exact: true })

      .waitFor()

    await page

      .getByText("Không tìm thấy địa điểm phù hợp.", { exact: true })

      .waitFor()

    console.log("PASS location: skeleton and real empty state.")

    await page.goto(origin + "/fixture-profile")

    await page
      .getByRole("status", { name: "Đang tải hồ sơ", exact: true })
      .waitFor()

    await page
      .getByRole("heading", { name: "Người kiểm thử", exact: true })
      .waitFor()

    assert(
      (await page
        .getByRole("status", { name: "Đang tải danh sách", exact: true })
        .count()) > 0,
      "profile header loads independently from friends",
    )

    await page.goto(origin + "/fixture-profile")

    await page.waitForFunction(() => !!window.fixtureRender)

    await page.evaluate(() => {
      window.fixture.profileDelay = 200
      window.fixtureAuth.user = { id: "other" }
      window.fixtureRender()
    })

    await page
      .getByRole("heading", { name: "Tài khoản khác", exact: true })
      .waitFor()

    await page.waitForTimeout(1000)

    assert.equal(
      await page
        .getByRole("heading", { name: "Người kiểm thử", exact: true })
        .count(),
      0,
    )

    console.log(
      "PASS profile: header/friends load independently; account switch drops stale profile response.",
    )

    await page.goto(origin + "/fixture-friends")

    await page

      .getByRole("status", { name: "Đang tải bạn bè", exact: true })

      .first()

      .waitFor()

    await page.waitForFunction(() =>
      window.fixture.calls.some((c) => c[1].includes("/friendships/")),
    )

    await page.evaluate(() => (window.fixture.friendsError = true))

    await page.getByRole("alert").waitFor()

    assert.equal(
      await page

        .getByText("Không có lời mời kết bạn mới", { exact: true })

        .count(),

      0,
    )

    await page.evaluate(() => (window.fixture.friendsError = false))

    await page.getByRole("button", { name: "Thử lại", exact: true }).click()

    await page.waitForFunction(() =>
      document.body.innerText.includes("Không có lời mời kết bạn mới"),
    )

    console.log("PASS friends: loading, error without false empty, retry.")

    assert.deepEqual(errors, [])

    console.log("PASS no browser errors. Fixture checks, not live backend E2E.")
  } finally {
    await browser?.close()

    vite.kill()
  }
})().catch((e) => {
  console.error(e)

  process.exitCode = 1
})
