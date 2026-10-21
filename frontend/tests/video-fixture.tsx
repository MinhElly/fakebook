// @ts-nocheck
import React from "react";
import {createRoot} from "react-dom/client";
import "../src/index.css";
import {loadRuntimeConfig} from "../src/config/runtime-config";
await loadRuntimeConfig();
const {default:Grid}=await import("../src/components/feed/MediaGrid");
const {default:Viewer}=await import("../src/components/feed/PostMediaViewer");
window.fixture={url:"/test-video.webm"};
const post={id:"post",user:"Nguyen Minh",content:"Video xem truoc tren Feed",mediaIds:["video"],media:[{id:"video",mediaType:"VIDEO"}]};
function App(){const [open,setOpen]=React.useState(false);return <main style={{width:"min(680px,100%)",margin:"20px auto"}}><h2>Nguyen Minh</h2><Grid mediaIds={post.mediaIds} media={post.media} onOpen={()=>setOpen(true)}/><div style={{height:1400}}>Next post</div>{open&&<Viewer post={post} initialIndex={0} onClose={()=>setOpen(false)} onReactionChange={()=>{}}/>}</main>}
createRoot(document.getElementById("root")).render(<App/>);
