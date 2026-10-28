package com.amin.tvos.browser

/** Selectors characterized on the public website on 2026-10-15. No media attributes read. */
object FarsiLandScripts {
    /** Trusted, inert origin host for public catalogue fetches; no site/ad scripts or artwork. */
    const val catalogHost = "<!doctype html><html><head><meta charset=\"utf-8\"><title>Aminema catalog</title></head><body></body></html>"
    // The .btn-row wrapper was removed on the real intermediary (2026-10-28).
    val entryQualityNodes = """Array.from(document.querySelectorAll('a.btn-group')).filter(function(a){return /^(480p?|720p?|1080p?|2160p?|HD|Server ?1|Live on Youtube|Backup Server)$/i.test(a.textContent.trim()) && a.getClientRects().length>0 && getComputedStyle(a).visibility!=='hidden';})"""
    // Characterized on the site's smart-play quality controls; never read href/onclick.
    val playerQualityNodes = """Array.from(document.querySelectorAll('a,.plyr button[data-plyr="quality"]')).filter(function(a){if(a.matches('.plyr button[data-plyr="quality"]'))return /^(480|720|1080)$/.test(a.value);var p=a.querySelector('p');return p && /^(480|720|1080)$/.test(p.textContent.trim()) && a.getClientRects().length>0;})"""
    val qualityLabel = """function(a){return a.getAttribute('data-plyr')==='quality'?a.value:(a.querySelector('p')?a.querySelector('p').textContent.trim():a.textContent.trim());}"""
    // Both controls were verified on the live site. Record the active choice too, so
    // recovery never bounces back to the engine that already failed on this title.
    val alternatePlayerNodes = """Array.from(document.querySelectorAll('a')).filter(function(a){var p=a.querySelector('p');return p && /^Player [12]$/.test(p.textContent.trim()) && a.getClientRects().length>0 && getComputedStyle(a).visibility!=='hidden';}).sort(function(a,b){return a.textContent.trim().localeCompare(b.textContent.trim());})"""
    /** Verified IMA request failure contract on FarsiPlex's current ordinary player pages.
     * Never report a successful ad, call resumeContent, read tags/media, or alter access gates.
     * Let the site's own onFail waterfall complete instead of waiting 5s per blocked ad tag.
     */
    val adRequestFilter = """
        (function(){
          if(!/^(www\.)?farsiplex\.com$/.test(location.hostname) || !/^\/play-[a-z-]+\/?$/.test(location.pathname))return;
          if(/Unlock more content|Take action to continue accessing/i.test(document.body?document.body.innerText:''))return;
          if(window.__aminImaFilter)return;
          if(typeof window.requestImaAd!=='function' || typeof window.playCriticalAd!=='function' || typeof window.resumeContent!=='function')return;
          var code=Function.prototype.toString.call(window.requestImaAd);
          if(code.indexOf('google.ima.AdsRequest')<0 || code.indexOf('adsLoader.requestAds')<0 || code.indexOf('onFail')<0)return;
          var state={suspended:false,pending:[]};window.__aminImaFilter=state;
          state.flush=function(){
            if(state.suspended || window.__aminUserPaused)return;
            var tasks=state.pending.splice(0,32);tasks.forEach(function(task){task();});
          };
          window.requestImaAd=function(_ignoredTag,_ignoredSuccess,onFail){
            if(typeof onFail!=='function')return;
            // Asynchronous failure, not a fabricated impression or completed advertisement.
            setTimeout(function(){
              if(state.suspended || window.__aminUserPaused){if(state.pending.length<32)state.pending.push(onFail);return;}
              onFail();
            },0);
          };
          document.addEventListener('click',function(e){
            if(!e.isTrusted || !e.target.closest || !e.target.closest('.jw-icon-playback,.jw-icon-display,.plyr__control[data-plyr="play"]'))return;
            var v=document.querySelector('#player video,video#player')||document.querySelector('.jwplayer video,.plyr video');
            if(v){window.__aminUserPaused=!v.paused;if(!window.__aminUserPaused)state.flush();}
          },true);
        })();
    """.trimIndent()
    val suspendPlayback = """(function(){if(window.__aminImaFilter)window.__aminImaFilter.suspended=true;document.querySelectorAll('video').forEach(function(v){v.pause();});})()"""
    val resumeObservation = """(function(){if(window.__aminImaFilter){window.__aminImaFilter.suspended=false;window.__aminImaFilter.flush();}})()"""
    val togglePlayback = """(function(){var v=document.querySelector('#player video,video#player')||document.querySelector('.jwplayer video,.plyr video');if(v){window.__aminUserPaused=!v.paused;if(v.paused){v.play();if(window.__aminImaFilter)window.__aminImaFilter.flush();}else v.pause();}})()"""
    val stalledQualitySnapshot = """
        (function(){
          var gate=/Unlock more content|Take action to continue accessing/i.test(document.body?document.body.innerText:'');
          var valid=/^(www\.)?farsiplex\.com$/.test(location.hostname)&&/^\/play-[a-z-]+\/?$/.test(location.pathname);
          var v=valid&&!gate?(document.querySelector('#player video,video#player')||document.querySelector('.plyr video')):null;
          return JSON.stringify({gate:gate||!valid,accessGate:gate,userPaused:!!window.__aminUserPaused,seeking:!!(v&&v.seeking),ready:v?v.readyState:-1,time:v&&isFinite(v.currentTime)?v.currentTime:-1,choices:!valid||gate?[]:Array.from(document.querySelectorAll('.plyr button[data-plyr="quality"]')).filter(function(b){return /^(480|720|1080)$/.test(b.value);}).map(function(b){return {label:b.value,selected:b.getAttribute('aria-checked')==='true'};})});
        })();
    """.trimIndent()
    fun selectStalledQuality(label:String):String {
        require(label in setOf("480","720","1080"))
        return """(function(){if(!/^(www\.)?farsiplex\.com$/.test(location.hostname)||!/^\/play-[a-z-]+\/?$/.test(location.pathname)||window.__aminUserPaused||(window.__aminImaFilter&&window.__aminImaFilter.suspended)||/Unlock more content|Take action to continue accessing/i.test(document.body?document.body.innerText:''))return;var b=Array.from(document.querySelectorAll('.plyr button[data-plyr="quality"]')).find(function(e){return e.value==='$label'&&e.getAttribute('aria-checked')!=='true';});if(b)b.click();})()"""
    }
    /** Retry a stuck ordinary seek slightly earlier, never skip unseen content forward. */
    val retryStalledSeek = """
        (function(){
          if(!/^(www\.)?farsiplex\.com$/.test(location.hostname)||!/^\/play-[a-z-]+\/?$/.test(location.pathname)||window.__aminUserPaused||(window.__aminImaFilter&&window.__aminImaFilter.suspended)||/Unlock more content|Take action to continue accessing/i.test(document.body?document.body.innerText:''))return false;
          var v=document.querySelector('#player video,video#player')||document.querySelector('.plyr video');
          if(!v||v.paused||!v.seeking||v.readyState>=2||!isFinite(v.currentTime)||v.currentTime<3)return false;
          v.currentTime=Math.max(0,Math.floor(v.currentTime)-3);return true;
        })();
    """.trimIndent()
    val playback = """
        (function(){
          function visible(e){return e && e.getClientRects().length>0 && getComputedStyle(e).visibility!=='hidden';}
          function notePlayError(e){var name=e&&e.name;window.__aminPlayError=/^(NotAllowedError|AbortError|NotSupportedError|InvalidStateError)$/.test(name)?name:'Other';}
          var text=document.body?document.body.innerText:'';
          if(/Unlock more content|Take action to continue accessing/i.test(text))return JSON.stringify({state:'gate'});
          $adRequestFilter
          var v=document.querySelector('#player video,video#player')||document.querySelector('.jwplayer video,.plyr video');
          if(v){
            if(v.error)return JSON.stringify({state:'player-error',code:v.error.code});
            var userPaused=!!window.__aminUserPaused;
            // A provider quality/player swap must not cancel the user's explicit Pause.
            if(userPaused && !v.paused)v.pause();
            if(location.hostname.replace(/^www\./,'')==='farsiplex.com' && !window.__aminInitialQualityChosen){
              var quality=Array.from(document.querySelectorAll('.plyr button[data-plyr="quality"]')).filter(function(b){return /^(480|720|1080)$/.test(b.value);});
              if(quality.length){
                window.__aminInitialQualityChosen=true;
                var preferred=['1080','720','480'].find(function(label){return quality.some(function(b){return b.value===label;});});
                var selected=quality.find(function(b){return b.value===preferred && b.getAttribute('aria-checked')==='true';});
                if(!selected)return JSON.stringify({state:'player-quality',labels:quality.map(function(b){return b.value;})});
              }
            }
            if(window.__aminVideo!==v){window.__aminVideo=v;window.__aminEverPlayed=false;window.__aminStartTries=0;window.__aminLastStart=0;window.__aminPlayError='';window.__aminPlayClicked=false;if(!userPaused){var p=v.play();if(p && p.catch)p.catch(notePlayError);}}
            if(!userPaused && !window.__aminEverPlayed && v.readyState>=1 && v.currentTime===0 && v.paused && window.__aminStartTries<3 && Date.now()-window.__aminLastStart>1500){
              window.__aminStartTries++;window.__aminLastStart=Date.now();var readyPlay=v.play();if(readyPlay && readyPlay.catch)readyPlay.catch(notePlayError);
            }
            if(v.readyState>=1){
              var box=v.closest('.jwplayer,.plyr')||v;
              // Keep the media/player attached to its original DOM parent. Reparenting
              // can interrupt Chromium playback/seeks; fixed CSS provides theater layout.
              if(!document.getElementById('aminema-theater-style')){
                var st=document.createElement('style');st.id='aminema-theater-style';
                st.textContent='html,body{background:#000!important;overflow:hidden!important;}'+
                  '.aminema-theater-ancestor{backdrop-filter:none!important;-webkit-backdrop-filter:none!important;}'+
                  '.aminema-theater{position:fixed!important;top:0!important;right:0!important;bottom:0!important;left:0!important;width:100vw!important;height:100vh!important;'+
                  'max-width:none!important;max-height:none!important;margin:0!important;z-index:1000!important;background:#000!important;}'+
                  '.aminema-theater video,video.aminema-theater{width:100%!important;height:100%!important;object-fit:contain!important;}';
                document.head.appendChild(st);
              }
              if(!box.classList.contains('aminema-theater'))box.classList.add('aminema-theater');
              // A backdrop-filter ancestor establishes its own fixed containing block.
              // Neutralize only the player's ancestors; never detach the media element.
              for(var parent=box.parentElement;parent&&parent!==document.body;parent=parent.parentElement){
                if(!parent.classList.contains('aminema-theater-ancestor'))parent.classList.add('aminema-theater-ancestor');
              }
            }
            if((v.readyState>=2 && (v.currentTime>0 || window.__aminEverPlayed)) || (userPaused && v.paused)){
              window.__aminEverPlayed=window.__aminEverPlayed || v.currentTime>0;
              return JSON.stringify({state:v.paused?'paused':'playing',paused:v.paused,time:Math.floor(v.currentTime),ready:v.readyState,userPaused:!!window.__aminUserPaused});
            }
            var play=document.querySelector('.jw-icon-playback,.plyr__control[data-plyr="play"],button[aria-label="Play"]');
            if(!userPaused && !window.__aminEverPlayed && v.paused && visible(play) && !window.__aminPlayClicked){window.__aminPlayClicked=true;play.click();}
            if(/This video file cannot be played|Error Code:/i.test(text))return JSON.stringify({state:'player-error'});
            return JSON.stringify({state:'buffering',ready:v.readyState,paused:v.paused,time:Math.floor(v.currentTime),playError:window.__aminPlayError||''});
          }
          if(/This video file cannot be played|Error Code:/i.test(text))return JSON.stringify({state:'player-error'});
          // Current JW creates its video element only after the visible Play control.
          // Scope to the main player; never click page-wide ads, links or access prompts.
          var initialPlay=Array.from(document.querySelectorAll('#player [role="button"],.jwplayer [role="button"]')).find(function(b){return visible(b) && /^play(?:\s|$)/i.test(b.getAttribute('aria-label')||'');});
          if(!window.__aminUserPaused && initialPlay && window.__aminInitialPlay!==initialPlay){window.__aminInitialPlay=initialPlay;initialPlay.click();return JSON.stringify({state:'opening-player'});}
          if((location.hostname.replace(/^www\./,'')==='farsiland.com' && /^\/(movies|episodes)\/[^/]+\/?$/.test(location.pathname)) ||
             (location.hostname.replace(/^www\./,'')==='farsiplex.com' && /^\/(movie|episode)\/[^/]+\/?$/.test(location.pathname))){
            var go=document.querySelector('.fakeplayer a[onclick]');
            if(visible(go) && !window.__aminEntryClicked){window.__aminEntryClicked=true;go.click();return JSON.stringify({state:'opening'});}
          }
          if((location.hostname.replace(/^www\./,'')==='farsiplex.com' && /^\/season\/?$/.test(location.pathname) && location.search) || location.hostname.replace(/^www\./,'')==='farsiland.buzz'){
            var choices=$entryQualityNodes;
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
