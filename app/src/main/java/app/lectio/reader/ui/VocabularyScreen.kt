package app.lectio.reader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.lectio.reader.data.Word
import app.lectio.reader.data.DictionaryClient
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun VocabularyScreen(words: List<Word>, pronounce: (String) -> Unit, toggle: (Word) -> Unit, delete: (Word) -> Unit, review: (Word,Boolean) -> Unit) {
    val uri=LocalUriHandler.current
    var query by rememberSaveable {mutableStateOf("")}
    var filter by rememberSaveable {mutableStateOf(0)}
    var selected by remember {mutableStateOf<Word?>(null)}
    var reviewing by rememberSaveable {mutableStateOf(false)}
    val due=words.filter {!it.learned && it.due<=System.currentTimeMillis()}
    val filtered=words.filter { (it.text.contains(query,true)||it.translation.contains(query,true)) && when(filter) {1->!it.learned;2->it.learned;else->true} }
    Scaffold(topBar={TopAppBar(title={Text("Vocabulario",style=MaterialTheme.typography.headlineMedium)})},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(words.isNotEmpty()) item {
                Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.large) { Row(Modifier.fillMaxWidth().padding(20.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("${due.size} ${if(due.size==1) "palabra para hoy" else "palabras para hoy"}",style=MaterialTheme.typography.titleMedium);Text("Repaso espaciado",style=MaterialTheme.typography.bodySmall) }
                    FilledTonalButton(onClick={reviewing=true},enabled=due.isNotEmpty()) {Text("Repasar")}
                } }
            }
            item { OutlinedTextField(value=query,onValueChange={query=it},Modifier.fillMaxWidth(),placeholder={Text("Buscar en tu vocabulario")},singleLine=true,leadingIcon={Icon(Icons.Outlined.Search,null)})
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Todas","Aprendiendo","Aprendidas").forEachIndexed { i,label -> FilterChip(selected=filter==i,onClick={filter=i},label={Text(label)}) } }
            }
            if(filtered.isEmpty()) item { EmptyState(Icons.Outlined.Bookmarks,if(words.isEmpty()) "Aún no hay palabras guardadas" else "Sin resultados",if(words.isEmpty()) "Toca una palabra en el libro y elige Guardar en vocabulario." else "Cambia el filtro o prueba otra búsqueda.") }
            items(filtered,key={it.text}) { word ->
                ListItem(modifier=Modifier.clickable {selected=word},headlineContent={Text(word.text,style=MaterialTheme.typography.titleLarge)},supportingContent={Column {Text(word.translation,maxLines=2);if(word.book.isNotBlank()) Text(word.book,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)}},trailingContent={IconButton(onClick={pronounce(word.text)}) {Icon(Icons.Outlined.VolumeUp,"Pronunciar ${word.text}")}},colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.background))
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    selected?.let { original -> val word=words.find {it.text==original.text} ?: original
        ModalBottomSheet(onDismissRequest={selected=null}) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {Text(word.text,Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge);IconButton(onClick={pronounce(word.text)}) {Icon(Icons.Outlined.VolumeUp,"Escuchar pronunciación")}}
                Text(word.translation,style=MaterialTheme.typography.titleLarge)
                if(word.definition.isNotBlank()&&word.definition!=word.translation) Text(word.definition)
                if(DictionaryClient.safeLink(word.dictionarySource)) {
                    Text(if(word.language=="es") "Autores de Wikcionario · definición en español." else "Autores de Wiktionary. La definición puede estar traducida automáticamente.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick={uri.openUri(word.dictionarySource)}) {Text("Ver definición original")}
                    if(DictionaryClient.safeLink(word.dictionaryLicense)) TextButton(onClick={uri.openUri(word.dictionaryLicense)}) {Text("CC BY-SA 4.0")}
                }
                if(word.context.isNotBlank()) Text("“${word.context}”",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(word.example.isNotBlank()) Text(word.example,style=MaterialTheme.typography.bodyMedium)
                Text(listOf(word.source,word.level.takeIf {it.isNotBlank()}?.let {"CEFR orientativo: $it"}).filterNotNull().joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick={toggle(word)},Modifier.fillMaxWidth()) {Icon(if(word.learned) Icons.Outlined.Refresh else Icons.Outlined.Check,null);Spacer(Modifier.width(8.dp));Text(if(word.learned) "Volver a aprender" else "Marcar como aprendida")}
                TextButton(onClick={delete(word);selected=null},Modifier.fillMaxWidth()) {Text("Eliminar del vocabulario")}
            }
        }
    }
    if(reviewing) ReviewDialog(due,onClose={reviewing=false},review=review,pronounce=pronounce)
}

@Composable private fun ReviewDialog(due: List<Word>,onClose: () -> Unit,review: (Word,Boolean) -> Unit,pronounce: (String) -> Unit) {
    val queue=remember {due.toList()}
    var index by rememberSaveable {mutableIntStateOf(0)}
    var revealed by rememberSaveable {mutableStateOf(false)}
    val word=queue.getOrNull(index)
    AlertDialog(onDismissRequest=onClose,icon={Icon(Icons.Outlined.Style,null)},title={Text(if(word==null) "Repaso completado" else "${index+1} de ${queue.size}")},text={
        Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(word==null) Text("Has terminado las palabras pendientes de hoy.")
            else {
                Row(verticalAlignment=Alignment.CenterVertically) {Text(word.text,Modifier.weight(1f),style=MaterialTheme.typography.headlineLarge);IconButton(onClick={pronounce(word.text)}) {Icon(Icons.Outlined.VolumeUp,"Pronunciar")}}
                if(revealed) {Text(word.translation,style=MaterialTheme.typography.titleLarge);if(word.example.isNotBlank()) Text(word.example)} else Text("¿Recuerdas su significado?",color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    },confirmButton={when {word==null->TextButton(onClick=onClose){Text("Listo")};!revealed->TextButton(onClick={revealed=true}){Text("Ver significado")};else->TextButton(onClick={review(word,true);index++;revealed=false}){Text("Lo recordé")}}},dismissButton={if(word!=null&&revealed) TextButton(onClick={review(word,false);index++;revealed=false}) {Text("Otra vez")}})
}
