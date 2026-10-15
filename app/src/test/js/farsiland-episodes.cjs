// Offline DOM characterization of the exact production script. No network/media access.
const fs=require('fs'),path=require('path'),vm=require('vm'),assert=require('assert/strict');
const source=fs.readFileSync(path.resolve(__dirname,'../../main/java/com/amin/tvos/browser/FarsiLandScripts.kt'),'utf8');
const script=source.split('val episodes = """')[1].split('""".trimIndent()')[0];
const origin='https://farsiland.com',series='/tvshows/fun-abutaleb/';
const anchor=(href,text='')=>({textContent:text,getAttribute:()=>href});
const episode=(n,href=origin+'/episodes/fun-abutaleb-ep0'+n+'/',soon=false)=>{
  const row={textContent:'Episode '+n+(soon?' Soon':''),querySelector:s=>s==='.numerando'?{textContent:'1 - '+n}:anchor(href)};
  return {closest:()=>row};
};
const doc=(rows=[],heading='Fun Abutaleb',links=[],parent=series)=>({
  querySelector:s=>s==='h1'?{textContent:heading}:null,
  querySelectorAll:s=>s==='.episodiotitle'?rows:s==='h3 a[href]'?links:s==='.pag_episodes a[href]'?[anchor(origin+parent,'ALL')]:[]
});
async function run(main,home,ep){
  let result;const requests=[];
  const context=vm.createContext({window:{},document:main,location:{hostname:'farsiland.com',origin,pathname:series},URL,AbortController,setTimeout,clearTimeout,JSON,Array,
    DOMParser:class {parseFromString(text){return text==='home'?home:ep;}},
    fetch:async url=>{requests.push(url);return {ok:true,text:async()=>url===origin+'/home'?'home':'episode'};},
    AminEpisode:{result:s=>{result=s;}}
  });
  await vm.runInContext(script,context);
  return {result:JSON.parse(result),requests};
}
const episodes=x=>x.result.editions[0].seasons.flatMap(s=>s.episodes);
(async()=>{
  const direct=await run(doc([episode(1),episode(2)]));assert.equal(episodes(direct).length,2);assert.equal(direct.requests.length,0);
  const home=doc([],null,[anchor(origin+'/episodes/fun-abutaleb-ep04/','Fun Abutaleb EP04')]);
  const fallback=await run(doc(),home,doc([1,2,3,4].map(n=>episode(n))));assert.equal(episodes(fallback).length,4);assert.equal(fallback.requests.length,2);
  const unrelated=await run(doc(),home,doc([episode(1)],null,[],'/tvshows/another-show/'));assert.equal(episodes(unrelated).length,0);
  const mismatch=await run(doc(),doc([],null,[anchor(origin+'/episodes/other/','Other Show EP04')]),doc([episode(1)]));assert.equal(episodes(mismatch).length,0);assert.equal(mismatch.requests.length,1);
  const unsafe=await run(doc(),doc([],null,[anchor('https://evil.test/episodes/fun-abutaleb-ep04/','Fun Abutaleb EP04')]),doc([episode(1)]));assert.equal(episodes(unsafe).length,0);assert.equal(unsafe.requests.length,1);
  const unavailable=await run(doc([episode(1,origin+'/episodes/fun-abutaleb-ep01/',true),episode(2,'https://evil.test/episodes/fake/')]));assert.equal(episodes(unavailable).length,1);assert.equal(episodes(unavailable)[0].isAvailableOnline,false);
  console.log('PASS: direct rows, verified-parent fallback, wrong-parent rejection, title mismatch, foreign-host rejection, unavailable episode');
})().catch(e=>{console.error(e);process.exitCode=1;});
