package com.amin.tvos.browser

/** Selectors characterized on the public website on 2026-10-15. No media attributes read. */
object FarsiLandScripts {
    /** Trusted, inert origin host for public catalogue fetches; no site/ad scripts or artwork. */
    const val catalogHost = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Aminema catalog</title></head><body></body></html>"
    // Characterized on the site's smart-play quality controls; never read href/onclick.
    val playerQualityNodes = """Array.from(document.querySelectorAll('a')).filter(function(a){var p=a.querySelector('p');return p && /^(480|720|1080)$/.test(p.textContent.trim()) && a.getClientRects().length>0;})"""
    // Both controls were verified on the live site. Record the active choice too, so
    // recovery never bounces back to the engine that already failed on this title.
    val alternatePlayerNodes = """Array.from(document.querySelectorAll('a')).filter(function(a){var p=a.querySelector('p');return p && /^Player [12]$/.test(p.textContent.trim()) && a.getClientRects().length>0 && getComputedStyle(a).visibility!=='hidden';}).sort(function(a,b){return a.textContent.trim().localeCompare(b.textContent.trim());})"""
    val playback = """
        (function(){
          function visible(e){return e && e.getClientRects().length>0 && getComputedStyle(e).visibility!=='hidden';}
          function notePlayError(e){var name=e&&e.name;window.__aminPlayError=/^(NotAllowedError|AbortError|NotSupportedError|InvalidStateError)$/.test(name)?name:'Other';}
          var text=document.body?document.body.innerText:'';
          if(/Unlock more content|Take action to continue accessing/i.test(text))return JSON.stringify({state:'gate'});
          var v=document.querySelector('#player video,video#player')||document.querySelector('.jwplayer video,.plyr video');
          if(v){
            if(v.error)return JSON.stringify({state:'player-error',code:v.error.code});
            if(window.__aminVideo!==v){window.__aminVideo=v;window.__aminEverPlayed=false;window.__aminStartTries=0;window.__aminLastStart=0;window.__aminPlayError='';window.__aminPlayClicked=false;var p=v.play();if(p && p.catch)p.catch(notePlayError);}
            if(!window.__aminEverPlayed && v.readyState>=1 && v.currentTime===0 && v.paused && window.__aminStartTries<3 && Date.now()-window.__aminLastStart>1500){
              window.__aminStartTries++;window.__aminLastStart=Date.now();var readyPlay=v.play();if(readyPlay && readyPlay.catch)readyPlay.catch(notePlayError);
            }
            if(v.readyState>=1){
              var box=v.closest('.jwplayer,.plyr')||v;
              if(box.parentElement!==document.body)document.body.appendChild(box);
              if(!document.getElementById('aminema-theater-style')){
                var st=document.createElement('style');st.id='aminema-theater-style';
                st.textContent='html,body{background:#000!important;overflow:hidden!important;}'+
                  '.aminema-theater{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;'+
                  'max-width:none!important;max-height:none!important;margin:0!important;z-index:1000!important;background:#000!important;}'+
                  '.aminema-theater video,video.aminema-theater{width:100%!important;height:100%!important;object-fit:contain!important;}';
                document.head.appendChild(st);
              }
              if(!box.classList.contains('aminema-theater'))box.classList.add('aminema-theater');
            }
            if(v.readyState>=2 && (v.currentTime>0 || window.__aminEverPlayed)){
              window.__aminEverPlayed=true;
              return JSON.stringify({state:v.paused?'paused':'playing',paused:v.paused,time:Math.floor(v.currentTime),ready:v.readyState});
            }
            var play=document.querySelector('.jw-icon-playback,.plyr__control[data-plyr="play"],button[aria-label="Play"]');
            if(!window.__aminEverPlayed && v.paused && visible(play) && !window.__aminPlayClicked){window.__aminPlayClicked=true;play.click();}
            if(/This video file cannot be played|Error Code:/i.test(text))return JSON.stringify({state:'player-error'});
            return JSON.stringify({state:'buffering',ready:v.readyState,paused:v.paused,time:Math.floor(v.currentTime),playError:window.__aminPlayError||''});
          }
          if(/This video file cannot be played|Error Code:/i.test(text))return JSON.stringify({state:'player-error'});
          if(location.hostname.replace(/^www\./,'')==='farsiland.com' && /^\/(movies|episodes)\/[^/]+\/?$/.test(location.pathname)){
            var go=document.querySelector('.fakeplayer a[onclick]');
            if(visible(go) && !window.__aminEntryClicked){window.__aminEntryClicked=true;go.click();return JSON.stringify({state:'opening'});}
          }
          if((location.hostname.replace(/^www\./,'')==='farsiplex.com' && /^\/season\/?$/.test(location.pathname) && location.search) || location.hostname.replace(/^www\./,'')==='farsiland.buzz'){
            var choices=Array.from(document.querySelectorAll('.btn-row a.btn-group')).filter(visible);
            return JSON.stringify({state:'quality',labels:choices.map(function(a){return a.textContent.trim().slice(0,60);})});
          }
          return JSON.stringify({state:'waiting'});
        })();
    """.trimIndent()

