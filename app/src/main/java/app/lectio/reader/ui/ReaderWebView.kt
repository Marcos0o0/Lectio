package app.lectio.reader.ui

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.viewinterop.AndroidView
import app.lectio.reader.data.*
import app.lectio.reader.data.Annotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLConnection

private class ReaderBridge(val selection: (Selection) -> Unit,val progress: (Float) -> Unit) {
    @JavascriptInterface fun selected(text: String,context: String) { if(text.isNotBlank()&&text.length<=6000) selection(Selection(text,context.take(3000))) }
    @JavascriptInterface fun position(value: Float) {if(value.isFinite()) progress(value.coerceIn(0f,1f))}
}
private class PageState {var loaded="";var ready=false;var navigationId=-1}

/** Let WebView load its internal documents. Block external requests before looking
 * up a MIME type: Android's FileNameMap throws when the filename is null. */
internal fun readerResourceResponse(context: Context, book: File, uri: Uri): WebResourceResponse? {
    fun empty()=WebResourceResponse("application/octet-stream",null,ByteArrayInputStream(byteArrayOf()))
    if(uri.scheme=="data"||uri.toString()=="about:blank") return null
    if(uri.scheme!="https"||uri.host!="lectio.local") return empty()
    if(uri.path=="/app-font/literata.ttf") {
        return runCatching {WebResourceResponse("font/ttf",null,context.assets.open("fonts/literata.ttf"))}.getOrElse {empty()}
    }
    val encodedPath=uri.encodedPath ?: return empty()
    if(!encodedPath.startsWith("/book/")||uri.lastPathSegment.isNullOrEmpty()) return empty()
    val path=runCatching {Epub.resolve("",encodedPath.removePrefix("/book/"))}.getOrNull() ?: return empty()
    // Only passive book resources may be served. HTML navigation is handled separately.
    if(path.endsWith(".js",true)||path.endsWith(".html",true)||path.endsWith(".xhtml",true)||path.endsWith(".svg",true)) return empty()
    val bytes=runCatching {Epub.resource(book,path)}.getOrNull() ?: return empty()
    val mime=URLConnection.guessContentTypeFromName(uri.lastPathSegment.orEmpty()) ?: "application/octet-stream"
    return WebResourceResponse(mime,null,ByteArrayInputStream(bytes))
}

