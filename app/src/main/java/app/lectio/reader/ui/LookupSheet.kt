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

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun LookupSheet(vm: ReaderViewModel,state: ReaderState,pronounce: (String) -> Unit,settings: () -> Unit) {
    val selection=state.selection ?: return
    val result=state.lookup
    var manual by remember(selection) {mutableStateOf("")}
    var note by remember(selection) {mutableStateOf("")}
    var annotation by remember(selection) {mutableStateOf(false)}
    val isWord=selection.text.trim().split(Regex("\\s+")).size==1
    val canSave=selection.text.trim().split(Regex("\\s+")).size<=12
    val uriHandler=LocalUriHandler.current
    ModalBottomSheet(onDismissRequest=vm::dismissSelection,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(if(isWord) selection.text else "Traducción",Modifier.weight(1f),style=MaterialTheme.typography.headlineMedium,maxLines=2,overflow=TextOverflow.Ellipsis)
                IconButton(onClick={pronounce(selection.text)}) {Icon(Icons.Outlined.VolumeUp,"Escuchar texto en inglés")}
                IconButton(onClick=vm::dismissSelection) {Icon(Icons.Outlined.Close,"Cerrar consulta")}
            }
            if(!isWord) SelectionContainer {Text(selection.text,style=MaterialTheme.typography.bodyMedium,maxLines=7,overflow=TextOverflow.Ellipsis)}
            if(state.lookupLoading) Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {CircularProgressIndicator(Modifier.size(22.dp),strokeWidth=2.dp);Text("Traduciendo…")}
            if(state.translationNeedsDownload||state.translationDownloading) {
                Text("Traduce sin conexión",style=MaterialTheme.typography.titleMedium)
                Text("Descarga español una vez (alrededor de 30 MB). Después podrás traducir palabras y frases sin clave ni cobros por consulta.",style=MaterialTheme.typography.bodyMedium)
                Button(onClick={vm.downloadTranslation()},enabled=!state.translationDownloading,modifier=Modifier.fillMaxWidth()) {Text(if(state.translationDownloading) "Descargando idioma…" else "Descargar por Wi-Fi")}
                if(state.translationDownloading) LinearProgressIndicator(Modifier.fillMaxWidth())
                else TextButton(onClick={vm.downloadTranslation(false)},modifier=Modifier.fillMaxWidth()) {Text("Descargar usando datos móviles")}
            }
            state.lookupError?.let {
                Text(it,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.error)
                if(!state.translationNeedsDownload&&!state.translationDownloading) OutlinedButton(onClick={vm.query("translate",false)}) {Text("Reintentar traducción")}
            }
            if(result!=null) {
                SelectionContainer { Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(result.translation,style=if(isWord) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
                    if(result.definition.isNotBlank()) Text(result.definition,style=MaterialTheme.typography.bodyMedium)
                    if(result.synonyms.isNotBlank()) Text("Sinónimos: ${result.synonyms}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(result.contextTranslation.isNotBlank()) {
                        Text("En esta oración",style=MaterialTheme.typography.titleSmall)
                        Text(selection.context,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(result.contextTranslation,style=MaterialTheme.typography.bodyMedium)
                    }
                } }
                if(result.source.startsWith("Google Translate")) {
                    Image(painterResource(if(MaterialTheme.colorScheme.surface.luminance()<.5f) R.drawable.google_translate_dark else R.drawable.google_translate_light),"Powered by Google Translate",Modifier.width(150.dp).height(48.dp).clickable {uriHandler.openUri("https://translate.google.com")})
                    Text("Traducción automática · en el dispositivo",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                } else Text(result.source,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(result.source.startsWith("Gemini")||result.source.startsWith("IA")) Text("Generado con IA. Puede contener errores.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(result!=null&&canSave) Button(onClick={vm.saveWord()},Modifier.fillMaxWidth(),enabled=!state.lookupLoading) {Icon(Icons.Outlined.BookmarkAdd,null);Spacer(Modifier.width(8.dp));Text(if(state.words.any {it.text.equals(selection.text,true)}) "Actualizar vocabulario" else "Guardar en vocabulario")}
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            if(state.preferences.key.isBlank()) {
                OutlinedButton(onClick=settings,Modifier.fillMaxWidth()) {Text("Conectar Gemini para explicar")}
            } else {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("explain" to "Explicar significado","grammar" to "Gramática","simplify" to "Inglés sencillo").forEach { (action,label) -> AssistChip(onClick={vm.query(action)},enabled=!state.lookupLoading,label={Text(label)}) }
                }
                Text("Estas acciones usan tu cuenta y pueden consumir saldo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick={annotation=!annotation},contentPadding=PaddingValues(0.dp)) {Icon(Icons.Outlined.BorderColor,null);Spacer(Modifier.width(8.dp));Text("Subrayar o añadir nota")}
            if(annotation) {
                OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("Nota (opcional)")},maxLines=4)
                FilledTonalButton(onClick={vm.annotate(selection.text,note,"highlight");vm.dismissSelection()},Modifier.fillMaxWidth()) {Text("Guardar subrayado")}
                if(result==null&&canSave) {
                    OutlinedTextField(manual,{manual=it},Modifier.fillMaxWidth(),label={Text("Mi traducción")},maxLines=4)
                    OutlinedButton(onClick={vm.saveWord(manual)},enabled=manual.isNotBlank(),modifier=Modifier.fillMaxWidth()) {Text("Guardar traducción propia")}
                }
            }
        }
    }
}
