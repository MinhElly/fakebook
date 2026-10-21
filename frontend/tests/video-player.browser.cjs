const {chromium}=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const assert=require('node:assert/strict'),path=require('node:path');
const html=`<!doctype html><html><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><body><div id="root"></div><script type="module">import R from '/@react-refresh';R.injectIntoGlobalHook(window);window.$RefreshReg$=()=>{};window.$RefreshSig$=()=>x=>x;window.__vite_plugin_react_preamble_installed__=true;</script><script type="module" src="/tests/video-fixture.tsx"></script></body></html>`;
(async()=>{
 const {createServer}=await import('vite');const root=path.resolve(__dirname,'..');
 const server=await createServer({root,server:{port:0,host:'127.0.0.1'},plugins:[{name:'video-fixture',configureServer(v){v.middlewares.use((req,res,next)=>{
 if(req.url==='/test-video.webm'){res.setHeader('Content-Type','video/webm');res.end(require('node:fs').readFileSync(path.resolve(__dirname,'fixtures/video.webm')));return;}
 let body;if(req.url?.startsWith('/fixture'))body=html;
 else if(req.url?.split('?')[0]==='/src/services/apis.ts')body=`export default {post:async()=>{console.log('ticket requested',window.fixture?.url);return {data:{ticket:'fixture',path:window.fixture.url}}}};`;
 else if(req.url?.split('?')[0]==='/src/components/feed/Post.tsx')body=`export default ()=>null;`;
 if(body){res.setHeader('Content-Type',req.url.startsWith('/fixture')?'text/html':'application/javascript');res.end(body);}else next();
 });}}]});let browser;
 try{await server.listen();browser=await chromium.launch({channel:'chrome',headless:true});const page=await browser.newPage({viewport:{width:1200,height:800}});page.setDefaultTimeout(60000);
 const errors=[];page.on('console',m=>{if(m.type()==='error'||m.text().startsWith('ticket')||m.text().startsWith('media-error'))console.log(m.text());});await page.addInitScript(()=>document.addEventListener('error',e=>{if(e.target instanceof HTMLVideoElement)console.log('media-error',e.target.error?.message,e.target.currentSrc);},true));page.on('pageerror',e=>errors.push(e.message));
 await page.goto('http://127.0.0.1:'+server.httpServer.address().port+'/fixture',{waitUntil:'domcontentloaded'});

 await page.locator('video').waitFor({state:'attached'});
 await page.waitForFunction(()=>{const v=document.querySelector('video');if(v)v.loop=true;return v&&!v.paused&&v.currentTime>0;});

 assert.equal(await page.locator('video').evaluate(v=>v.muted),true);
 assert.equal(await page.locator('video').evaluate(v=>v.getBoundingClientRect().height<=560),true);
 await page.evaluate(()=>scrollTo(0,900));await page.waitForFunction(()=>document.querySelector('video').paused);
 await page.evaluate(()=>scrollTo(0,0));await page.waitForFunction(()=>!document.querySelector('video').paused);
 await page.getByRole('button',{name:'Mở trình xem video'}).click();await page.getByRole('dialog').waitFor();await page.waitForFunction(()=>document.querySelector('main > div video')?.paused ?? true);
 await page.getByRole('button',{name:'Ẩn bình luận'}).click();assert.equal(await page.getByRole('complementary').count(),0);
 await page.screenshot({path:path.resolve(root,'../work/video-viewer-wide.png'),animations:'disabled'});
 await page.getByRole('button',{name:'Hiện bình luận'}).click();assert.equal(await page.getByRole('complementary').count(),1);
 await page.screenshot({path:path.resolve(root,'../work/video-viewer-comments.png'),animations:'disabled'});
 await page.keyboard.press('Escape');assert.equal(await page.getByRole('dialog').count(),0);
 await page.setViewportSize({width:390,height:844});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);
 await page.screenshot({path:path.resolve(root,'../work/video-feed-mobile.png'),animations:'disabled'});
 assert.deepEqual(errors,[]);console.log('PASS real local video: muted autoplay, pause offscreen, resume, bounded dimensions, viewer toggle/Escape, mobile overflow. Cloudinary not exercised.');
 }catch(error){console.log(await browser?.contexts()[0]?.pages()[0]?.evaluate(()=>({text:document.body.innerText,video:document.querySelector('video')?{src:document.querySelector('video').getAttribute('src'),paused:document.querySelector('video').paused,ready:document.querySelector('video').readyState,error:document.querySelector('video').error?.message}:null})));throw error;}finally{await browser?.close();await server.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
