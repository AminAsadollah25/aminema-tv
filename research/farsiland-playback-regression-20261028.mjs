// Pure local DOM fixtures. Not evidence of live playback or ad removal.
import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const source=fs.readFileSync(new URL('../app/src/main/java/com/amin/tvos/browser/FarsiLandScripts.kt',import.meta.url),'utf8');
const entry=source.match(/val entryQualityNodes = """([\s\S]*?)"""/)[1];
const playback=source.match(/val playback = """([\s\S]*?)"""\.trimIndent/)[1].replace('$entryQualityNodes',entry);
function fixture({host='farsiland.buzz',path='/',text='',choices=[],video=null,quality=[],start=null,go=null}={}) {
 const body={innerText:text,appendChild(box){box.parentElement=body;if(video)video.paused=true;}};
 const ctx=vm.createContext({window:{},location:{hostname:host,pathname:path,search:'?public'},Date,JSON,
  getComputedStyle:()=>({visibility:'visible'}),
  document:{body,head:{appendChild(){}},getElementById:()=>({}),
   querySelector:s=>s.includes('#player video')?video:s.includes('.fakeplayer')?go:null,
   querySelectorAll:s=>s==='a.btn-group'?choices:s.includes('button[data-plyr="quality"]')?quality:s.includes('[role="button"]')?(start?[start]:[]):[]}});
 const run=()=>JSON.parse(vm.runInContext(playback,ctx));run.replaceVideo=next=>{video=next;};return run;
}
const node=(label)=>({textContent:label,getClientRects:()=>[{}],click(){this.clicked=(this.clicked||0)+1;},getAttribute:n=>n==='aria-label'?label:null});
let labels=['480','720','1080','2160','Watch Advert'].map(node);
let run=fixture({choices:labels});
assert.deepEqual(run(),{state:'quality',labels:['480','720','1080','2160']});
console.log('PASS quality controls survive removal of btn-row; unrelated ad label excluded');
let start=node('Play');run=fixture({host:'farsiplex.com',path:'/play-online-video/',start});
assert.equal(run().state,'opening-player');assert.equal(start.clicked,1);assert.equal(run().state,'waiting');
console.log('PASS visible semantic JW Play before a video exists; clicked once');
start=node('Play');run=fixture({host:'farsiplex.com',text:'Unlock more content',start});
assert.equal(run().state,'gate');assert.equal(start.clicked,undefined);
console.log('PASS access gate never clicked or bypassed');
let go=node('HD');run=fixture({host:'farsiplex.com',path:'/episode/real-episode/',go});
assert.equal(run().state,'opening');assert.equal(go.clicked,1);assert.equal(run().state,'waiting');
console.log('PASS real FarsiPlex episode HD entry clicked once');
const video={error:null,readyState:0,currentTime:0,paused:true,play(){this.paused=false;return {catch(){}}}};
let quality=['2160','480','720','1080'].map(value=>({...node(value),value}));
run=fixture({host:'farsiplex.com',path:'/play-elite-player/',video,quality});
assert.deepEqual(run(),{state:'player-quality',labels:['480','720','1080']});
assert.equal(run().state,'buffering');
run.replaceVideo({...video});assert.equal(run().state,'buffering');
console.log('PASS actual quality-button values only; no 2160 and no repeated selection');
const box={parentElement:null,classList:{contains:()=>true,add(){}}};
Object.assign(video,{readyState:4,currentTime:3,paused:false,closest:()=>box});
run=fixture({host:'farsiplex.com',video});assert.equal(run().state,'playing');
video.paused=true;assert.equal(run().state,'paused');
console.log('PASS content playback and manual pause retained');
run=fixture({host:'farsiplex.com',video:{error:{code:4}}});assert.equal(run().state,'player-error');
console.log('PASS real media error stays an error, never a playback success');
let resumes=0;const movingBox={parentElement:null,classList:{contains:()=>true,add(){}}};
const movingVideo={readyState:4,currentTime:2,paused:false,ended:false,closest:()=>movingBox,play(){resumes++;this.paused=false;return {catch(){}}}};
run=fixture({host:'farsiplex.com',video:movingVideo});assert.equal(run().state,'playing');assert.equal(resumes,2);
console.log('PASS theater reparenting resumes playback interrupted by our own layout move');
resumes=0;movingBox.parentElement=null;movingVideo.paused=true;
assert.equal(run().state,'paused');assert.equal(resumes,0);
console.log('PASS layout move does not add a resume for an already paused video');
console.log('9 local playback fixtures passed; live QA is separate.');
