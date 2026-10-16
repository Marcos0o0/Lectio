package app.lectio.reader.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lectio.reader.ReaderState
import app.lectio.reader.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun LookupSheet(vm: ReaderViewModel,state: ReaderState,pronounce: (String) -> Unit,settings: () -> Unit) {
    val selection=state.selection ?: return
    val result=state.lookup
    var manual by remember(selection) {mutableStateOf("")}
    var note by remember(selection) {mutableStateOf("")}
    var annotation by remember(selection) {mutableStateOf(false)}
    val isWord=selection.text.trim().split(Regex("\\s+")).size==1
    val canSave=selection.text.trim().split(Regex("\\s+")).size<=12
    ModalBottomSheet(onDismissRequest=vm::dismissSelection,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(if(isWord) selection.text else "Entre líneas",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                IconButton(onClick={pronounce(selection.text)}) {Icon(Icons.Outlined.VolumeUp,"Escuchar texto en inglés")}
                IconButton(onClick=vm::dismissSelection) {Icon(Icons.Outlined.Close,"Cerrar consulta")}
            }
            if(!isWord) Surface(color=MaterialTheme.colorScheme.surfaceContainer,shape=MaterialTheme.shapes.medium) { SelectionContainer {Text(selection.text,Modifier.padding(16.dp),style=MaterialTheme.typography.bodyMedium,maxLines=7,overflow=TextOverflow.Ellipsis)} }
            if(state.lookupLoading) {Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp);Text("Consultando…")}}
            state.lookupError?.let {Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            if(result!=null) {
                SelectionContainer { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(result.translation,style=if(isWord) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
                    if(result.definition.isNotBlank()) Text(result.definition,style=MaterialTheme.typography.bodyMedium)
                    if(result.example.isNotBlank()) Text("“${result.example}”",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(result.synonyms.isNotBlank()) Text("Sinónimos: ${result.synonyms}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                Text(listOf(result.source,if(result.cached) "Guardado sin conexión" else null,result.level.takeIf {it.isNotBlank()}?.let {"CEFR orientativo: $it"}).filterNotNull().joinToString(" · "),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
                if(result.source.startsWith("IA")) Text("Respuesta generada por IA; puede contener errores. Las explicaciones son interpretaciones, no palabras del autor.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(result==null&&!state.lookupLoading&&canSave) OutlinedTextField(manual,{manual=it},Modifier.fillMaxWidth(),label={Text("Añadir mi propia traducción")},minLines=1,maxLines=4)
            if(canSave) Button(onClick={vm.saveWord(manual.takeIf {it.isNotBlank()})},Modifier.fillMaxWidth(),enabled=!state.lookupLoading&&(result!=null||manual.isNotBlank())) {Icon(Icons.Outlined.BookmarkAdd,null);Spacer(Modifier.width(8.dp));Text(if(state.words.any {it.text.equals(selection.text,true)}) "Actualizar vocabulario" else "Guardar en vocabulario")}
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            Text("Una segunda mirada",style=MaterialTheme.typography.titleMedium)
            if(state.preferences.key.isBlank()) {
                Text("Conecta tu API en Ajustes para traducir frases o pedir una explicación.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick=settings,Modifier.fillMaxWidth()) {Text("Configurar IA opcional")}
            } else {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("translate" to "Traducir con IA","explain" to "Explicar","simplify" to "Inglés sencillo","grammar" to "Gramática").forEach { (action,label) -> AssistChip(onClick={vm.query(action)},enabled=!state.lookupLoading,label={Text(label)}) }
                }
                Text("Estas acciones envían el fragmento y su contexto a tu proveedor de IA, salvo que haya una respuesta en caché.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick={annotation=!annotation},contentPadding=PaddingValues(0.dp)) {Icon(Icons.Outlined.BorderColor,null);Spacer(Modifier.width(8.dp));Text("Subrayar y añadir nota")}
            if(annotation) {
                OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("Nota (opcional)")},maxLines=4)
                FilledTonalButton(onClick={vm.annotate(selection.text,note,"highlight");vm.dismissSelection()},Modifier.fillMaxWidth()) {Text("Guardar subrayado")}
            }
        }
    }
}
