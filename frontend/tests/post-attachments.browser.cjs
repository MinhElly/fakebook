const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const assert = require("node:assert/strict");
const path = require("node:path");
const fs = require("node:fs");

const mediaStub = `
export const getMediaUrl = id => '/default-avatar.svg';
export async function uploadPostMedia(file, options={}) {
  const f=window.fixture;(f.uploadOptions??=[]).push({publicationId:options.publicationId,fileKey:options.fileKey});f.calls[file.name]=(f.calls[file.name]||0)+1;f.active++;f.max=Math.max(f.max,f.active);
  try {
    options.onProgress?.(40,'uploading');
    await new Promise((resolve,reject)=>{const timer=setTimeout(resolve,700);options.signal?.addEventListener('abort',()=>{clearTimeout(timer);reject(new DOMException('Aborted','AbortError'));},{once:true});});
    if(file.name==='fail.png' && f.calls[file.name]===1) throw new Error('Fixture network error');
    options.onProgress?.(96,'processing');
    return {id:file.name,mediaType:file.type.startsWith('video/')?'VIDEO':'IMAGE'};
  } finally {f.active--;}
}`;

const html = `<!doctype html><html lang="vi"><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><body><div id="root"></div>
<script type="module">import RefreshRuntime from '/@react-refresh';RefreshRuntime.injectIntoGlobalHook(window);window.$RefreshReg$=()=>{};window.$RefreshSig$=()=>type=>type;window.__vite_plugin_react_preamble_installed__=true;</script>
<script type="module">
import React from '/node_modules/.vite/deps/react.js';import ReactDOM from '/node_modules/.vite/deps/react-dom_client.js';import '/src/index.css';
window.fixture={calls:{},active:0,max:0};const {loadRuntimeConfig}=await import('/src/config/runtime-config.ts');await loadRuntimeConfig();
const {default:Attachments,usePostAttachments}=await import('/src/components/feed/PostAttachments.tsx');
const {default:Composer}=await import('/src/components/feed/CreatePostModal.tsx');
const {PostContext}=await import('/src/stores/postStore.ts');
function App(){const initial=location.search.includes('edit')?{mediaIds:['old-image','old-video'],media:[{id:'old-image',mediaType:'IMAGE'},{id:'old-video',mediaType:'VIDEO'}]}:undefined;
 const manager=usePostAttachments(initial);window.fixture.snapshot=()=>manager.items.map(i=>({name:i.name,id:i.mediaId,status:i.status}));
 if(location.search.includes('composer'))return React.createElement(PostContext.Provider,{value:{addPost:async(content,ids)=>{if(location.search.includes('failure')&&!window.fixture.failed){window.fixture.failed=true;throw new Error('Fixture save failed');}window.fixture.saved=ids;},updatePost:async()=>{}}},React.createElement(Composer,{onClose:()=>window.fixture.close()}));
 return React.createElement('main',{style:{maxWidth:520,margin:'20px auto',background:'white'}},React.createElement('h1',{style:{padding:16,fontWeight:700}},'Ảnh và video bài viết'),React.createElement(Attachments,{manager,disabled:false}),React.createElement('button',{disabled:!manager.ready||!manager.items.length,onClick:()=>manager.uploadAll().catch(()=>{})},'Đăng'));}
const root=ReactDOM.createRoot(document.getElementById('root'));window.fixture.close=()=>root.unmount();root.render(React.createElement(React.StrictMode,null,React.createElement(App)));
</script></body></html>`;

