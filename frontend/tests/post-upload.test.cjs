const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const source = fs.readFileSync(path.resolve(__dirname,'../src/services/mediaService.ts'),'utf8');
const code = ts.transpileModule(source,{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,esModuleInterop:true}}).outputText;

function harness({retryFirst=false,abortAfterFirst}={}) {
  const signRequests=[];const calls=[];let signs=0,completes=0,failed=false;
  const api={post:async(url,body)=>{
    if(url.endsWith('/sign')){signRequests.push(body);signs++;return {data:{sessionId:'session',uploadUrl:'https://api.cloudinary.com/test/video/upload',apiKey:'public-key',signature:'signature',parameters:{timestamp:123,public_id:'reserved',type:'authenticated'}}};}
    completes++;return {data:{id:'verified-id',mediaType:'VIDEO'}};
  }};
  const axios={isAxiosError:error=>!!error.isAxiosError,post:async(url,form,config)=>{
    if(config.signal?.aborted)throw new DOMException('Aborted','AbortError');
    calls.push({range:config.headers?.['Content-Range'],chunk:config.headers?.['X-Unique-Upload-Id'],size:form.get('file').size});
    assert.equal(config.headers?.Authorization,undefined);
    assert.equal(form.get('public_id'),'reserved');
    if(retryFirst&&!failed){failed=true;throw Object.assign(new Error('offline'),{isAxiosError:true});}
    if(abortAfterFirst&&calls.length===1)abortAfterFirst.abort();
    config.onUploadProgress?.({loaded:form.get('file').size});
    return {data:{done:true}};
  }};
  const output={};new Function('require','exports',code)(name=>name==='axios'?axios:name==='./apis'?api:{resolveApiUrl:value=>value},output);
  return {upload:output.uploadPostMedia,calls,signRequests,counts:()=>({signs,completes})};
}
(async()=>{
 const size=13*1024*1024;const file=new File([new Uint8Array(size)],'movie.mp4',{type:'video/mp4'});
 const first=harness({retryFirst:true});assert.equal((await first.upload(file)).id,'verified-id');
 assert.equal(first.calls.length,4);assert.equal(first.calls[0].range,first.calls[1].range);
 assert.equal(first.calls[2].range,`bytes ${6*1024*1024}-${12*1024*1024-1}/${size}`);
 assert.equal(first.calls[3].range,`bytes ${12*1024*1024}-${size-1}/${size}`);
 assert.equal(new Set(first.calls.map(call=>call.chunk)).size,1);assert.deepEqual(first.counts(),{signs:1,completes:1});
 await first.upload(file);assert.deepEqual(first.counts(),{signs:1,completes:1});
 const controller=new AbortController();const resumed=harness({abortAfterFirst:controller});
 await assert.rejects(resumed.upload(file,{signal:controller.signal}));
 await resumed.upload(file);assert.equal(resumed.calls[1].range,`bytes ${6*1024*1024}-${12*1024*1024-1}/${size}`);
 assert.deepEqual(resumed.counts(),{signs:1,completes:1});
 const bound=harness();await bound.upload(file,{publicationId:'one',fileKey:'file-key'});await bound.upload(file,{publicationId:'one',fileKey:'file-key'});
 assert.equal(bound.counts().signs,1);await bound.upload(file,{publicationId:'two',fileKey:'file-key'});assert.equal(bound.counts().signs,2);
 assert.deepEqual(bound.signRequests.map(r=>r.publicationId),['one','two']);assert.ok(bound.signRequests.every(r=>r.fileKey==='file-key'));
 console.log('PASS actual uploader: chunk boundaries, retry same chunk/id, no bearer to provider, cached completion, resume after abort. Mock HTTP only.');
})().catch(error=>{console.error(error);process.exitCode=1;});