    /** Normal metadata only; parse public archive or search cards, never player sources. */
    val cardParser = """
        function cards(doc){
          var seen={};return Array.from(doc.querySelectorAll('article.item')).map(function(c){
            var a=c.querySelector('h3 a[href]'),im=c.querySelector('.poster img');
            if(!a)return null;var u=new URL(a.getAttribute('href'),location.origin);
            if(u.origin!==location.origin || !/^\/(movies|tvshows)\/[^/]+\/?$/.test(u.pathname) || /\/tv-channels-online\/?$/.test(u.pathname))return null;
            if(seen[u.href])return null;seen[u.href]=true;
            var poster=im?(im.getAttribute('data-src')||im.getAttribute('src')||''):'';
            return {title:a.textContent.trim().slice(0,140),kind:u.pathname.indexOf('/movies/')===0?'MOVIE':'SERIES',
              contentUrl:u.href,posterUrl:/^https:\/\//i.test(poster)?poster:'',serviceId:'farsiland'};
          }).filter(Boolean);
        }
    """.trimIndent()

    fun catalog(pageLimit:Int,pageStart:Int=1):String = """
        (async function(){try{
          $cardParser
          async function pages(first){var output=[],url=location.origin+first+(${pageStart}>1?'page/'+${pageStart}+'/':''),more=false;
            for(var i=${pageStart};i<=${pageLimit} && url;i++){
              var r=await fetch(url,{credentials:'same-origin'});if(!r.ok)throw new Error('catalog');
              var d=new DOMParser().parseFromString(await r.text(),'text/html'),rows=cards(d);
              output=output.concat(rows);var links=Array.from(d.querySelectorAll('.pagination a[href],a.next[href]'));
              var current=new URL(url),n=links.map(function(a){return {a:a,u:new URL(a.getAttribute('href'),location.origin)};})
                .find(function(x){var m=x.u.pathname.match(/\/page\/(\d+)\//),prev=current.pathname.match(/\/page\/(\d+)\//);
                  return x.u.origin===location.origin && m && Number(m[1])===(prev?Number(prev[1]):1)+1;});
              url=n?n.u.href:'';more=!!url;if(!rows.length)break;
            }return {items:output,more:more};
          }
          // Two public archive requests at most, never a whole-archive crawl.
          var recent=await Promise.all([pages('/iran-movie-2025/'),pages('/series-26/')]);
          var older=await Promise.all([pages('/old-iranian-movies/'),pages('/iranian-series/')]);
          var movies=recent[0],series=recent[1],oldMovies=older[0],oldSeries=older[1];
          movies.items=movies.items.concat(oldMovies.items);movies.more=movies.more||oldMovies.more;
          series.items=series.items.concat(oldSeries.items);series.more=series.more||oldSeries.more;
          if(!movies.items.length && !series.items.length)throw new Error('empty');
          AminCatalog.section('farsiland',JSON.stringify({movies:movies.items,series:series.items,
            all:movies.items.concat(series.items),hasMoreMovies:movies.more,hasMoreSeries:series.more,
            hasMoreAll:movies.more||series.more}));
        }catch(_){AminCatalog.failed('farsiland','دریافت آرشیو انجام نشد');}})();
    """.trimIndent()

