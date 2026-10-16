package app.lectio.reader.ui

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
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
    val book=state.current ?: return
    var panel by rememberSaveable(book.id) {mutableStateOf<String?>(null)}
    var webView by remember {mutableStateOf<WebView?>(null)}
    var searchTerm by rememberSaveable(book.id) {mutableStateOf("")}
    var searchOccurrence by rememberSaveable(book.id) {mutableIntStateOf(0)}
    val lifecycle=LocalLifecycleOwner.current
    LaunchedEffect(book.id,lifecycle) {
        lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while(true) {delay(10000);vm.readingTick(10)}
        }
    }
    LaunchedEffect(state.selection) { if(state.selection==null) webView?.evaluateJavascript("window.getSelection().removeAllRanges();window.lastLookup='';",null) }
    Scaffold(
        snackbarHost={SnackbarHost(snackbars)},
        topBar={TopAppBar(title={Column {Text(book.title,style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis);Text("Capítulo ${book.chapter+1} de ${book.chapters.size}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},navigationIcon={IconButton(onClick=vm::closeBook){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Volver a la biblioteca")}},actions={
            IconButton(onClick={panel="search"}){Icon(Icons.Outlined.Search,"Buscar dentro del libro")}
            IconButton(onClick={panel="type"}){Icon(Icons.Outlined.TextFields,"Apariencia de lectura")}
        })},
        bottomBar={Surface(color=MaterialTheme.colorScheme.surface) { Column(Modifier.navigationBarsPadding()) {
            LinearProgressIndicator(progress={book.progress},Modifier.fillMaxWidth(),trackColor=MaterialTheme.colorScheme.surfaceVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                IconButton(onClick={panel="contents"}){Icon(Icons.Outlined.FormatListBulleted,"Índice y anotaciones")}
                IconButton(onClick={vm.chapter(book.chapter-1)},enabled=book.chapter>0){Icon(Icons.AutoMirrored.Outlined.NavigateBefore,"Capítulo anterior")}
                Text("${(book.progress*100).toInt()} %",style=MaterialTheme.typography.labelLarge)
                IconButton(onClick={if(book.chapter==book.chapters.lastIndex){vm.progress(book.chapter,1f);vm.closeBook()}else vm.chapter(book.chapter+1)}){Icon(if(book.chapter==book.chapters.lastIndex) Icons.Outlined.DoneAll else Icons.AutoMirrored.Outlined.NavigateNext,if(book.chapter==book.chapters.lastIndex) "Terminar libro" else "Capítulo siguiente")}
                IconButton(onClick={vm.annotate(text=book.chapters[book.chapter].title)}){Icon(Icons.Outlined.BookmarkAdd,"Guardar marcador")}
            }
        } }},contentWindowInsets=WindowInsets(0,0,0,0)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if(book.demo&&book.chapter==0) Text("Mantén pulsada una palabra para descubrir su significado.",Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=10.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
            key(book.id,book.chapter) {
                ReaderWebView(book,state.preferences,MaterialTheme.colorScheme.background,MaterialTheme.colorScheme.onBackground,state.annotations,searchTerm,searchOccurrence,state.navigationId,state.fragment,Modifier.weight(1f).fillMaxWidth(),{webView=it},vm::select,{vm.readerProgress(book.id,book.chapter,it)},{index,anchor->vm.chapter(index,fragment=anchor)},vm::navigationConsumed)
            }
        }
    }
    when(panel) {
        "type" -> ModalBottomSheet(onDismissRequest={panel=null}) {
            Column(Modifier.fillMaxWidth().padding(horizontal=24.dp).padding(bottom=32.dp)) {
                Text("Tu página, a tu ritmo",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Sistema","Claro","Oscuro").forEach { FilterChip(selected=state.preferences.theme==it,onClick={vm.preferences(state.preferences.copy(theme=it))},label={Text(it)}) }}
                ReadingPreferences(state.preferences,vm::preferences)
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
                    if(state.annotations.isEmpty()) item {EmptyState(Icons.Outlined.BookmarkBorder,"Guarda una idea","Usa el marcador para guardar tu lugar, o selecciona un fragmento para subrayarlo y añadir una nota.")}
                    itemsIndexed(state.annotations) { _,annotation -> ListItem(modifier=Modifier.clickable {vm.chapter(annotation.chapter,annotation.position);panel=null},headlineContent={Text(annotation.text.ifBlank {"Marcador"},maxLines=3,overflow=TextOverflow.Ellipsis)},supportingContent={Text("Capítulo ${annotation.chapter+1}${if(annotation.note.isNotBlank()) " · ${annotation.note}" else ""}")},leadingContent={Icon(if(annotation.type=="highlight") Icons.Outlined.BorderColor else Icons.Outlined.BookmarkBorder,null)},trailingContent={IconButton(onClick={vm.deleteAnnotation(annotation.id)}) {Icon(Icons.Outlined.DeleteOutline,"Eliminar anotación")}}) }
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
                Text("Entre las páginas",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(16.dp))
                OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Buscar palabra o frase")},singleLine=true,leadingIcon={Icon(Icons.Outlined.Search,null)})
                Spacer(Modifier.height(12.dp));Text(if(searching) "Buscando en el libro…" else if(query.length<2) "Escribe al menos dos caracteres." else "${matches.size} resultados${if(matches.size>=100) " (primeros 100)" else ""}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max=440.dp),contentPadding=PaddingValues(bottom=32.dp)) {
                itemsIndexed(matches) { _,match ->ListItem(modifier=Modifier.clickable {
                    searchTerm=query.trim();searchOccurrence=match.occurrence
                    if(match.chapter==book.chapter) webView?.let {view ->
                        var moved=false
                        view.setFindListener {_,count,done->if(done&&!moved&&count>0){moved=true;repeat(match.occurrence.coerceAtMost(count-1)){view.findNext(true)}}}
                        view.clearMatches();view.findAllAsync(searchTerm)
                    } else vm.chapter(match.chapter)
                    panel=null
                },headlineContent={Text(book.chapters[match.chapter].title,style=MaterialTheme.typography.titleMedium)},supportingContent={Text(match.snippet)})}
            }
        }
    }
}
