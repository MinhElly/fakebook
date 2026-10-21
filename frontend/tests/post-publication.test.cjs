const assert=require("node:assert/strict"),fs=require("node:fs"),path=require("node:path"),ts=require("typescript");
const code=ts.transpileModule(fs.readFileSync(path.resolve(__dirname,"../src/services/postPublication.ts"),"utf8"),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022,esModuleInterop:true}}).outputText;
function harness(api){const output={};new Function("require","exports",code)(()=>api,output);return output;}
(async()=>{
 const lost=new Error("lost response");let posts=0,reads=0;
 const replay=harness({post:async()=>{posts++;throw lost;},get:async()=>{reads++;return {data:{id:"key",status:"PUBLISHED",post:{id:"post",mediaIds:["media"]}}};}});
 assert.equal((await replay.publishPublication("key",["media"])).id,"post");assert.equal(posts,1);assert.equal(reads,1);
 for(const status of ["PENDING","CANCELLED"]){const pending=harness({post:async()=>{throw lost;},get:async()=>({data:{status}})});await assert.rejects(pending.publishPublication("key",[]),/lost response/);}
 const offline=harness({post:async()=>{throw lost;},get:async()=>{throw new Error("offline");}});await assert.rejects(offline.publishPublication("key",[]),/lost response/);
 const mismatch=harness({post:async()=>{throw lost;},get:async()=>({data:{status:"PUBLISHED",post:{id:"post",mediaIds:["other"]}}})});await assert.rejects(mismatch.publishPublication("key",["media"]),/lost response/);
 const incomplete=harness({post:async()=>({data:{status:"PENDING"}})});await assert.rejects(incomplete.publishPublication("key",[]),/has not committed/);
 console.log("PASS publication client: recover committed lost response, never report pending/cancelled/unknown as success.");
})().catch(e=>{console.error(e);process.exitCode=1;});