(async () => {
  const { createServer } = await import("vite");
  const root = path.resolve(__dirname, "..");
  const server = await createServer({ root, server: { port: 0, strictPort: false, host: "127.0.0.1" }, plugins: [{
    name: "attachments-fixture",
    configureServer(vite) { vite.middlewares.use((req, res, next) => {
      if (req.url?.startsWith("/fixture")) { res.setHeader("Content-Type", "text/html; charset=utf-8"); res.end(html); return; }
      if (req.url?.split("?")[0] === "/src/services/postPublication.ts") { res.setHeader("Content-Type", "application/javascript"); res.end(`
        const states={};export async function beginPublication(id,draft){window.fixture.begins=(window.fixture.begins||0)+1;return states[id]??=( {id,status:'PENDING'} );}
        export async function publicationStatus(id){return states[id];}
        export async function cancelPublication(id){window.fixture.cancelled=id;return states[id]={id,status:'CANCELLED'};}
      `); return; }
      if (req.url?.split("?")[0] === "/src/services/mediaService.ts") { res.setHeader("Content-Type", "application/javascript"); res.end(mediaStub); return; }
      next();
    }); },
  }] });
  let browser;
  try {
    await server.listen();
    browser = await chromium.launch({ headless: true, channel: "chrome" });
    const page = await browser.newPage();
    const errors = []; page.on("pageerror", error => errors.push(error.message));
    const base = `http://127.0.0.1:${server.httpServer.address().port}/fixture`;
    const png = Buffer.from(await page.evaluate(()=>{const canvas=document.createElement('canvas');canvas.width=300;canvas.height=200;const ctx=canvas.getContext('2d');ctx.fillStyle='#1877F2';ctx.fillRect(0,0,300,200);ctx.fillStyle='white';ctx.font='22px sans-serif';ctx.fillText('Ảnh kiểm tra',24,100);return canvas.toDataURL().split(',')[1];}),'base64');
    const file = name => ({ name, mimeType: "image/png", buffer: png });
    if (!process.env.COMPOSER_ONLY) {
    await page.goto(base); await page.locator('input[type=file]').waitFor({state:"attached"});
    await page.locator('input[type=file]').setInputFiles(Array.from({length:31},(_,i)=>file(`image-${i}.png`)));
    await page.getByRole("alert").waitFor(); assert.equal((await page.evaluate(()=>window.fixture.snapshot())).length,0);
    await page.locator('input[type=file]').setInputFiles(Array.from({length:30},(_,i)=>file(`image-${i}.png`)));
    assert.deepEqual(await page.evaluate(()=>window.fixture.calls),{});
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.waitForFunction(()=>window.fixture.snapshot().length===30 && window.fixture.snapshot().every(i=>i.status==='ready'),{},{timeout:60000})
      .catch(async error=>{console.log(JSON.stringify(await page.evaluate(()=>({items:window.fixture.snapshot(),calls:window.fixture.calls,active:window.fixture.active,max:window.fixture.max}))));throw error;});
    assert.equal(await page.evaluate(()=>window.fixture.max),3);
    await page.getByRole('button',{name:'Chỉnh sửa',exact:false}).click();
    await page.getByRole('button',{name:'Di chuyển tệp 1 ra sau',exact:true}).click();
    assert.equal((await page.evaluate(()=>window.fixture.snapshot()))[0].id,'image-1.png');
    assert.equal((await page.evaluate(()=>window.fixture.calls))['image-0.png'],1);
    console.log('PASS 30 files, reject 31 atomically, concurrency 3, reorder without re-upload');
    await page.goto(base); await page.locator('input[type=file]').setInputFiles([file('fail.png'),file('ok.png')]);
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.getByRole('button',{name:'Thử lại',exact:true}).waitFor();
    assert.equal(await page.getByRole('button',{name:'Đăng',exact:true}).isDisabled(),false);
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.waitForFunction(()=>window.fixture.snapshot().every(i=>i.status==='ready'));
    assert.equal((await page.evaluate(()=>window.fixture.calls))['ok.png'],1);
    console.log('PASS partial failure, retry failed item only, submit gating');
    await page.goto(base); await page.locator('input[type=file]').setInputFiles(file('cancel.png'));
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.getByRole('button',{name:'Hủy tải',exact:true}).click();
    await page.getByText('Đã hủy',{exact:true}).waitFor();
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.waitForFunction(()=>window.fixture.snapshot().every(i=>i.status==='ready'));
    console.log('PASS cancellation and retry');
    await page.goto(base+'?edit'); await page.waitForFunction(()=>document.querySelectorAll('section ol li').length===2);
    await page.getByRole('button',{name:'Xóa tệp 2',exact:true}).click();
    await page.locator('input[type=file]').setInputFiles(file('new.png'));
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.waitForFunction(()=>window.fixture.snapshot().length===2 && window.fixture.snapshot().every(i=>i.status==='ready'));
    assert.deepEqual((await page.evaluate(()=>window.fixture.snapshot())).map(i=>i.id),['old-image','new.png']);
    await page.evaluate(()=>{const transfer=new DataTransfer();transfer.items.add(new File(['png'],'dropped.png',{type:'image/png'}));document.querySelector('section').dispatchEvent(new DragEvent('drop',{bubbles:true,cancelable:true,dataTransfer:transfer}));});
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.waitForFunction(()=>window.fixture.snapshot().length===3 && window.fixture.snapshot().every(i=>i.status==='ready'));
    await page.setViewportSize({width:390,height:844});
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
    fs.mkdirSync(path.resolve(root,'../work'),{recursive:true});
    await page.screenshot({path:path.resolve(root,'../work/post-attachments-mobile.png'),fullPage:true});
    assert.deepEqual(errors,[]);
    console.log('PASS edit retain/remove/add, native file drop, mobile overflow, no browser exceptions. Fixture checks only.');
    }
    await page.setViewportSize({width:520,height:900});
    await page.goto(base+'?composer');
    await page.locator('textarea').first().waitFor();
    await page.evaluate(data=>{const bytes=Uint8Array.from(atob(data),c=>c.charCodeAt(0));const transfer=new DataTransfer();transfer.items.add(new File([bytes],'single.png',{type:'image/png'}));document.querySelector('textarea').dispatchEvent(new DragEvent('drop',{bubbles:true,cancelable:true,dataTransfer:transfer}));},png.toString('base64'));
    await page.waitForFunction(()=>document.querySelectorAll('section ol li').length===1);
    assert.deepEqual(await page.evaluate(()=>window.fixture.calls),{});
    await page.screenshot({path:path.resolve(root,'../work/post-composer-single.png'),fullPage:true,animations:'disabled'});
    await page.goto(base+'?composer');
    assert.deepEqual(await page.evaluate(()=>window.fixture.calls),{});
    console.log('PASS selecting then leaving composer makes zero upload calls');
    await page.setViewportSize({width:390,height:844});
    await page.goto(base+'?composer');await page.locator('textarea').first().waitFor();
    await page.evaluate(data=>{const bytes=Uint8Array.from(atob(data),c=>c.charCodeAt(0));const transfer=new DataTransfer();for(let i=0;i<3;i++)transfer.items.add(new File([bytes],'composer-'+i+'.png',{type:'image/png'}));document.querySelector('textarea').dispatchEvent(new DragEvent('drop',{bubbles:true,cancelable:true,dataTransfer:transfer}));},png.toString('base64'));
    await page.waitForFunction(()=>document.querySelectorAll('section ol li').length===3)
      .catch(async error=>{console.log(JSON.stringify(await page.evaluate(()=>({text:document.body.innerText,calls:window.fixture.calls,active:window.fixture.active}))));console.log(errors);throw error;});
    assert.deepEqual(await page.evaluate(()=>window.fixture.calls),{});
    await page.screenshot({path:path.resolve(root,'../work/post-composer-mobile.png'),fullPage:true,animations:'disabled'});
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
    assert.equal(await page.getByRole('button',{name:'Đăng',exact:true}).isDisabled(),false);
    await page.getByRole('button',{name:'Đăng',exact:true}).click();
    await page.waitForFunction(()=>window.fixture.saved?.length===3);
    assert.deepEqual(await page.evaluate(()=>window.fixture.saved),['composer-0.png','composer-1.png','composer-2.png']);
    assert.equal(await page.evaluate(()=>window.fixture.begins),1);
    const uploadOptions=await page.evaluate(()=>window.fixture.uploadOptions);
    assert.equal(new Set(uploadOptions.map(o=>o.publicationId)).size,1);
    assert.ok(uploadOptions.every(o=>o.publicationId));assert.equal(new Set(uploadOptions.map(o=>o.fileKey)).size,3);
    assert.deepEqual(errors,[]);console.log('PASS full composer accepts drop and enables submit with ready attachments');
    await page.goto(base+'?composer-failure');await page.locator('textarea').first().fill('Keep this draft');
    await page.evaluate(data=>{const transfer=new DataTransfer();transfer.items.add(new File([Uint8Array.from(atob(data),c=>c.charCodeAt(0))],'retry.png',{type:'image/png'}));document.querySelector('textarea').dispatchEvent(new DragEvent('drop',{bubbles:true,cancelable:true,dataTransfer:transfer}));},png.toString('base64'));
    await page.getByRole('button',{name:'Đăng',exact:true}).click();await page.waitForFunction(()=>window.fixture.cancelled);
    assert.equal(await page.locator('textarea').first().inputValue(),'Keep this draft');
    await page.getByRole('button',{name:'Đăng',exact:true}).click();await page.waitForFunction(()=>window.fixture.saved?.length===1);
    assert.equal(await page.evaluate(()=>window.fixture.begins),2);
    assert.equal(await page.evaluate(()=>new Set(window.fixture.uploadOptions.map(o=>o.publicationId)).size),2);
    console.log('PASS failed publish cancels old publication, preserves draft, retries under a new publication');
    await page.goto(base+'?composer');await page.locator('textarea').first().fill('Close while uploading');
    await page.evaluate(data=>{const transfer=new DataTransfer();transfer.items.add(new File([Uint8Array.from(atob(data),c=>c.charCodeAt(0))],'close.png',{type:'image/png'}));document.querySelector('textarea').dispatchEvent(new DragEvent('drop',{bubbles:true,cancelable:true,dataTransfer:transfer}));},png.toString('base64'));
    await page.getByRole('button',{name:'Đăng',exact:true}).click();await page.waitForFunction(()=>window.fixture.active>0);
    await page.getByRole('button',{name:'Đóng trình soạn bài',exact:true}).click();await page.waitForFunction(()=>window.fixture.cancelled&&window.fixture.active===0);
    assert.equal(await page.evaluate(()=>window.fixture.saved),undefined);
    console.log('PASS closing during upload aborts files and requests publication cancellation');
  } finally { await browser?.close(); await server.close(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
