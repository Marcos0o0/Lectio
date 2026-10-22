package app.lectio.reader.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lectio.reader.R
import app.lectio.reader.ReaderState
import app.lectio.reader.ReaderViewModel
import app.lectio.reader.data.DictionaryEntry

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun LookupSheet(vm: ReaderViewModel,state: ReaderState,pronounce: (String) -> Unit,settings: () -> Unit) {
    val selection=state.selection ?: return
    val result=state.lookup
    val spanish=state.lookupLanguage=="es"
    var tab by remember(selection) {mutableIntStateOf(0)}
    var manual by remember(selection) {mutableStateOf("")}
    var note by remember(selection) {mutableStateOf("")}
    val isWord=selection.text.trim().split(Regex("\\s+")).size==1
    val canSave=selection.text.trim().split(Regex("\\s+")).size<=12
    ModalBottomSheet(onDismissRequest=vm::dismissSelection,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).imePadding()) {
            Row(Modifier.padding(start=24.dp,end=12.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if(isWord) selection.text else if(spanish) "Consulta" else "Traducción",style=MaterialTheme.typography.headlineMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                    state.dictionary?.phonetic?.takeIf {it.isNotBlank()}?.let {Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
                IconButton(onClick={pronounce(selection.text)}) {Icon(Icons.Outlined.VolumeUp,"Escuchar texto en ${if(spanish) "español" else "inglés"}")}
                IconButton(onClick=vm::dismissSelection) {Icon(Icons.Outlined.Close,"Cerrar consulta")}
            }
            PrimaryTabRow(selectedTabIndex=tab) {
                listOf(if(isWord) "Significado" else if(spanish) "Texto" else "Traducción","Contexto","Notas").forEachIndexed {index,title ->
                    Tab(selected=tab==index,onClick={tab=index},text={Text(title,maxLines=1)})
                }
            }
            key(tab) {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    when(tab) {
                        0 -> {
                            Text("Idioma del texto",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                listOf("es" to "Español","en" to "Inglés").forEach {(code,label) -> FilterChip(selected=state.lookupLanguage==code,onClick={vm.lookupLanguage(code)},label={Text(label)})}
                            }
                            if(!isWord) SelectionContainer {Text(selection.text,style=MaterialTheme.typography.bodyMedium)}
                            if(!spanish&&state.lookupLoading&&result==null) LookupProgress("Traduciendo…")
                            if(!spanish&&(state.translationNeedsDownload||state.translationDownloading)) {
                                Text("Descarga español para traducir",style=MaterialTheme.typography.titleMedium)
                                Text("Alrededor de 30 MB. Se descarga una vez y funciona sin conexión.",style=MaterialTheme.typography.bodyMedium)
                                Button(onClick={vm.downloadTranslation()},enabled=!state.translationDownloading,modifier=Modifier.fillMaxWidth()) {Text(if(state.translationDownloading) "Descargando idioma…" else "Descargar por Wi-Fi")}
                                if(state.translationDownloading) LinearProgressIndicator(Modifier.fillMaxWidth())
                                else TextButton(onClick={vm.downloadTranslation(false)}) {Text("Descargar usando datos móviles")}
                            }
                            state.lookupError?.let {Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick={vm.query("translate",false)}) {Text("Reintentar traducción")}}
                            if(!spanish&&result!=null) {
                                Text("Traducción",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                SelectionContainer {Text(result.translation,style=if(isWord) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)}
                                TranslationCredit(result.source)
                            }
                            if(isWord) {
                                if(!spanish) HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                                if(state.definitionLoading) LookupProgress("Buscando significado…")
                                state.dictionary?.let {entry ->
                                    DictionaryMeaning(entry) {word -> vm.select(app.lectio.reader.data.Selection(word,selection.context))}
                                }
                                if(!state.definitionLoading&&state.dictionary==null) {
                                    Text(state.definitionError ?: if(state.preferences.dictionaryEnabled) "No encontramos una entrada en ${if(spanish) "español" else "inglés"}. Comprueba el idioma o intenta otra palabra." else "Activa el diccionario en Ajustes para consultar palabras nuevas.",style=MaterialTheme.typography.bodyMedium,color=if(state.definitionError!=null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                    if(state.preferences.dictionaryEnabled) TextButton(onClick=vm::define) {Text("Reintentar significado")}
                                }
                            }
                            if(result?.definition?.isNotBlank()==true) {
                                Text("Explicación contextual",style=MaterialTheme.typography.titleMedium)
                                Text(result.definition,style=MaterialTheme.typography.bodyMedium)
                                Text(result.source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick={tab=1},contentPadding=PaddingValues(0.dp)) {Text("Ver la oración del libro")}
                        }
                        1 -> {
                            Text("En esta oración",style=MaterialTheme.typography.titleMedium)
                            SelectionContainer {Text(selection.context.ifBlank {selection.text},style=MaterialTheme.typography.bodyLarge)}
                            val translated=result?.contextTranslation?.takeIf {it.isNotBlank()} ?: if(selection.context.isBlank()||selection.context.trim()==selection.text.trim()) result?.translation else null
                            if(translated!=null) {HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant);SelectionContainer {Text(translated,style=MaterialTheme.typography.bodyLarge)};TranslationCredit(result?.source.orEmpty())}
                            else if(!spanish) Text("La traducción de la oración aparecerá cuando esté lista la consulta.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            if(!spanish) {
                                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                                Text("Entender el fragmento",style=MaterialTheme.typography.titleMedium)
                                if(state.preferences.key.isBlank()) {
                                    Text("Puedes añadir explicaciones sobre el uso de esta palabra y la gramática con tu cuenta de Gemini.",style=MaterialTheme.typography.bodyMedium)
                                    OutlinedButton(onClick=settings,Modifier.fillMaxWidth()) {Text("Conectar Gemini")}
                                } else {
                                    Text("Cada acción puede consumir saldo de tu cuenta.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("explain" to "Explicar significado","grammar" to "Gramática","simplify" to "Inglés sencillo").forEach {(action,label) -> AssistChip(onClick={vm.query(action)},enabled=!state.lookupLoading,label={Text(label)})}}
                                }
                                if(state.lookupLoading) LookupProgress("Consultando…")
                                state.lookupError?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                                if(result?.definition?.isNotBlank()==true) {
                                    Text(result.definition,style=MaterialTheme.typography.bodyLarge)
                                    if(result.example.isNotBlank()) {Text("Ejemplo",style=MaterialTheme.typography.titleSmall);Text(result.example)}
                                    if(result.synonyms.isNotBlank()) Text("Sinónimos: ${result.synonyms}")
                                    Text("${result.source}. Generado con IA; puede contener errores.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        2 -> {
                            Text("Fragmento seleccionado",style=MaterialTheme.typography.titleSmall)
                            SelectionContainer {Text(selection.text,style=MaterialTheme.typography.bodyLarge)}
                            OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("Nota (opcional)")},maxLines=5)
                            FilledTonalButton(onClick={vm.annotate(selection.text,note,"highlight");vm.dismissSelection()},Modifier.fillMaxWidth()) {Icon(Icons.Outlined.BorderColor,null);Spacer(Modifier.width(8.dp));Text("Guardar subrayado")}
                            if(result==null&&canSave) {
                                OutlinedTextField(manual,{manual=it},Modifier.fillMaxWidth(),label={Text(if(spanish) "Mi significado" else "Mi traducción")},maxLines=4)
                                OutlinedButton(onClick={vm.saveWord(manual)},enabled=manual.isNotBlank(),modifier=Modifier.fillMaxWidth()) {Text(if(spanish) "Guardar significado propio" else "Guardar traducción propia")}
                            }
                        }
                    }
                }
            }
            if((result!=null||(spanish&&state.dictionary!=null))&&canSave) {
                HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                Button(onClick={vm.saveWord()},Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),enabled=!state.lookupLoading&&!state.definitionLoading) {
                    Icon(Icons.Outlined.BookmarkAdd,null);Spacer(Modifier.width(8.dp));Text(if(state.words.any {it.text.equals(selection.text,true)}) "Actualizar vocabulario" else "Guardar en vocabulario")
                }
            }
        }
    }
}

@Composable private fun LookupProgress(label: String) {
    Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp);Text(label,style=MaterialTheme.typography.bodyMedium)}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun DictionaryMeaning(entry: DictionaryEntry,onRelated: (String)->Unit) {
    var original by remember(entry.word) {mutableStateOf(false)}
    var more by remember(entry.word) {mutableStateOf(false)}
    val uri=LocalUriHandler.current
    Text("Significados",style=MaterialTheme.typography.titleMedium)
    if(entry.lemma.isNotBlank()) Text("Forma de «${entry.lemma}»",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    if(entry.language=="es") Text("Entrada: ${entry.word}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    (if(more) entry.senses else entry.senses.take(2)).forEachIndexed {index,sense ->
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("${index+1}. ${partOfSpeech(sense.partOfSpeech)}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            SelectionContainer {Text(sense.spanish.ifBlank {sense.definition},style=MaterialTheme.typography.bodyLarge)}
            if(sense.example.isNotBlank()) Text(sense.example,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(original&&sense.spanish.isNotBlank()) Text(sense.definition,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if(entry.senses.size>2) TextButton(onClick={more=!more},contentPadding=PaddingValues(0.dp)) {Text(if(more) "Ver menos significados" else "Ver ${entry.senses.size-2} significados más")}
    if(entry.senses.any {it.spanish.isNotBlank()}) TextButton(onClick={original=!original},contentPadding=PaddingValues(0.dp)) {Text(if(original) "Ocultar original en inglés" else "Ver original en inglés")}
    val related=entry.relatedWords.filter {it!=entry.lemma&&it!=entry.word}
    if(entry.language=="es"&&related.isNotEmpty()) {
        Text("Palabras de la definición",style=MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {related.forEach {word ->
            SuggestionChip(onClick={onRelated(word)},label={Text("Consultar «$word»")})
        }}
    }
    Column {
        Text("Autores de ${if(entry.language=="es") "Wikcionario" else "Wiktionary"}${if(entry.cached) " · guardado en el dispositivo" else ""}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(entry.senses.any {it.spanish.isNotBlank()}) Text("Definiciones traducidas automáticamente.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        entry.sources.forEachIndexed {index,url -> TextButton(onClick={uri.openUri(url)},contentPadding=PaddingValues(0.dp)) {Text(if(index==0) "Ver entrada original" else "Ver entrada de «${entry.lemma}»")}}
        entry.licenses.forEach {license -> TextButton(onClick={uri.openUri(license.url)},contentPadding=PaddingValues(0.dp)) {Text(license.name,style=MaterialTheme.typography.bodySmall)}}
    }
}

private fun partOfSpeech(part: String)=when(part) {
    "noun"->"Sustantivo";"verb"->"Verbo";"adjective"->"Adjetivo";"adverb"->"Adverbio";"pronoun"->"Pronombre";"preposition"->"Preposición";"conjunction"->"Conjunción";"interjection","exclamation"->"Interjección";"determiner"->"Determinante";else->part.ifBlank {"Uso"}
}

@Composable private fun TranslationCredit(source: String) {
    val uri=LocalUriHandler.current
    if(source.startsWith("Google Translate")) {
        Column {
            Image(painterResource(if(MaterialTheme.colorScheme.surface.luminance()<.5f) R.drawable.google_translate_dark else R.drawable.google_translate_light),"Powered by Google Translate",Modifier.width(150.dp).height(48.dp).clickable {uri.openUri("https://translate.google.com")})
            Text("Traducción automática en el dispositivo",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else Text(source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
}