@SuppressLint("SetJavaScriptEnabled")
@Composable fun ReaderWebView(book: Book, preferences: Preferences, background: Color, foreground: Color,
    annotations: List<Annotation>, search: String, searchOccurrence: Int, navigationId: Int, fragment: String?, modifier: Modifier,
    onReady: (WebView?) -> Unit, onSelect: (Selection) -> Unit, onProgress: (Float) -> Unit, onChapter: (Int,String?) -> Unit, onNavigationConsumed: () -> Unit) {
    val currentSelect by rememberUpdatedState(onSelect)
    val currentProgress by rememberUpdatedState(onProgress)
    val currentChapter by rememberUpdatedState(onChapter)
    val ready by rememberUpdatedState(onReady)
    val consumeNavigation by rememberUpdatedState(onNavigationConsumed)
    val chapter=book.chapters[book.chapter]
    val highlights=annotations.filter {it.chapter==book.chapter&&it.type=="highlight"}.map {it.text}
    val restore by rememberUpdatedState(book.position)
    val targetFragment by rememberUpdatedState(fragment)
    val occurrence by rememberUpdatedState(searchOccurrence)
    val currentSearch by rememberUpdatedState(search)
    val page=remember {PageState()}
    val bg="#%06X".format(0xFFFFFF and background.toArgb())
    val fg="#%06X".format(0xFFFFFF and foreground.toArgb())
    val scheme=readingColors(preferences)
    fun Color.css()="#%06X".format(0xFFFFFF and toArgb())
    val quote=scheme.surfaceContainer.css()
    val selection=scheme.primaryContainer.css()
    val onSelection=scheme.onPrimaryContainer.css()
    val highlight=scheme.secondaryContainer.css()
    val onHighlight=scheme.onSecondaryContainer.css()
    val divider=scheme.outlineVariant.css()
    val dark=background.luminance()<.5f
    val html by produceState<String?>(null,book.id,book.chapter,preferences.fontSize,preferences.lineHeight,preferences.serif,preferences.margin,preferences.paragraphSpacing,preferences.justified,preferences.tapToTranslate,scheme,highlights) {
        value=withContext(Dispatchers.IO) {
            runCatching {
                val body=Epub.safeBody(Epub.resource(File(book.file),chapter.path))
                val nonce=java.util.UUID.randomUUID().toString()
                """<!doctype html><html lang="en"><head><meta name="viewport" content="width=device-width, initial-scale=1"><meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src https://lectio.local data:; style-src 'unsafe-inline'; script-src 'nonce-$nonce'; font-src https://lectio.local; connect-src 'none'; base-uri 'none'; form-action 'none'"><style>
                @font-face{font-family:Literata;src:url('https://lectio.local/app-font/literata.ttf') format('truetype');font-weight:100 900;font-display:swap}
                :root{color-scheme:${if(dark) "dark" else "light"}}
                *{box-sizing:border-box}html{background:$bg;color:$fg;-webkit-text-size-adjust:100%}body{margin:0 auto;padding:24px ${preferences.margin}px 70px;max-width:740px;font-family:${if(preferences.serif) "Literata, serif" else "sans-serif"};font-size:${preferences.fontSize}px;line-height:${preferences.lineHeight};overflow-wrap:break-word}h1{font-size:1.8em;line-height:1.2;font-weight:400;margin:.45em 0 1.1em}h2{display:flow-root;font-size:1.25em;line-height:1.35;margin:0 0 .7em;padding-top:1em}p{margin:0 0 ${preferences.paragraphSpacing}em;text-align:${if(preferences.justified) "justify" else "start"};hyphens:auto}img,svg{max-width:100%;height:auto}a{color:inherit;text-decoration-thickness:1px;text-underline-offset:3px}blockquote{margin:1em 0;padding:1em;background:$quote}::selection{background:$selection;color:$onSelection}mark{background:$highlight;color:$onHighlight;padding:2px 0}table{max-width:100%;display:block;overflow-x:auto}pre{white-space:pre-wrap}hr{border:0;border-top:1px solid $divider;margin:2em 0}
                </style></head><body>$body<script nonce="$nonce">
                window.lastLookup='';let timer,touching=false,touchStart=null;const marks=${JSONArray(highlights)};
                window.lectioPayload=function(){let s=getSelection(),text=s.toString().trim();if(!text)return null;let n=s.anchorNode,p=n?(n.nodeType===1?n:n.parentElement):null,b=p?p.closest('p,li,blockquote,div,section'):null;let context=b?b.innerText:(p?p.innerText:text);let sentences=context.match(/[^.!?]+[.!?]+|[^.!?]+$/g)||[context];context=sentences.find(x=>x.includes(text))||context;return {text:text,context:context.trim().slice(0,1200)}};
                function lookup(){let payload=window.lectioPayload();if(!payload||payload.text===window.lastLookup||touching)return;window.lastLookup=payload.text;Lectio.selected(payload.text,payload.context)}
                document.addEventListener('touchstart',e=>{touching=true;clearTimeout(timer);let t=e.touches[0];touchStart={x:t.clientX,y:t.clientY,time:Date.now(),moved:false}},{passive:true});
                document.addEventListener('touchmove',e=>{if(touchStart){let t=e.touches[0];if(Math.hypot(t.clientX-touchStart.x,t.clientY-touchStart.y)>10)touchStart.moved=true}},{passive:true});
                function selectWord(x,y){let r=document.caretRangeFromPoint(x,y);if(!r||r.startContainer.nodeType!==3)return;let n=r.startContainer,p=n.parentElement;if(!p||p.closest('a,button,script,style'))return;let i=r.startOffset,value=n.textContent;let segmenter=new Intl.Segmenter('en',{granularity:'word'});let word=Array.from(segmenter.segment(value)).find(w=>w.isWordLike&&i>=w.index&&i<w.index+w.segment.length);if(!word)return;r.setStart(n,word.index);r.setEnd(n,word.index+word.segment.length);let s=getSelection();s.removeAllRanges();s.addRange(r);lookup()}
                document.addEventListener('touchend',()=>{touching=false;clearTimeout(timer);if(${preferences.tapToTranslate}&&touchStart&&!touchStart.moved&&Date.now()-touchStart.time<350&&!getSelection().toString().trim())selectWord(touchStart.x,touchStart.y);touchStart=null;timer=setTimeout(lookup,750)},{passive:true});
                document.addEventListener('touchcancel',()=>{touching=false;touchStart=null;clearTimeout(timer);timer=setTimeout(lookup,900)},{passive:true});
                document.addEventListener('selectionchange',()=>{clearTimeout(timer);timer=setTimeout(lookup,850)});
                let scrollTimer;window.addEventListener('scroll',()=>{clearTimeout(scrollTimer);scrollTimer=setTimeout(()=>Lectio.position((innerHeight+scrollY>=document.documentElement.scrollHeight-6)?1:scrollY/Math.max(1,document.documentElement.scrollHeight-innerHeight)),350)},{passive:true});
                function highlight(text){if(!text)return;let walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT),nodes=[],full='';while(walker.nextNode()){let n=walker.currentNode;if(n.parentElement.closest('script,style,mark'))continue;nodes.push({n,start:full.length});full+=n.textContent}let i=full.indexOf(text);if(i<0)return;let start,end;nodes.forEach(x=>{if(x.start<=i)start=x;if(x.start<i+text.length)end=x});if(!start||!end)return;let range=document.createRange();range.setStart(start.n,i-start.start);range.setEnd(end.n,i+text.length-end.start);let mark=document.createElement('mark');mark.appendChild(range.extractContents());range.insertNode(mark)}marks.forEach(highlight);
                window.restore=function(r){const h=Math.max(0,document.documentElement.scrollHeight-innerHeight);scrollTo(0,h*r)};
                </script></body></html>"""
            }.getOrElse { """<html><meta name="viewport" content="width=device-width, initial-scale=1"><body style="background:$bg;color:$fg;font-family:sans-serif;padding:24px"><h1>No se pudo abrir este capítulo</h1><p>Vuelve a la biblioteca e importa el EPUB de nuevo.</p></body></html>""" }
        }
    }
    AndroidView(modifier=modifier,factory={ context ->
        SelectionWebView(context) {currentSelect(it)}.apply {
            keepScreenOn=preferences.keepScreenOn
            setBackgroundColor(background.toArgb())
            settings.apply {
                javaScriptEnabled=true;domStorageEnabled=false;allowFileAccess=false;allowContentAccess=false
                setSupportZoom(false);builtInZoomControls=false;displayZoomControls=false
                mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture=true
            }
            isVerticalScrollBarEnabled=false
            addJavascriptInterface(ReaderBridge({s->post { currentSelect(s) }},{p->post { currentProgress(p) }}),"Lectio")
            webViewClient=object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView,request: WebResourceRequest): WebResourceResponse? {
                    return readerResourceResponse(context,File(book.file),request.url)
                }
                override fun shouldOverrideUrlLoading(view: WebView,request: WebResourceRequest): Boolean {
                    val uri=request.url
                    if(uri.scheme=="https"&&uri.host=="lectio.local") {
                        val path=runCatching {Epub.resolve("",uri.encodedPath.orEmpty().removePrefix("/book/"))}.getOrNull()
                        val index=book.chapters.indexOfFirst {it.path==path}
                        if(index>=0) { if(index!=book.chapter) currentChapter(index,uri.fragment) else if(!uri.fragment.isNullOrBlank()) view.evaluateJavascript("var e=document.getElementById(${JSONObject.quote(uri.fragment)});if(e)e.scrollIntoView()",null) }
                    }
                    return true
                }
                override fun onPageFinished(view: WebView,url: String) {
                    page.ready=true
                    view.evaluateJavascript("setTimeout(()=>{if(window.restore)window.restore($restore);var e=document.getElementById(${JSONObject.quote(targetFragment.orEmpty())});if(e)e.scrollIntoView()},100)",null)
                    if(!targetFragment.isNullOrBlank()) consumeNavigation()
                    if(currentSearch.isNotBlank()) view.postDelayed({view.findAllAsync(currentSearch)},300)
                }
            }
            var searchMoved=false
            setFindListener { _, count, done ->
                if(done&&!searchMoved&&count>0) {searchMoved=true;repeat(occurrence.coerceAtMost(count-1)) {findNext(true)}}
            }
            ready(this)
        }
    },update={view ->
        ready(view)
        view.keepScreenOn=preferences.keepScreenOn
        view.setBackgroundColor(background.toArgb())
        view.settings.textZoom=(view.context.resources.configuration.fontScale*100).toInt()
        val content=html
        if(content!=null&&page.loaded!=content) {page.loaded=content;page.ready=false;view.loadDataWithBaseURL("https://lectio.local/book/${chapter.path}",content,"text/html","UTF-8",null)}
        if(page.navigationId!=navigationId) {
            page.navigationId=navigationId
            if(page.ready) view.evaluateJavascript("if(window.restore)window.restore($restore);var e=document.getElementById(${JSONObject.quote(fragment.orEmpty())});if(e)e.scrollIntoView()",null)
            if(page.ready&&!fragment.isNullOrBlank()) consumeNavigation()
        }
    },onRelease={view->ready(null);view.keepScreenOn=false;view.removeJavascriptInterface("Lectio");view.stopLoading();view.destroy()})
}
