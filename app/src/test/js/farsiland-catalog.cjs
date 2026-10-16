// Offline characterization of the shipped public metadata script, not a network test.
const fs=require('fs'),vm=require('vm'),assert=require('assert/strict'),path=require('path');
const source=fs.readFileSync(path.resolve(__dirname,'../../main/java/com/amin/tvos/browser/FarsiLandScripts.kt'),'utf8');
const parser=source.split('val cardParser = """')[1].split('""".trimIndent()')[0];
const template=source.split('fun catalog(pageLimit:Int,pageStart:Int=1):String = """')[1].split('""".trimIndent()')[0];
const script=template.replace('$cardParser',parser).replaceAll('${pageStart}','1').replaceAll('${pageLimit}','1');
let active=0,peak=0,requested=[],result;
const archives=['/iran-movie-2025/','/series-26/','/old-iranian-movies/','/iranian-series/'];
function fakeDoc(archive){
  const series=archive.includes('series'),kind=series?'tvshows':'movies';
  const slug=archive.replaceAll('/','');
  const cards=[{querySelector(selector){
    if(selector==='h3 a[href]')return {textContent:slug,getAttribute:()=>`/${kind}/${slug}/`};
    if(selector==='.poster img')return {getAttribute:()=>`https://farsiland.com/art/${slug}.jpg`};
  }}];
  return {querySelectorAll:selector=>selector==='article.item'?cards:[]};
}
const context=vm.createContext({URL,location:{origin:'https://farsiland.com'},
  DOMParser:class{parseFromString(html){return fakeDoc(html)}},
  fetch:async(url,options)=>{
    assert.equal(options.credentials,'same-origin');
    const u=new URL(url);assert.equal(u.origin,'https://farsiland.com');assert(archives.includes(u.pathname));
    requested.push(u.pathname);active++;peak=Math.max(peak,active);
    await new Promise(resolve=>setImmediate(resolve));active--;
    return {ok:true,text:async()=>u.pathname};
  },
  AminCatalog:{section:(id,json)=>{assert.equal(id,'farsiland');result=JSON.parse(json)},failed:()=>assert.fail('catalog failed')}
});
(async()=>{
  await vm.runInContext(script,context);
  assert.equal(peak,2,'public metadata requests must stay bounded to two');
  assert.deepEqual([...requested].sort(),[...archives].sort());
  assert.equal(result.movies.length,2);assert.equal(result.series.length,2);assert.equal(result.all.length,4);
  assert(result.movies.every(x=>x.kind==='MOVIE'));assert(result.series.every(x=>x.kind==='SERIES'));
  assert.equal(result.hasMoreAll,false);
  const host=source.match(/const val catalogHost = "(.*)"/)[1];
  assert(!/<(?:script|img|iframe|link)\b/i.test(host),'inert host must not execute site code or load artwork');
  console.log('PASS: inert host, four same-origin archives, at most two requests, movie/series separation');
})().catch(error=>{console.error(error);process.exitCode=1});
