const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const { spawn } = require("node:child_process");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const root = path.resolve(__dirname, "../..");
const origin = "http://127.0.0.1:5198";
const out = path.join(root, ".artifacts/admin-browser");
fs.mkdirSync(out, { recursive: true });
const api = `export default {
 get:async(url,options={})=>{window.calls.push(['get',url,options.params instanceof URLSearchParams?options.params.toString():options.params]);if(window.apiDelay)await new Promise(resolve=>setTimeout(resolve,window.apiDelay));if(window.fail)throw Error('unavailable');
 if(url.endsWith('/overview')){const days=options.params?.days||7;return {data:{users:window.chartZero?0:9,admins:window.chartZero?0:1,suspended:window.chartZero?0:2,pending:window.chartZero?0:2,directory_ready:1,user_states:{active:window.chartZero?0:7,suspended:window.chartZero?0:1,processing:window.chartZero?0:1},sync_states:{synced:window.chartZero?0:8,pending:window.chartZero?0:1,retry:window.chartZero?0:1},days,timezone:'Asia/Ho_Chi_Minh',activity:Array.from({length:days},(_,i)=>({date:new Date(Date.UTC(2026,9,18-days+i)).toISOString().slice(0,10),views:window.chartZero?0:i%3===0?i+1:0,changes:window.chartZero?0:i%4}))}};}
 if(url.includes('/api/admin/media/'))return {data:new Blob(['<svg xmlns="http://www.w3.org/2000/svg" width="160" height="90"><rect width="160" height="90" fill="blue"/></svg>'],{type:'image/svg+xml'})};
 if(new RegExp('[a-f0-9-]{36}$').test(url))return {data:url.includes('/api/admin/users/')?{subject:'11111111-1111-1111-1111-111111111111',username:'nguoithunghiem',display_name:'Người thử nghiệm',email:'nguoithunghiem@example.test',last_seen_at:'2026-10-17T03:12:00Z',version:4,status:'ACTIVE',account_kind:'USER',sync_state:'SYNCED'}:{id:'11111111-1111-1111-1111-111111111111',content:'Nội dung riêng tư đã được ghi nhật ký',created_at:'2026-10-17T18:30:00Z',version:4,status:'ACTIVE',media_ids:['33333333-3333-3333-3333-333333333333']}};
 const area=url.split('/').pop();return {data:{items:window.empty?[]:area==='users'||area==='admins'?[{subject:'11111111-1111-1111-1111-111111111111',username:'nguoithunghiem',display_name:'Người thử nghiệm',email:'nguoithunghiem@example.test',last_seen_at:null,status:'ACTIVE',sync_state:'SYNCED',version:4}]:area==='audit'?[{event_id:'11111111-1111-1111-1111-111111111111',occurred_at:'2026-10-17T18:30:00Z',actor:'admin',target:'post',action:'VIEW_POST',reason:'Kiểm duyệt'}]:[{id:'11111111-1111-1111-1111-111111111111',author_id:'22222222-2222-2222-2222-222222222222',created_at:'2026-10-17T18:30:00Z',status:'ACTIVE',moderation_visibility:'VISIBLE',community_archived:false,version:4}],total:1,page:0,size:20}};},
 post:async(url,body,options)=>{window.calls.push(['post',url,body,options]);return {data:{version:5}};}
}`;
function html(area, admin = true, delay = 0, authStatus = 'authenticated', zero = false) {
  return `<!doctype html><html lang="vi"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{margin:0}</style></head><body><div id="root"></div>
<script type="module">import RefreshRuntime from '/@react-refresh';RefreshRuntime.injectIntoGlobalHook(window);window.$RefreshReg$=()=>{};window.$RefreshSig$=()=>type=>type;window.__vite_plugin_react_preamble_installed__=true;</script><script type="module">
import React from '/node_modules/.vite/deps/react.js';import ReactDOM from '/node_modules/.vite/deps/react-dom_client.js';const {createRoot}=ReactDOM;
import {createMemoryRouter,RouterProvider} from '/node_modules/.vite/deps/react-router.js';
import '/src/index.css';
const {loadRuntimeConfig}=await import('/src/config/runtime-config.ts');await loadRuntimeConfig();
const {AuthContext}=await import('/src/stores/authStore.ts');const {default:RequireAdmin}=await import('/src/components/RequireAdmin.tsx');
const {default:RequireAuth}=await import('/src/components/RequireAuth.tsx');
const {default:AdminLayout}=await import('/src/layouts/AdminLayout.tsx');const {default:AdminPage}=await import('/src/pages/admin/AdminPage.tsx');
window.calls=[];window.fail=false;window.empty=false;window.apiDelay=${delay};window.chartZero=${zero};
const auth={status:'${authStatus}',user:{id:'admin',username:'quantrivien'},isAdmin:${admin},roles:['ROLE_ADMIN'],accountKind:'ADMIN',logout:async()=>{},login:async()=>{}};
const router=createMemoryRouter([{path:'/admin',element:React.createElement(RequireAuth,null,React.createElement(RequireAdmin,null,React.createElement(AdminLayout))),children:[{index:true,element:React.createElement(AdminPage,{area:'${area}'})},{path:'${area}',element:React.createElement(AdminPage,{area:'${area}'})}]}],{initialEntries:['/admin${area==='overview'?'':'/'+area}']});
createRoot(document.getElementById('root')).render(React.createElement(AuthContext.Provider,{value:auth},React.createElement(RouterProvider,{router})));
</script></body></html>`;
}
(async () => {
 const vite = spawn(process.execPath, ["node_modules/vite/bin/vite.js", "--host", "127.0.0.1", "--port", "5198", "--strictPort"], {cwd:path.join(root,"frontend"), windowsHide:true, stdio:"pipe"});
 let browser;
 try {
  await new Promise((resolve,reject)=>{vite.stdout.on('data',data=>{if(data.toString().includes('Local:'))resolve()});vite.on('error',reject);vite.on('exit',code=>reject(Error('vite '+code)));});
  browser=await chromium.launch({channel:process.env.BROWSER_CHANNEL||'chrome',headless:true,args:["--disable-gpu"]});
  const page=await browser.newPage({viewport:{width:1440,height:1000},timezoneId:process.env.ADMIN_TEST_TIMEZONE||'UTC'});const errors=[];page.on('pageerror',e=>{errors.push(e.message);console.error('PAGEERROR',e.message)});
  page.on('console',message=>{if(message.type()==='error')console.error('CONSOLE',message.text())});
  await page.route(origin+'/**',async route=>{const response=await fetch(route.request().url());await route.fulfill({status:response.status,contentType:response.headers.get('content-type')||'text/plain',body:(await response.text()).replace(/(\/node_modules\/\.vite\/deps\/[\w.-]+\.js)\?v=[\da-f]+/g,'$1')});});
  await page.route('**/@vite/client',route=>route.fulfill({contentType:'application/javascript',body:'export const createHotContext=()=>({accept(){},dispose(){},prune(){},on(){},send(){}});const styles=new Map();export function updateStyle(id,content){let style=styles.get(id);if(!style){style=document.createElement("style");document.head.appendChild(style);styles.set(id,style)}style.textContent=content}export const removeStyle=id=>{styles.get(id)?.remove();styles.delete(id)};export const injectQuery=(url)=>url;'}));
  await page.route('**/src/services/apis.ts*',route=>route.fulfill({contentType:'application/javascript',body:api}));
  let area='overview',admin=true,delay=0,authStatus='authenticated',zero=false;
  await page.route(origin+'/admin-fixture',route=>route.fulfill({contentType:'text/html',body:html(area,admin,delay,authStatus,zero)}));
  await page.goto(origin+'/admin-fixture');await page.getByRole('heading',{name:'Tổng quan',exact:true}).waitFor();
  await page.getByRole('heading',{name:'Trạng thái người dùng',exact:true}).waitFor();
  await page.getByText('Thời gian theo giờ Việt Nam (GMT+7)',{exact:true}).waitFor();
  await page.screenshot({animations:"disabled",path:path.join(out,'overview-desktop.png'),fullPage:true});
  await page.getByRole('button',{name:'11/10/2026: Xem chi tiết/tệp 1, Thay đổi quản trị 0',exact:true}).focus();await page.getByText('11/10/2026 · Xem chi tiết/tệp: 1 · Thay đổi quản trị: 0',{exact:true}).waitFor();
  await page.getByText('Xem bảng số liệu hoạt động',{exact:true}).click();assert.equal(await page.getByRole('table').locator('tbody tr').count(),7);
  await page.getByRole('button',{name:'30 ngày',exact:true}).click();await page.getByText('30 ngày gần nhất · GMT+7',{exact:true}).waitFor();
  if(!await page.locator('.admin-chart-data').evaluate(element=>element.open))await page.getByText('Xem bảng số liệu hoạt động',{exact:true}).click();assert.equal(await page.getByRole('table').locator('tbody tr').count(),30);
  assert.equal(await page.evaluate(()=>window.calls.filter(c=>c[1].endsWith('/overview')).at(-1)[2].days),30);
  await page.getByText('Xem bảng số liệu hoạt động',{exact:true}).click();await page.setViewportSize({width:390,height:844});await page.screenshot({animations:'disabled',path:path.join(out,'overview-mobile.png'),fullPage:true});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);await page.setViewportSize({width:1440,height:1000});
  zero=true;await page.goto(origin+'/admin-fixture');await page.getByText('Chưa có tài khoản người dùng.',{exact:true}).waitFor();await page.getByText('Chưa có tài khoản để đồng bộ.',{exact:true}).waitFor();await page.getByText('Chưa có hoạt động quản trị trong khoảng thời gian này.',{exact:true}).waitFor();
  await page.evaluate(()=>window.fail=true);await page.getByRole('button',{name:'Tải lại',exact:true}).click();await page.getByRole('alert').waitFor();await page.evaluate(()=>window.fail=false);await page.getByRole('button',{name:'Thử lại',exact:true}).click();await page.getByText('Chưa có tài khoản người dùng.',{exact:true}).waitFor();zero=false;
  for(const next of ['users','admins','posts','comments','audit']) {
   area=next;await page.goto(origin+'/admin-fixture');await page.getByText('1 kết quả',{exact:true}).waitFor();
   if(next==='users') {
    await page.getByText('Người thử nghiệm',{exact:true}).waitFor();await page.getByText('nguoithunghiem@example.test',{exact:true}).waitFor();await page.getByText('Chưa ghi nhận',{exact:true}).waitFor();
    await page.getByRole('button',{name:'Chi tiết',exact:true}).click();
    const details=page.getByRole('dialog',{name:'Chi tiết người dùng'});await details.waitFor();await details.getByText('nguoithunghiem@example.test',{exact:true}).waitFor();
    await details.getByText('17/10/2026 10:12:00',{exact:true}).waitFor();await details.getByText('Online lần cuối (GMT+7)',{exact:true}).waitFor();
    assert.equal(await page.locator('section.admin-detail').count(),0);
    await page.screenshot({animations:"disabled",path:path.join(out,'users-detail-desktop.png'),fullPage:true});
    await page.keyboard.press('Escape');await details.waitFor({state:'hidden'});
    assert.equal(await page.getByRole('button',{name:'Chi tiết',exact:true}).evaluate(el=>el===document.activeElement),true);
    await page.getByRole('button',{name:'Khóa',exact:true}).click();await page.getByLabel('Lý do',{exact:true}).fill('Kiểm thử khóa có audit');
    await page.getByRole('button',{name:'Xác nhận khóa',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});
    const mutation=await page.evaluate(()=>window.calls.find(c=>c[0]==='post'));
    assert.equal(mutation[2].version,4);assert.equal(mutation[2].reason,'Kiểm thử khóa có audit');assert.ok(mutation[3].headers['Idempotency-Key']);
   }
   if(next==='posts'||next==='comments') {
    await page.getByText('18/10/2026 01:30:00',{exact:true}).waitFor();
    assert.equal(await page.getByText('Nội dung riêng tư đã được ghi nhật ký',{exact:true}).count(),0);
    await page.getByRole('button',{name:'Chi tiết',exact:true}).click();await page.getByText('Nội dung riêng tư đã được ghi nhật ký',{exact:true}).waitFor();
    await page.getByRole('dialog').getByText('18/10/2026 01:30:00',{exact:true}).waitFor();
    assert.equal(await page.getByRole('dialog').count(),1);assert.equal(await page.locator('section.admin-detail').count(),0);
    await page.getByRole('button',{name:'Xem tệp đính kèm 1',exact:true}).click();await page.getByRole('img',{name:'Tệp đính kèm 1',exact:true}).waitFor();
    await page.screenshot({animations:"disabled",path:path.join(out,next+'-desktop.png'),fullPage:true});
    await page.setViewportSize({width:390,height:844});await page.screenshot({animations:"disabled",path:path.join(out,next+'-mobile.png'),fullPage:true});
    assert.equal(await page.getByRole('dialog').evaluate(el=>el.scrollWidth<=el.clientWidth),true);
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),true);await page.setViewportSize({width:1440,height:1000});
   }
   if(next==='audit') {
    await page.getByText('18/10/2026 01:30:00',{exact:true}).waitFor();
    await page.getByLabel('Từ thời điểm (GMT+7)',{exact:true}).fill('2026-10-17T10:12');await page.getByLabel('Đến thời điểm (GMT+7)',{exact:true}).fill('2026-10-17T19:12');await page.getByRole('button',{name:'Áp dụng',exact:true}).click();
    await page.waitForFunction(()=>window.calls.some(c=>typeof c[2]==='string'&&new URLSearchParams(c[2]).get('from')==='2026-10-17T03:12:00.000Z'&&new URLSearchParams(c[2]).get('to')==='2026-10-17T12:12:00.000Z'));
    assert.equal(await page.getByLabel('Từ thời điểm (GMT+7)',{exact:true}).inputValue(),'2026-10-17T10:12');
   }
  }
  area='users';delay=900;await page.goto(origin+'/admin-fixture');await page.getByRole('status',{name:'Đang tải dữ liệu quản trị'}).waitFor();await page.screenshot({animations:"disabled",path:path.join(out,'users-skeleton.png'),fullPage:true});await page.getByText('1 kết quả',{exact:true}).waitFor();
  await page.getByRole('button',{name:'Chi tiết',exact:true}).click();await page.getByRole('dialog').getByRole('status',{name:'Đang tải chi tiết'}).waitFor();await page.screenshot({animations:"disabled",path:path.join(out,'detail-skeleton.png'),fullPage:true});
  await page.getByRole('dialog').getByText('nguoithunghiem@example.test',{exact:true}).waitFor();await page.getByRole('button',{name:'Đóng chi tiết'}).click();await page.getByRole('dialog').waitFor({state:'hidden'});
  await page.getByRole('button',{name:'Chi tiết',exact:true}).click();await page.getByRole('dialog').getByRole('status',{name:'Đang tải chi tiết'}).waitFor();await page.keyboard.press('Escape');await new Promise(resolve=>setTimeout(resolve,1000));assert.equal(await page.getByRole('dialog').count(),0);
  await page.evaluate(()=>{window.fail=true;window.apiDelay=0});await page.getByRole('button',{name:'Chi tiết',exact:true}).click();await page.getByRole('dialog').getByRole('alert').waitFor();await page.evaluate(()=>{window.fail=false});await page.getByRole('dialog').getByRole('button',{name:'Thử lại'}).click();await page.getByRole('dialog').getByText('nguoithunghiem@example.test',{exact:true}).waitFor();await page.keyboard.press('Escape');
  delay=0;
  authStatus='initializing';await page.goto(origin+'/admin-fixture');await page.getByRole('status',{name:'Đang tải dữ liệu quản trị'}).waitFor();assert.equal(await page.evaluate(()=>window.calls.length),0);await page.screenshot({animations:'disabled',path:path.join(out,'admin-shell-skeleton.png'),fullPage:true});authStatus='authenticated';
  area='users';admin=false;await page.goto(origin+'/admin-fixture');await page.getByRole('heading',{name:'Bạn không có quyền quản trị'}).waitFor();
  assert.equal(await page.evaluate(()=>window.calls.length),0);
  assert.deepEqual(errors,[]);console.log('PASS: charts 7/30 days, keyboard tooltip, zero/error states, GMT+7 dates/filter roundtrip in '+(process.env.ADMIN_TEST_TIMEZONE||'UTC')+', admin modals/skeletons/authorization and mobile layout');
 } finally {await browser?.close();vite.kill();}
})().catch(error=>{console.error(error);process.exitCode=1;});