    fun search(queryJson:String):String = """
        (async function(){try{
          $cardParser
          var r=await fetch(location.origin+'/?s='+encodeURIComponent($queryJson),{credentials:'same-origin'});
          if(!r.ok)throw new Error('search');var d=new DOMParser().parseFromString(await r.text(),'text/html');
          var items=Array.from(d.querySelectorAll('.result-item article')).map(function(c){
            var a=c.querySelector('.details .title a[href]'),im=c.querySelector('.image img');if(!a)return null;
            var u=new URL(a.getAttribute('href'),location.origin);
            if(u.origin!==location.origin || !/^\/(movies|tvshows)\/[^/]+\/?$/.test(u.pathname) || /tv-channels-online/.test(u.pathname))return null;
            return {title:a.textContent.trim().slice(0,140),kind:u.pathname.indexOf('/movies/')===0?'MOVIE':'SERIES',
              contentUrl:u.href,posterUrl:im?(im.getAttribute('data-src')||im.getAttribute('src')||''):'',serviceId:'farsiland'};
          }).filter(Boolean);
          AminSearch.results('farsiland',JSON.stringify(items.slice(0,24)));
        }catch(_){AminSearch.failed('farsiland','جستجو انجام نشد');}})();
    """.trimIndent()

    val episodes = """
        (async function(){
          if(location.hostname.replace(/^www\./,'')!=='farsiland.com' || window.__aminEpisodeExtracting)return;
          window.__aminEpisodeExtracting=true;
          try{
          async function page(url){var abort=new AbortController(),timer=setTimeout(function(){abort.abort();},8000);
            try{var response=await fetch(url,{credentials:'same-origin',signal:abort.signal});if(!response.ok)throw new Error('page');return new DOMParser().parseFromString(await response.text(),'text/html');}finally{clearTimeout(timer);}}
          // Public WordPress episode rows already exist in HTML. The native loader
          // need not execute the page's ad/player scripts to read that metadata.
          var mainDoc=window.__aminPublicEpisodeHost?await page(location.origin+location.pathname):document;
          var seasons={};
          function addRows(doc){Array.from(doc.querySelectorAll('.episodiotitle')).slice(0,500).map(function(e){return e.closest('li');}).filter(Boolean).forEach(function(row){
            var a=row.querySelector('.episodiotitle a[href]'),num=row.querySelector('.numerando');if(!a||!num)return;
            var m=num.textContent.trim().match(/^(\d+)\s*-\s*(\d+)$/);if(!m)return;
            var u=new URL(a.getAttribute('href'),location.origin);
            if(u.origin!==location.origin || !/^\/episodes\/[^/]+\/?$/.test(u.pathname))return;
            if(!seasons[m[1]])seasons[m[1]]={id:m[1],name:'فصل '+m[1],episodes:[]};
            if(seasons[m[1]].episodes.some(function(e){return e.id===m[2];}))return;
            seasons[m[1]].episodes.push({id:m[2],title:'قسمت '+m[2],actionPayload:u.href,
              isAvailableOnline:!(/Soon|بزودی|به زودی/i.test(row.textContent)),isWatched:false});
          });}
          addRows(mainDoc);
          if(!Object.keys(seasons).length && !window.__aminEpisodeFallbackTried){
            window.__aminEpisodeFallbackTried=true;
            function normalized(s){return s.toLowerCase().replace(/[^a-z0-9\u0600-\u06ff]+/g,' ').trim();}
            var heading=mainDoc.querySelector('h1'),title=normalized(heading?heading.textContent:''),seriesPath=location.pathname.replace(/\/$/,'');
            if(title){
              var home=await page(location.origin+'/home');
              var candidates=Array.from(home.querySelectorAll('h3 a[href]')).filter(function(a){
                var url=new URL(a.getAttribute('href'),location.origin);
                return url.origin===location.origin && /^\/episodes\/[^/]+\/?$/.test(url.pathname) &&
                  normalized(a.textContent.replace(/\s+(?:SE\d+\s*)?EP\d+\s*$/i,''))===title;
              }).slice(0,2);
              for(var i=0;i<candidates.length && !Object.keys(seasons).length;i++){
                var episodeDoc=await page(new URL(candidates[i].getAttribute('href'),location.origin).href);
                var parentMatches=Array.from(episodeDoc.querySelectorAll('.pag_episodes a[href]')).some(function(a){var u=new URL(a.getAttribute('href'),location.origin);return u.origin===location.origin && u.pathname.replace(/\/$/,'')===seriesPath;});
                if(parentMatches)addRows(episodeDoc);
              }
            }
          }
          var list=Object.keys(seasons).sort(function(a,b){return Number(a)-Number(b);}).map(function(k){return seasons[k];});
          AminEpisode.result(JSON.stringify({editions:[{id:'farsiland',label:'پخش سایت',language:'',resolution:'',isDefault:true,seasons:list}]}));
        }catch(_){AminEpisode.result('null');}finally{window.__aminEpisodeExtracting=false;}})();
    """.trimIndent()
}
