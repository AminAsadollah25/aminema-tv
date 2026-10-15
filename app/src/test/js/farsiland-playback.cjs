// Source-based DOM characterization; no browser/network/media access.
// Run: node app/src/test/js/farsiland-playback.cjs
const fs=require('fs'),vm=require('vm'),assert=require('assert/strict'),path=require('path');
const source=fs.readFileSync(path.resolve(__dirname,'../../main/java/com/amin/tvos/browser/FarsiLandScripts.kt'),'utf8');
const script=source.split('val playback = """')[1].split('""".trimIndent()')[0];
let calls=0,clicks=0,mounted=0;
const body={innerText:'',appendChild:e=>{e.parentElement=body}};
const box={parentElement:body,classList:{add(){mounted++;}}};
const video={readyState:0,currentTime:0,paused:true,error:null,closest:()=>box,play(){calls++;this.paused=false;return Promise.resolve();}};
const playButton={getClientRects:()=>[{}],click(){clicks++;video.paused=!video.paused;}};
const document={body,querySelector:s=>s.includes('video')?video:playButton,getElementById:()=>({})};
const context=vm.createContext({window:{},document,getComputedStyle:()=>({visibility:'visible'}),location:{hostname:'farsiplex.com',pathname:'/smart-play',search:''},JSON,Array});
const inspect=()=>JSON.parse(vm.runInContext(script,context));
assert.equal(inspect().state,'buffering');assert.equal(calls,1);
assert.equal(clicks,0,'UI toggle must not pause an already requested play');
video.paused=true;video.readyState=1;assert.equal(inspect().state,'buffering');assert.equal(calls,2,'metadata-ready must retry a pre-start pause');assert.equal(mounted,1,'prepare visible theater before the first frame without claiming playback');
video.currentTime=20;video.readyState=4;video.paused=true;
assert.equal(inspect().state,'paused');assert.equal(calls,2,'must preserve manual pause after playback starts');
video.currentTime=0;assert.equal(inspect().state,'paused');assert.equal(calls,2,'seeking to zero must not override a manual pause');assert.equal(clicks,0);
body.innerText='Unlock more content';assert.equal(inspect().state,'gate');assert.equal(calls,2);
body.innerText='';
const adOnly=vm.createContext({window:{},document:{...document,querySelector:s=>s==='video'?video:null},location:{hostname:'farsiplex.com',pathname:'/smart-play',search:''},JSON,Array});
assert.equal(JSON.parse(vm.runInContext(script,adOnly)).state,'waiting','an unrelated video must not be mistaken for the provider player');
console.log('PASS: initial attempt, no accidental pause toggle, metadata-ready retry, manual-pause preservation, access-gate preservation');
