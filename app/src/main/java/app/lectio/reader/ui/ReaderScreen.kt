package app.lectio.reader.ui

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateBefore
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.repeatOnLifecycle
import app.lectio.reader.ReaderState
import app.lectio.reader.ReaderViewModel
import app.lectio.reader.data.Epub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReaderScreen(vm: ReaderViewModel,state: ReaderState,snackbars: SnackbarHostState) {
    val appScheme=MaterialTheme.colorScheme
    val scheme=readingColors(state.preferences)
    val view=LocalView.current
    fun bars(light: Boolean) {(view.context as? android.app.Activity)?.window?.let {window -> androidx.core.view.WindowCompat.getInsetsController(window,view).apply {isAppearanceLightStatusBars=light;isAppearanceLightNavigationBars=light}}}
    SideEffect {bars(scheme.background.luminance()>.5f)}
    DisposableEffect(view,appScheme) {onDispose {bars(appScheme.background.luminance()>.5f)}}
    MaterialTheme(colorScheme=scheme) {ReaderContent(vm,state,snackbars)}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ReaderContent(vm: ReaderViewModel,state: ReaderState,snackbars: SnackbarHostState) {
    val book=state.current ?: return
    val readingScheme=readingColors(state.preferences)
    var panel by rememberSaveable(book.id) {mutableStateOf<String?>(null)}
    var webView by remember {mutableStateOf<WebView?>(null)}
    var pagination by remember(book.id,book.chapter) {mutableStateOf(ReaderPage())}
    fun turn(direction: Int) {webView?.evaluateJavascript("window.lectioTurn && window.lectioTurn($direction)",null)}
    var searchTerm by rememberSaveable(book.id) {mutableStateOf("")}
    var pendingSearch by rememberSaveable(book.id) {mutableStateOf("")}
    var searchOccurrence by rememberSaveable(book.id) {mutableIntStateOf(0)}
    val lifecycle=LocalLifecycleOwner.current
    val view=LocalView.current
    val hideBars=state.immersive&&panel==null&&state.selection==null
    DisposableEffect(view,lifecycle,hideBars) {
        val controller=(view.context as? android.app.Activity)?.window?.let {WindowCompat.getInsetsController(it,view)}
        val behavior=controller?.systemBarsBehavior
        fun applyBars() {controller?.apply {
            systemBarsBehavior=WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if(hideBars) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())
        }}
        applyBars()
        val observer=LifecycleEventObserver {_,event -> if(event==Lifecycle.Event.ON_RESUME) applyBars()}
        lifecycle.lifecycle.addObserver(observer)
        onDispose {lifecycle.lifecycle.removeObserver(observer);controller?.show(WindowInsetsCompat.Type.systemBars());if(behavior!=null) controller?.systemBarsBehavior=behavior}
    }
    LaunchedEffect(book.id,lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(true) {delay(10000);vm.readingTick(10)}
        }
    }
    LaunchedEffect(state.selection) { if(state.selection==null) webView?.evaluateJavascript("window.getSelection().removeAllRanges();window.lastLookup='';",null) }
    Scaffold(
        snackbarHost={SnackbarHost(snackbars)},
        topBar={if(!state.immersive) TopAppBar(title={Column {Text(book.title,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis);Text("${book.chapter+1}/${book.chapters.size} · ${book.chapters[book.chapter].title}",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},navigationIcon={IconButton(onClick=vm::closeBook){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Volver a la biblioteca")}},actions={
            IconButton(onClick={panel="search"}){Icon(Icons.Outlined.Search,"Buscar dentro del libro")}
            IconButton(onClick={panel="type"}){Icon(Icons.Outlined.TextFields,"Apariencia de lectura")}
            IconButton(onClick={vm.immersive(true)}){Icon(Icons.Outlined.Fullscreen,"Leer a pantalla completa")}
        })},
        bottomBar={if(!state.immersive) Surface(color=MaterialTheme.colorScheme.surface) { Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(progress={book.progress},Modifier.fillMaxWidth(),trackColor=MaterialTheme.colorScheme.surfaceVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                IconButton(onClick={panel="contents"}){Icon(Icons.Outlined.FormatListBulleted,"Índice y anotaciones")}
                IconButton(onClick={turn(-1)},enabled=pagination.pages>0&&(pagination.page>0||book.chapter>0)){Icon(Icons.AutoMirrored.Outlined.NavigateBefore,"Página anterior")}
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(if(pagination.pages==0) "Preparando páginas…" else "Página ${pagination.page+1} de ${pagination.pages}",style=MaterialTheme.typography.labelLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
                    Text("del capítulo · ${(book.progress*100).toInt()} % del libro",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick={turn(1)},enabled=pagination.pages>0&&(pagination.page<pagination.pages-1||book.chapter<book.chapters.lastIndex)){Icon(Icons.AutoMirrored.Outlined.NavigateNext,"Página siguiente")}
                IconButton(onClick={vm.annotate(text=book.chapters[book.chapter].title)}){Icon(Icons.Outlined.BookmarkAdd,"Guardar marcador")}
            }
        } }},contentWindowInsets=WindowInsets(0,0,0,0)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).then(if(state.immersive) Modifier.windowInsetsPadding(WindowInsets.displayCutout) else Modifier)) {
            if(state.preferences.showReadingHint&&!state.immersive) Text(if(state.preferences.tapToTranslate) "Desliza para pasar página. Toca una palabra para consultar; mantén pulsado para seleccionar." else "Desliza para pasar página. Mantén pulsada una palabra o frase para consultar.",Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                key(book.id,book.chapter) {
                    ReaderWebView(book,state.preferences,readingScheme.background,readingScheme.onBackground,state.annotations,pendingSearch,searchOccurrence,state.navigationId,state.fragment,Modifier.fillMaxSize().padding(top=if(state.immersive) 56.dp else 0.dp),{webView=it},vm::select,{pagination=it;vm.readerProgress(book.id,book.chapter,it.progress,it.offset)},{direction->if(direction<0&&book.chapter>0)vm.chapter(book.chapter-1,1f) else if(direction>0&&book.chapter<book.chapters.lastIndex)vm.chapter(book.chapter+1)},{index,anchor->vm.chapter(index,fragment=anchor)},{pendingSearch="";vm.navigationConsumed()})
                }
                if(state.immersive) IconButton(onClick={vm.immersive(false)},modifier=Modifier.align(Alignment.TopEnd).padding(horizontal=8.dp,vertical=4.dp).size(48.dp)) {Icon(Icons.Outlined.FullscreenExit,"Salir de pantalla completa")}
            }
        }
    }
    when(panel) {
        "type" -> ModalBottomSheet(onDismissRequest={panel=null},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).padding(horizontal=24.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {Text("Lectura",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall);IconButton(onClick={panel=null}) {Icon(Icons.Outlined.Close,"Cerrar apariencia")}}
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom=32.dp)) {ReadingPreferences(state.preferences,vm::preferences)}
            }
        }
        "contents" -> ModalBottomSheet(onDismissRequest={panel=null}) {
            var tab by rememberSaveable {mutableIntStateOf(0)}
            Text("Dentro de este libro",Modifier.padding(horizontal=24.dp),style=MaterialTheme.typography.headlineSmall)
            PrimaryTabRow(selectedTabIndex=tab) {Tab(selected=tab==0,onClick={tab=0},text={Text("Índice")});Tab(selected=tab==1,onClick={tab=1},text={Text("Anotaciones (${state.annotations.size})")})}
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=500.dp),contentPadding=PaddingValues(bottom=32.dp)) {
                if(tab==0) itemsIndexed(book.chapters) { i,chapter ->
                    ListItem(modifier=Modifier.clickable {vm.chapter(i);panel=null},headlineContent={Text(chapter.title)},leadingContent={Text("${i+1}",color=MaterialTheme.colorScheme.primary)},trailingContent={if(i==book.chapter) Icon(Icons.Outlined.MenuBook,"Capítulo actual",tint=MaterialTheme.colorScheme.primary)})
                } else {
                    if(state.annotations.isEmpty()) item {EmptyState(Icons.Outlined.BookmarkBorder,"Aún no hay anotaciones","Usa el marcador para guardar tu lugar, o selecciona un fragmento para subrayarlo y añadir una nota.")}
                    itemsIndexed(state.annotations) { _,annotation -> ListItem(modifier=Modifier.clickable {vm.chapter(annotation.chapter,annotation.position,textOffset=annotation.textOffset);panel=null},headlineContent={Text(annotation.text.ifBlank {"Marcador"},maxLines=3,overflow=TextOverflow.Ellipsis)},supportingContent={Text("Capítulo ${annotation.chapter+1}${if(annotation.note.isNotBlank()) " · ${annotation.note}" else ""}")},leadingContent={Icon(if(annotation.type=="highlight") Icons.Outlined.BorderColor else Icons.Outlined.BookmarkBorder,null)},trailingContent={IconButton(onClick={vm.deleteAnnotation(annotation.id)}) {Icon(Icons.Outlined.DeleteOutline,"Eliminar anotación")}}) }
                }
            }
        }
        "search" -> ModalBottomSheet(onDismissRequest={panel=null}) {
            var query by rememberSaveable {mutableStateOf(searchTerm)}
            data class Match(val chapter: Int,val snippet: String,val occurrence: Int)
            var searching by remember {mutableStateOf(false)}
            val matches by produceState<List<Match>>(emptyList(),query) {
                value=emptyList()
                if(query.trim().length>=2) {
                    searching=true;delay(350)
                    value=withContext(Dispatchers.IO) {buildList {
                        book.chapters.forEachIndexed { i,ch ->
                            if(size>=100)return@forEachIndexed
                            val text=runCatching {Jsoup.parse(Epub.safeBody(Epub.resource(File(book.file),ch.path))).body().text()}.getOrDefault("")
                            var start=0;var count=0
                            while(count<5) {val found=text.indexOf(query.trim(),start,true);if(found<0)break;add(Match(i,"…"+text.substring((found-45).coerceAtLeast(0),(found+query.length+85).coerceAtMost(text.length))+"…",count));start=found+query.length;count++}
                        }
                    }}
                    searching=false
                } else searching=false
            }
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp)) {
                Text("Buscar en el libro",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(16.dp))
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Buscar palabra o frase")},singleLine=true,leadingIcon={Icon(Icons.Outlined.Search,null)})
                Spacer(Modifier.height(12.dp));Text(if(searching) "Buscando en el libro…" else if(query.length<2) "Escribe al menos dos caracteres." else "${matches.size} resultados${if(matches.size>=100) " (primeros 100)" else ""}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=440.dp),contentPadding=PaddingValues(bottom=32.dp)) {
                itemsIndexed(matches) { _,match ->ListItem(modifier=Modifier.clickable {
                    searchTerm=query.trim();searchOccurrence=match.occurrence
                    pendingSearch=if(match.chapter==book.chapter) "" else searchTerm
                    if(match.chapter==book.chapter) webView?.let {view ->
                        view.evaluateJavascript("window.lectioSearch(${org.json.JSONObject.quote(searchTerm)},${match.occurrence})",null)
                    } else vm.chapter(match.chapter)
                    panel=null
                },headlineContent={Text(book.chapters[match.chapter].title,style=MaterialTheme.typography.titleMedium)},supportingContent={Text(match.snippet)})}
            }
        }
    }
}
