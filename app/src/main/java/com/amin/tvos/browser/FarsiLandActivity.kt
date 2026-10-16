package com.amin.tvos.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONObject

/** Isolated two-site browser. Uses the site's controls; no media URL/manifest/credential access. */
class FarsiLandActivity:ComponentActivity() {
    private lateinit var root:FrameLayout
    private lateinit var web:WebView
    private lateinit var loading:PlaybackLoadingView
    private lateinit var error:TvErrorView
    private val handler=Handler(Looper.getMainLooper())
    private val asyncGuard=FarsiLandAsyncGuard()
    private val children=mutableSetOf<WebView>()
    private var startedAt=0L
    private var flowStartedAt=0L
    private var document=0
    private var inFlight=false
    private var inFlightAt=0L
    private var preparing=false
    private var stopped=false
    private var playing=false
    private var chosenDocument=-1
    private val triedQualities=mutableSetOf<String>()
    private var recovering=false
    private val triedPlayers=mutableSetOf<String>()
    private var ticks=0
    private var custom:View?=null
    private var customCallback:WebChromeClient.CustomViewCallback?=null
    private var seed=""
    private var live=false
    private val tick=object:Runnable { override fun run(){
        if(stopped || isFinishing || isDestroyed)return
        if(preparing && !playing && (SystemClock.elapsedRealtime()-startedAt>45_000 || SystemClock.elapsedRealtime()-flowStartedAt>120_000)){
            recoverOrFail("پخش در مهلت مقرر آماده نشد؛ برای تلاش دوباره یا برگشت از کنترل استفاده کن.");return
        }
        if(inFlight && SystemClock.elapsedRealtime()-inFlightAt>3000L){inFlight=false;asyncGuard.expireInspection()}
        if(!inFlight && !recovering && FarsiLandPolicy.allowedPage(web.url.orEmpty()))inspect()
        handler.postDelayed(this,if(playing)1500L else 550L)
    } }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        hideBars()
        val ep=intent.getStringExtra("sm_episode").orEmpty()
        seed=if(FarsiLandPolicy.isEpisode(ep))ep else intent.getStringExtra("url").orEmpty()
        if(seed.trimEnd('/')=="https://farsiland.com")seed="https://farsiland.com/home"
        live=intent.getBooleanExtra("live_theater_mode",false)
        if(!FarsiLandPolicy.allowedPage(seed)){finish();return}
        root=FrameLayout(this).apply {setBackgroundColor(Color.BLACK)}
        web=KeyboardSafeWebView(this).apply {
            setBackgroundColor(Color.BLACK);isFocusable=true;isFocusableInTouchMode=true
            settings.apply {
                javaScriptEnabled=true;domStorageEnabled=true
                allowFileAccess=false;allowContentAccess=false
                mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture=false
                javaScriptCanOpenWindowsAutomatically=true;setSupportMultipleWindows(true)
                userAgentString=com.amin.tvos.data.model.UserAgentMode.DESKTOP.value
            }
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,true)
        root.addView(web,FrameLayout.LayoutParams(-1,-1))
        loading=PlaybackLoadingView(this);root.addView(loading,FrameLayout.LayoutParams(-1,-1))
        error=TvErrorView(this){begin(seed)}.apply {visibility=View.GONE}
        root.addView(error,FrameLayout.LayoutParams(-1,-1));setContentView(root)
        web.setDownloadListener { _,_,_,_,_ -> fail("دانلود باز نشد؛ فقط پخش داخل سایت پشتیبانی می‌شود.") }
        web.webViewClient=object:WebViewClient(){
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean =
                request.isForMainFrame && !FarsiLandPolicy.allowedPage(request.url.toString())
            @Deprecated("Legacy WebView callback")
            override fun shouldOverrideUrlLoading(view:WebView,url:String)=!FarsiLandPolicy.allowedPage(url)
            override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse? =
                if(FarsiLandPolicy.blockedResourceHost(request.url.host))
                    WebResourceResponse("text/plain","UTF-8",java.io.ByteArrayInputStream(ByteArray(0))) else null
            override fun onPageStarted(view:WebView,url:String,favicon:Bitmap?){
                if(!FarsiLandPolicy.allowedPage(url)){
                    if(com.amin.tvos.BuildConfig.DEBUG){
                        val u=android.net.Uri.parse(url)
                        val route=u.pathSegments.firstOrNull()?.takeIf {it.matches(Regex("[A-Za-z-]{1,30}(\\.php)?"))}?:"other"
                        android.util.Log.d("AminFarsiLand","blocked-navigation scheme=${u.scheme} host=${u.host} route=$route")
                    }
                    view.stopLoading();return
                }
                asyncGuard.invalidate();recovering=false
                document++;chosenDocument=-1;inFlight=false;playing=false
                if(FarsiLandPolicy.isEpisode(url) || (FarsiLandPolicy.isTitle(url) && url.contains("/movies/")))flowStartedAt=SystemClock.elapsedRealtime()
                if(isPlayablePage(url))startedAt=SystemClock.elapsedRealtime()
                if(isPlayablePage(url)){preparing=true;loading.showPreparing(false,titleText=intent.getStringExtra("content_title").orEmpty())}
                else loading.hide()
            }
            override fun onPageFinished(view:WebView,url:String){CookieManager.getInstance().flush()}
            override fun onReceivedError(view:WebView,request:WebResourceRequest,e:WebResourceError){
                if(request.isForMainFrame)fail("اتصال به سایت برقرار نشد؛ اینترنت و DNS دستگاه را بررسی کن.")
            }
            override fun onReceivedSslError(view:WebView,h:SslErrorHandler,e:SslError){h.cancel();fail("گواهی اتصال امن سایت معتبر نیست.")}
        }
        web.webChromeClient=object:WebChromeClient(){
            override fun onPermissionRequest(request:PermissionRequest){request.deny()}
            override fun onGeolocationPermissionsShowPrompt(origin:String,callback:GeolocationPermissions.Callback){callback.invoke(origin,false,false)}
            override fun onCreateWindow(view:WebView,isDialog:Boolean,isUserGesture:Boolean,resultMsg:Message):Boolean {
                // Observe only the attempted ordinary navigation. Ad windows never become visible.
                if(children.size>=2)return false
                val child=WebView(this@FarsiLandActivity);children.add(child)
                fun release(){if(children.remove(child))child.destroy()}
                child.webViewClient=object:WebViewClient(){
                    override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean {
                        val target=r.url.toString()
                        if(FarsiLandPolicy.allowedPage(target))web.loadUrl(target)
                        handler.post {release()};return true
                    }
                    override fun onPageStarted(v:WebView,url:String,icon:Bitmap?){
                        if(url!="about:blank"){
                            if(FarsiLandPolicy.allowedPage(url))web.loadUrl(url)
                            v.stopLoading();handler.post {release()}
                        }
                    }
                }
                (resultMsg.obj as WebView.WebViewTransport).webView=child;resultMsg.sendToTarget()
                handler.postDelayed({release()},2500L);return true
            }
            override fun onShowCustomView(view:View,callback:CustomViewCallback){
                if(custom!=null){callback.onCustomViewHidden();return}
                custom=view;customCallback=callback;root.addView(view,FrameLayout.LayoutParams(-1,-1));hideBars()
            }
            override fun onHideCustomView(){closeCustom()}
        }
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){finish()}})
        begin(seed)
    }
    private fun isPlayablePage(url:String)=runCatching {
        val p=java.net.URI(url).path.orEmpty()
        p.startsWith("/movies/")||p.startsWith("/episodes/")||p.startsWith("/season/")||p.startsWith("/smart-play")
    }.getOrDefault(false)
    private fun begin(url:String){
        asyncGuard.invalidate()
        stopped=false;playing=false;preparing=isPlayablePage(url);inFlight=false;chosenDocument=-1;startedAt=SystemClock.elapsedRealtime();flowStartedAt=startedAt;ticks=0
        triedQualities.clear();recovering=false;triedPlayers.clear()
        error.visibility=View.GONE;handler.removeCallbacks(tick);web.loadUrl(url);handler.post(tick)
    }
    private fun inspect(){
        val request=asyncGuard.beginInspection();inFlight=true;inFlightAt=SystemClock.elapsedRealtime()
        web.evaluateJavascript(FarsiLandScripts.playback){encoded ->
            if(!asyncGuard.currentInspection(request) || stopped || isFinishing || isDestroyed)return@evaluateJavascript
            asyncGuard.finishInspection(request)
            inFlight=false
            val data=runCatching {JSONObject(org.json.JSONTokener(encoded).nextValue() as String)}.getOrNull()
            val phase=data?.optString("state").orEmpty()
            if(com.amin.tvos.BuildConfig.DEBUG && ticks++%8==0)android.util.Log.d("AminFarsiLand","phase=$phase ready=${data?.optInt("ready",-1)} paused=${data?.optBoolean("paused")} time=${data?.optInt("time",-1)} playError=${data?.optString("playError")}")
            when(phase){
                "playing", "paused" -> {playing=true;loading.hide();error.visibility=View.GONE}
                "gate" -> {loading.hide();playing=false;stopped=true;handler.removeCallbacks(tick)
                    android.widget.Toast.makeText(this,"سایت برای دسترسی اقدام تبلیغاتی می‌خواهد؛ پخش بدون تبلیغ تأیید نشد.",android.widget.Toast.LENGTH_LONG).show()}
                "quality" -> if(chosenDocument!=document){
                    val array=data?.optJSONArray("labels")
                    val labels=(0 until (array?.length()?:0)).map {array!!.optString(it)}
                    val index=FarsiLandPolicy.qualityIndex(labels,live)
                    if(index!=null){chosenDocument=document;triedQualities.add(labels[index].trim())
                        web.evaluateJavascript("(function(){var a=Array.from(document.querySelectorAll('.btn-row a.btn-group')).filter(function(e){return e.getClientRects().length>0 && getComputedStyle(e).visibility!=='hidden';});if(a[$index])a[$index].click();})()",null)}
                    else if(labels.isNotEmpty())fail("کیفیت مجاز پیدا نشد؛ گزینهٔ ۲۱۶۰ خودکار انتخاب نمی‌شود.")
                }
                "player-error" -> recoverOrFail("پلیر سایت خطا داد؛ دوباره تلاش کن یا سرور دیگری را در سایت انتخاب کن.")
            }
            if(!playing && !stopped && isPlayablePage(web.url.orEmpty()) &&
                SystemClock.elapsedRealtime()-startedAt>45_000)recoverOrFail("پخش در مهلت مقرر آماده نشد.")
        }
    }
    private fun recoverOrFail(message:String){
        if(recovering)return
        if(live || playing || triedQualities.size>=3 || SystemClock.elapsedRealtime()-flowStartedAt>=120_000){fail(message);return}
        recovering=true
        inFlight=false
        val request=asyncGuard.beginRecovery()
        val nodes=FarsiLandScripts.playerQualityNodes
        val players=FarsiLandScripts.alternatePlayerNodes
        web.evaluateJavascript("JSON.stringify({gate:/Unlock more content|Take action to continue accessing/i.test(document.body?document.body.innerText:''),players:$players.map(function(a){return {label:a.querySelector('p').textContent.trim(),active:a.classList.contains('nonex')};}),labels:$nodes.map(function(a){return a.querySelector('p').textContent.trim();})})"){encoded ->
            if(!asyncGuard.currentRecovery(request) || stopped || isFinishing || isDestroyed)return@evaluateJavascript
            asyncGuard.finishRecovery(request)
            recovering=false
            val data=runCatching {JSONObject(org.json.JSONTokener(encoded).nextValue() as String)}.getOrNull()
            if(data?.optBoolean("gate")==true){
                stopped=true;handler.removeCallbacks(tick);loading.hide()
                android.widget.Toast.makeText(this,"سایت شرط دسترسی تبلیغاتی دارد؛ پخش بدون تبلیغ تأیید نشد.",android.widget.Toast.LENGTH_LONG).show()
                return@evaluateJavascript
            }
            val playerArray=data?.optJSONArray("players")
            val choices=(0 until (playerArray?.length()?:0)).mapNotNull { playerArray!!.optJSONObject(it) }
            choices.filter {it.optBoolean("active")}.forEach {triedPlayers.add(it.optString("label"))}
            val alternative=FarsiLandPolicy.alternatePlayer(
                choices.filterNot {it.optBoolean("active")}.map {it.optString("label")},triedPlayers)
            if(alternative!=null){
                triedPlayers.add(alternative);startedAt=SystemClock.elapsedRealtime()
                if(com.amin.tvos.BuildConfig.DEBUG)android.util.Log.d("AminFarsiLand","fallback-player=$alternative")
                val label=JSONObject.quote(alternative)
                web.evaluateJavascript("(function(){var a=$players.find(function(e){return e.querySelector('p').textContent.trim()===$label && !e.classList.contains('nonex');});if(a)a.click();})()",null)
                handler.removeCallbacks(tick);handler.post(tick)
                return@evaluateJavascript
            }
            val array=data?.optJSONArray("labels")
            val labels=(0 until (array?.length()?:0)).map {array!!.optString(it)}
            val available=labels.indices.filter {labels[it] !in triedQualities}
            val choice=FarsiLandPolicy.qualityIndex(available.map {labels[it]})
            if(choice==null){fail(message);return@evaluateJavascript}
            val index=available[choice];triedQualities.add(labels[index]);startedAt=SystemClock.elapsedRealtime()
            if(com.amin.tvos.BuildConfig.DEBUG)android.util.Log.d("AminFarsiLand","fallback-quality=${labels[index]}")
            web.evaluateJavascript("(function(){var a=$nodes;if(a[$index])a[$index].click();})()",null)
            handler.removeCallbacks(tick);handler.post(tick)
        }
        handler.postDelayed({if(asyncGuard.currentRecovery(request) && recovering && !stopped){
            asyncGuard.finishRecovery(request);recovering=false;fail(message)
        }},3500L)
    }
    private fun fail(message:String){
        asyncGuard.invalidate();recovering=false;inFlight=false
        stopped=true;playing=false;handler.removeCallbacks(tick);loading.hide()
        web.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause();});",null)
        error.show("پخش آماده نشد",message);error.bringToFront()
    }
    private fun hideBars(){WindowInsetsControllerCompat(window,window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars());systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }}
    private fun closeCustom(){custom?.let {root.removeView(it)};custom=null;customCallback?.onCustomViewHidden();customCallback=null}
    @SuppressLint("RestrictedApi") // Same ComponentActivity key-dispatch contract as Main/Browser.
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        // WebView/HTML focus may move while the provider swaps its player. The TV
        // transport key belongs to the visible player, not a particular DOM focus target.
        if(!stopped && ::error.isInitialized && error.visibility!=View.VISIBLE && handlePlaybackKey(event))return true
        return super.dispatchKeyEvent(event)
    }
    private fun handlePlaybackKey(event:KeyEvent):Boolean {
        if(event.keyCode in setOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,KeyEvent.KEYCODE_NUMPAD_ENTER) && playing){
            if(event.action==KeyEvent.ACTION_UP)web.evaluateJavascript("(function(){var v=document.querySelector('#player video,video#player')||document.querySelector('.jwplayer video,.plyr video');if(v){if(v.paused)v.play();else v.pause();}})()",null)
            return true
        }
        return false
    }
    override fun onPause(){
        asyncGuard.invalidate()
        stopped=true;recovering=false;inFlight=false;handler.removeCallbacksAndMessages(null)
        children.toList().forEach {it.destroy()};children.clear()
        if(::web.isInitialized){web.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause();});",null);web.onPause()}
        super.onPause()
    }
    override fun onResume(){super.onResume();if(::web.isInitialized){web.onResume();stopped=false;startedAt=SystemClock.elapsedRealtime();if(!playing)flowStartedAt=startedAt;handler.removeCallbacks(tick);handler.post(tick)}}
    override fun onDestroy(){asyncGuard.invalidate();stopped=true;handler.removeCallbacksAndMessages(null);closeCustom()
        children.toList().forEach {it.destroy()};children.clear()
        if(::web.isInitialized){web.stopLoading();root.removeView(web);web.destroy()};super.onDestroy()}
}
