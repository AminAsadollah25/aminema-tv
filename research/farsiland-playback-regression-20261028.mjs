// Pure local DOM fixtures. Not evidence of live playback or ad removal.
import fs from 'node:fs';
import vm from 'node:vm';
import assert from 'node:assert/strict';
const source=fs.readFileSync(new URL('../app/src/main/java/com/amin/tvos/browser/FarsiLandScripts.kt',import.meta.url),'utf8');
const entry=source.match(/val entryQualityNodes = """([\s\S]*?)"""/)[1];
const filter=source.match(/val adRequestFilter = """([\s\S]*?)"""\.trimIndent/)[1];
const playback=source.match(/val playback = """([\s\S]*?)"""\.trimIndent/)[1].replace('$entryQualityNodes',entry).replace('$adRequestFilter',filter);
function fixture({host='farsiland.buzz',path='/',text='',choices=[],video=null,quality=[],start=null,go=null}={}) {
 const body={innerText:text,appendChild(box){box.parentElement=body;if(video)video.paused=true;}};
 const ctx=vm.createContext({window:{},location:{hostname:host,pathname:path,search:'?public'},Date,JSON,
  getComputedStyle:()=>({visibility:'visible'}),
  document:{body,head:{appendChild(){}},getElementById:()=>({}),
   querySelector:s=>s.includes('#player video')?video:s.includes('.fakeplayer')?go:null,
   querySelectorAll:s=>s==='a.btn-group'?choices:s.includes('button[data-plyr="quality"]')?quality:s.includes('[role="button"]')?(start?[start]:[]):[]}});
 const run=()=>JSON.parse(vm.runInContext(playback,ctx));run.replaceVideo=next=>{video=next;};run.setUserPaused=value=>{ctx.window.__aminUserPaused=value;};return run;
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
run=fixture({host:'farsiplex.com',video:movingVideo});assert.equal(run().state,'playing');assert.equal(resumes,1);assert.equal(movingBox.parentElement,null);
console.log('PASS theater styling never detaches or reparents the media player');
resumes=0;movingBox.parentElement=null;movingVideo.paused=true;
assert.equal(run().state,'paused');assert.equal(resumes,0);
console.log('PASS theater styling does not add a resume for an already paused video');
console.log('9 local playback fixtures passed; live QA is separate.');
quality=['1080','720','480'].map(value=>({...node(value),value,getAttribute:n=>n==='aria-checked'&&value==='1080'?'true':null}));
const already={...video,readyState:0,currentTime:0,paused:true};
run=fixture({host:'farsiplex.com',path:'/play-online-video/',video:already,quality});
assert.equal(run().state,'buffering');
console.log('PASS already-selected preferred quality does not trigger a redundant quality click');

function adFixture({host='farsiplex.com',path='/play-online-video/',gate=false,known=true}={}){
 const tasks=[],listeners={};
 const original=known?function requestImaAd(_tag,_success,onFail){/* google.ima.AdsRequest; adsLoader.requestAds; onFail */throw Error('network should not run');}:function unknownRequest(){};
 const ctx=vm.createContext({window:{requestImaAd:original,playCriticalAd(){},resumeContent(){throw Error('must not directly resume content');}},location:{hostname:host,pathname:path},Function,setTimeout:f=>tasks.push(f),document:{body:{innerText:gate?'Unlock more content':''},addEventListener:(name,fn)=>listeners[name]=fn,querySelector:()=>null}});
 vm.runInContext(filter,ctx);
 return {ctx,original,flush(){let limit=100;while(tasks.length){assert.ok(limit-->0);tasks.shift()();}},listeners};
}
let ad=adFixture(),failed=0,succeeded=0;
ad.ctx.window.requestImaAd(new Proxy({},{get(){throw Error('ad tag must not be read');}}),()=>succeeded++,()=>failed++);
assert.equal(failed,0);ad.flush();assert.equal(failed,1);assert.equal(succeeded,0);
console.log('PASS blocked IMA uses asynchronous onFail only, no tag read or false ad success');
ad=adFixture();failed=0;
ad.ctx.window.__aminImaFilter.suspended=true;
ad.ctx.window.requestImaAd(null,null,()=>failed++);ad.flush();assert.equal(failed,0);
ad.ctx.window.__aminImaFilter.suspended=false;ad.ctx.window.__aminImaFilter.flush();assert.equal(failed,1);
ad.ctx.window.__aminUserPaused=true;
ad.ctx.window.requestImaAd(null,null,()=>failed++);ad.flush();assert.equal(failed,1);
ad.ctx.window.__aminImaFilter.flush();assert.equal(failed,1);
ad.ctx.window.__aminUserPaused=false;ad.ctx.window.__aminImaFilter.flush();assert.equal(failed,2);
console.log('PASS background and explicit user Pause defer ad-failure callbacks until permitted');
for(const opts of [{host:'evil.test'},{path:'/explore/'},{gate:true},{known:false}]){
 ad=adFixture(opts);assert.equal(ad.ctx.window.requestImaAd,ad.original);assert.equal(ad.ctx.window.__aminImaFilter,undefined);
}
console.log('PASS unknown hosts, routes, access gates and unrecognized request code are unchanged');
ad=adFixture();failed=0;
function chain(n){if(!n)return;ad.ctx.window.requestImaAd(null,null,()=>{failed++;chain(n-1);});}
chain(6);ad.flush();assert.equal(failed,6);
const installed=ad.ctx.window.requestImaAd;vm.runInContext(filter,ad.ctx);assert.equal(ad.ctx.window.requestImaAd,installed);
console.log('PASS six-tag waterfall finishes without network waits; filter installs once per document');
console.log('14 local playback/filter fixtures passed; no live ad-free guarantee.');
let unsolicitedPlays=0;
const replacement={readyState:0,currentTime:0,paused:true,error:null,play(){unsolicitedPlays++;this.paused=false;return {catch(){}};}};
run=fixture({host:'farsiplex.com',path:'/play-online-video/',video:replacement});run.setUserPaused(true);
assert.equal(run().paused,true);assert.equal(unsolicitedPlays,0);
run.replaceVideo({...replacement});assert.equal(run().paused,true);assert.equal(unsolicitedPlays,0);
console.log('PASS manual Pause survives new/replaced video before the first frame; no forced autoplay');
run.setUserPaused(false);run();assert.equal(unsolicitedPlays,0);
// Explicit Resume is handled by togglePlayback, not a new autoplay/retry side effect.
console.log('PASS clearing manual intent alone does not silently restart the same video');
let pausedAgain=0;const restored={...replacement,paused:false,pause(){pausedAgain++;this.paused=true;}};
run=fixture({host:'farsiplex.com',path:'/play-online-video/',video:restored});run.setUserPaused(true);
assert.equal(run().paused,true);assert.equal(pausedAgain,1);assert.equal(unsolicitedPlays,0);
console.log('PASS provider auto-resume cannot override an explicit Pause after a player swap');
start=node('Play');run=fixture({host:'farsiplex.com',path:'/play-online-video/',start});run.setUserPaused(true);
assert.equal(run().state,'waiting');assert.equal(start.clicked,undefined);
console.log('PASS new semantic Play control is not clicked while manual Pause is active');
console.log('18 local playback/filter/intent fixtures passed; live QA remains separate.');
const snapshot=source.match(/val stalledQualitySnapshot = """([\s\S]*?)"""\.trimIndent/)[1];
const choose=source.match(/fun selectStalledQuality[\s\S]*?return """([\s\S]*?)"""/)[1].replace('$label','1080');
let selected='720';const candidates=['2160','1080','720','480'].map(value=>({...node(value),value,getAttribute:n=>n==='aria-checked'&&selected===value?'true':null}));
const qctx=vm.createContext({window:{},location:{hostname:'farsiplex.com',pathname:'/play-online-video/'},document:{body:{innerText:''},querySelector:()=>null,querySelectorAll:()=>candidates}});
let snap=JSON.parse(vm.runInContext(snapshot,qctx));assert.equal(snap.gate,false);assert.deepEqual(snap.choices.map(x=>x.label),['1080','720','480']);
vm.runInContext(choose,qctx);assert.equal(candidates[1].clicked,1);
console.log('PASS stall recovery uses real numeric controls and excludes automatic 2160');
selected='1080';vm.runInContext(choose,qctx);assert.equal(candidates[1].clicked,1);selected='720';
qctx.window.__aminUserPaused=true;vm.runInContext(choose,qctx);assert.equal(candidates[1].clicked,1);qctx.window.__aminUserPaused=false;
qctx.window.__aminImaFilter={suspended:true};vm.runInContext(choose,qctx);assert.equal(candidates[1].clicked,1);qctx.window.__aminImaFilter.suspended=false;
console.log('PASS selected quality, manual Pause and background suspension veto late quality switches');
for(const [host,path,body] of [['evil.test','/play-online-video/',''],['farsiplex.com','/explore/',''],['farsiplex.com','/play-online-video/','Unlock more content']]){
 qctx.location.hostname=host;qctx.location.pathname=path;qctx.document.body.innerText=body;
 assert.equal(JSON.parse(vm.runInContext(snapshot,qctx)).gate,true);vm.runInContext(choose,qctx);assert.equal(candidates[1].clicked,1);
}
console.log('PASS stall recovery refuses unknown origin, non-player route and access gate');
console.log('21 local playback/filter/intent/stall fixtures passed; live QA remains separate.');
const retry=source.match(/val retryStalledSeek = """([\s\S]*?)"""\.trimIndent/)[1];
const stuck={paused:false,seeking:true,readyState:1,currentTime:82.87};
const rctx=vm.createContext({window:{},location:{hostname:'farsiplex.com',pathname:'/play-online-video/'},isFinite,Math,document:{body:{innerText:''},querySelector:()=>stuck}});
assert.equal(vm.runInContext(retry,rctx),true);assert.equal(stuck.currentTime,79);
console.log('PASS stuck seek retries only backwards, preserving unseen content');
for(const override of [{paused:true},{seeking:false},{readyState:4},{currentTime:2},{currentTime:NaN}]){
 Object.assign(stuck,{paused:false,seeking:true,readyState:1,currentTime:82.87},override);
 const before=stuck.currentTime;assert.equal(vm.runInContext(retry,rctx),false);assert.ok(Object.is(stuck.currentTime,before));
}
console.log('PASS paused, progressing, non-seeking and invalid/early positions never trigger seek recovery');
for(const opts of [{pause:true},{suspended:true},{gate:true},{host:'evil.test'},{path:'/explore/'}]){
 Object.assign(stuck,{paused:false,seeking:true,readyState:1,currentTime:82.87});
 rctx.window.__aminUserPaused=!!opts.pause;rctx.window.__aminImaFilter={suspended:!!opts.suspended};
 rctx.location.hostname=opts.host||'farsiplex.com';rctx.location.pathname=opts.path||'/play-online-video/';rctx.document.body.innerText=opts.gate?'Unlock more content':'';
 assert.equal(vm.runInContext(retry,rctx),false);assert.equal(stuck.currentTime,82.87);
}
console.log('PASS seek recovery respects late Pause, lifecycle, origin and access gate');
assert.ok(playback.includes('backdrop-filter:none!important'));assert.ok(playback.includes('top:0!important;right:0!important;bottom:0!important;left:0!important;'));
assert.ok(playback.includes("parent!==document.body"));assert.ok(!playback.includes('document.body.appendChild(box)'));
console.log('PASS theater keeps DOM parent and neutralizes fixed-containing-block backdrop only on ancestors');
console.log('25 local playback fixtures passed; live QA remains separate.');
