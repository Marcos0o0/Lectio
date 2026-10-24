package app.lectio.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lectio.reader.ReaderState
import app.lectio.reader.data.Book
import app.lectio.reader.data.LibraryCatalog
import app.lectio.reader.data.LibraryFilter
import app.lectio.reader.data.LibrarySort

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun LibraryScreen(state: ReaderState, open: (Book) -> Unit, importBook: () -> Unit, remove: (Book) -> Unit, snackbars: SnackbarHostState, favorite: (Book) -> Unit, order: (LibrarySort) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var search by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var sortMenu by remember {mutableStateOf(false)}
    var delete by remember { mutableStateOf<Book?>(null) }
    val list=rememberLazyListState()
    val focus=remember {FocusRequester()}
    val keyboard=LocalSoftwareKeyboardController.current
    val sort=LibrarySort.from(state.preferences.librarySort)
    val favorites=state.preferences.favoriteBooks
    fun closeSearch() {search=false;query="";keyboard?.hide()}
    BackHandler(enabled=search) {closeSearch()}
    LaunchedEffect(search) {if(search) list.scrollToItem(0)}
    val current=state.books.filter { it.lastRead>0 }.maxByOrNull { it.lastRead } ?: state.books.firstOrNull()
    val books=remember(state.books,query,filter,favorites,sort) {LibraryCatalog.books(state.books,query,filter,favorites,sort)}
    Scaffold(
        snackbarHost={SnackbarHost(snackbars)},
        topBar={ TopAppBar(title={Text("Lectio",style=MaterialTheme.typography.headlineMedium)},actions={
            IconButton(onClick={if(search) closeSearch() else search=true}) { Icon(if(search) Icons.Outlined.Close else Icons.Outlined.Search,if(search) "Cerrar búsqueda" else "Buscar libros") }
        },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.background)) },
        floatingActionButton={ ExtendedFloatingActionButton(onClick={if(!state.importing) importBook()},icon={if(state.importing) CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp) else Icon(Icons.Outlined.Add,null)},text={Text(if(state.importing) "Importando…" else "Importar EPUB")},containerColor=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary) },
        contentWindowInsets=WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),state=list,contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=112.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(search) item(key="search") {
                LaunchedEffect(Unit) {focus.requestFocus()}
                OutlinedTextField(value=query,onValueChange={query=it},Modifier.fillMaxWidth().focusRequester(focus),label={Text("Buscar en tu biblioteca")},placeholder={Text("Título o autor")},leadingIcon={Icon(Icons.Outlined.Search,null)},
                    trailingIcon={if(query.isNotEmpty()) IconButton(onClick={query=""}) {Icon(Icons.Outlined.Close,"Borrar búsqueda")}},singleLine=true,
                    keyboardOptions=KeyboardOptions(imeAction=ImeAction.Search),keyboardActions=KeyboardActions(onSearch={keyboard?.hide()}))
            }
            item {
                Column(Modifier.padding(top=12.dp,bottom=12.dp)) {
                    Text("¿Qué te gustaría leer?",style=MaterialTheme.typography.headlineLarge)
                }
            }
            if(current!=null && !search && filter==LibraryFilter.ALL) item {
                Card(onClick={open(current)},shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=Alignment.CenterVertically) {
                        BookCover(current,Modifier.width(98.dp).height(148.dp),compact=true)
                        Column(Modifier.weight(1f)) {
                            Text(if(current.lastRead==0L) "Por empezar" else "Continuar leyendo",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(10.dp));Text(current.title,style=MaterialTheme.typography.headlineSmall,color=MaterialTheme.colorScheme.onPrimaryContainer,maxLines=3,overflow=TextOverflow.Ellipsis)
                            Spacer(Modifier.height(10.dp));Text(if(current.demo) "Lectura de muestra · en inglés" else current.author,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onPrimaryContainer,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Spacer(Modifier.height(16.dp))
                            LinearProgressIndicator(progress={current.progress},Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=.12f))
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment=Alignment.CenterVertically) { Text(if(current.lastRead==0L) "Empezar a leer" else "${(current.progress*100).toInt()} % leído",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onPrimaryContainer); Icon(Icons.AutoMirrored.Outlined.ArrowForward,null,tint=MaterialTheme.colorScheme.onPrimaryContainer) }
                        }
                    }
                }
            }
            item {
                SectionHeading("Tu biblioteca",Modifier.padding(top=12.dp)) { Text(if(query.isNotBlank()||filter!=LibraryFilter.ALL) "${books.size} de ${state.books.size}" else "${books.size} ${if(books.size==1) "libro" else "libros"}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant) }
                FlowRow(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    LibraryFilter.entries.forEach {option -> FilterChip(selected=filter==option,onClick={filter=option;keyboard?.hide()},label={Text(option.label)}) }
                }
                Box(Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)) {
                    TextButton(onClick={sortMenu=true}) {Text("Orden: ${sort.label}");Icon(Icons.Outlined.ArrowDropDown,null)}
                    DropdownMenu(expanded=sortMenu,onDismissRequest={sortMenu=false}) {
                        LibrarySort.entries.forEach {option -> DropdownMenuItem(text={Text(option.label)},onClick={order(option);sortMenu=false},modifier=Modifier.semantics {selected=sort==option},trailingIcon={if(sort==option) Icon(Icons.Outlined.Check,null)})}
                    }
                }
            }
            if(books.isEmpty()) item {
                val title=when {query.isNotBlank()->"Sin coincidencias";filter==LibraryFilter.FAVORITES->"Aún no tienes favoritos";filter==LibraryFilter.READING->"No hay libros en lectura";filter==LibraryFilter.FINISHED->"Aún no hay libros terminados";else->"Tu biblioteca está vacía"}
                val detail=when {query.isNotBlank()->"Prueba con otro título o autor, o cambia el filtro.";filter==LibraryFilter.FAVORITES->"Toca el corazón junto a un libro para añadirlo.";filter==LibraryFilter.ALL->"Importa un archivo EPUB para empezar. Tus libros se guardan en este dispositivo.";else->"Aquí aparecerán los libros de esta categoría."}
                EmptyState(if(filter==LibraryFilter.FAVORITES) Icons.Outlined.FavoriteBorder else Icons.Outlined.AutoStories,title,detail,if(filter==LibraryFilter.ALL&&query.isBlank()) "Importar EPUB" else null,importBook)
            }
            items(books,key={it.id}) { book ->
                Row(Modifier.fillMaxWidth().clickable {open(book)}.padding(vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=Alignment.CenterVertically) {
                    BookCover(book,Modifier.width(72.dp).height(108.dp),compact=true)
                    Column(Modifier.weight(1f)) {
                        Text(book.title,style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp));Text(book.author,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=2,overflow=TextOverflow.Ellipsis)
                        Spacer(Modifier.height(12.dp))
                        Text(if(book.lastRead==0L) "Por empezar · ${book.chapters.size} capítulos" else "${(book.progress*100).toInt()} % · Capítulo ${book.chapter+1} de ${book.chapters.size}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
                    }
                    var menu by remember { mutableStateOf(false) }
                    Column(verticalArrangement=Arrangement.spacedBy(8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                        val isFavorite=book.id in favorites
                        IconToggleButton(checked=isFavorite,onCheckedChange={favorite(book)}) {Icon(if(isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,if(isFavorite) "Quitar de favoritos: ${book.title}" else "Añadir a favoritos: ${book.title}")}
                        Box { IconButton(onClick={menu=true}) {Icon(Icons.Outlined.MoreVert,"Opciones de ${book.title}")};DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text("Abrir libro")},onClick={menu=false;open(book)},leadingIcon={Icon(Icons.Outlined.MenuBook,null)})
                        DropdownMenuItem(text={Text("Eliminar del dispositivo")},onClick={menu=false;delete=book},leadingIcon={Icon(Icons.Outlined.DeleteOutline,null)})
                        } }
                    }
                }
            }
        }
    }
    delete?.let { book -> AlertDialog(onDismissRequest={delete=null},title={Text("¿Eliminar este libro?")},text={Text("Se borrarán «${book.title}», su progreso y sus anotaciones de Lectio. El archivo original y tu vocabulario se conservarán.")},confirmButton={TextButton(onClick={remove(book);delete=null}) {Text("Eliminar")}},dismissButton={TextButton(onClick={delete=null}) {Text("Cancelar")}}) }
}
