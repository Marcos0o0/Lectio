package app.lectio.reader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.lectio.reader.data.Preferences

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(p: Preferences, dictionarySize: Int, save: (Preferences) -> Unit) {
    // API credentials intentionally stay in non-saveable memory while editing.
    var endpoint by remember(p.endpoint) {mutableStateOf(p.endpoint)}
    var model by remember(p.model) {mutableStateOf(p.model)}
    var key by remember(p.key) {mutableStateOf(p.key)}
    var showKey by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    var saved by remember {mutableStateOf(false)}
    Scaffold(topBar={TopAppBar(title={Text("Ajustes",style=MaterialTheme.typography.headlineMedium)})},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            SectionHeading("A tu manera")
            Text("Apariencia",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Sistema","Claro","Oscuro").forEach {theme -> FilterChip(selected=p.theme==theme,onClick={save(p.copy(theme=theme))},label={Text(theme)})}}
            ReadingPreferences(p,save)
            HorizontalDivider(Modifier.padding(vertical=12.dp),color=MaterialTheme.colorScheme.outlineVariant)
            SectionHeading("Una ayuda, si la necesitas")
            Text("La lectura, el diccionario inicial ($dictionarySize entradas) y las consultas ya guardadas funcionan sin conexión. La IA amplía las traducciones y explica fragmentos.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(endpoint,{endpoint=it;saved=false},Modifier.fillMaxWidth(),label={Text("URL base de la API")},placeholder={Text("https://api.openai.com/v1")},singleLine=true)
            OutlinedTextField(model,{model=it;saved=false},Modifier.fillMaxWidth(),label={Text("Modelo")},singleLine=true)
            OutlinedTextField(key,{key=it;saved=false},Modifier.fillMaxWidth(),label={Text("Tu clave de API")},visualTransformation=if(showKey) VisualTransformation.None else PasswordVisualTransformation(),singleLine=true,trailingIcon={IconButton(onClick={showKey=!showKey}) {Icon(if(showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(showKey) "Ocultar clave" else "Mostrar clave")}})
            Text("Compatible con APIs de chat completions. La clave se cifra con Android Keystore. Solo al pulsar una acción de IA se envían el fragmento y su contexto al proveedor configurado; puede tener coste.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let {Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodyMedium)}
            Button(onClick={
                val valid=runCatching {java.net.URI(endpoint.trim()).let {it.scheme=="https"&&it.host!=null&&it.userInfo==null&&it.query==null&&it.fragment==null}}.getOrDefault(false)
                if(!valid||model.isBlank()) error="Introduce una URL HTTPS válida y el nombre del modelo."
                else {error=null;save(p.copy(endpoint=endpoint.trim().trimEnd('/'),model=model.trim(),key=key.trim()));saved=true}
            },Modifier.fillMaxWidth()) {Text(if(saved) "Configuración guardada" else "Guardar configuración de IA")}
            if(p.key.isNotBlank()) TextButton(onClick={key="";save(p.copy(key=""));saved=false},Modifier.fillMaxWidth()) {Text("Eliminar clave del dispositivo")}
            HorizontalDivider(Modifier.padding(vertical=12.dp),color=MaterialTheme.colorScheme.outlineVariant)
            Text("Lectio",style=MaterialTheme.typography.headlineMedium)
            Text("Versión 0.1.0 · Un espacio para leer y aprender.\nEPUB sin DRM · Android 8 o posterior",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tus libros, anotaciones y vocabulario se guardan localmente. Al desinstalar la app se eliminan. No hay cuenta ni sincronización en esta versión.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun ReadingPreferences(p: Preferences, save: (Preferences) -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Tipografía de lectura",style=MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(selected=p.serif,onClick={save(p.copy(serif=true))},label={Text("Serif")});FilterChip(selected=!p.serif,onClick={save(p.copy(serif=false))},label={Text("Sans serif")})}
        Text("Tamaño de letra · ${p.fontSize}",style=MaterialTheme.typography.bodyMedium)
        var size by remember(p.fontSize) {mutableFloatStateOf(p.fontSize.toFloat())}
        Slider(value=size,onValueChange={size=it},onValueChangeFinished={save(p.copy(fontSize=size.toInt()))},valueRange=16f..32f,steps=15)
        Text("Interlineado · ${"%.1f".format(p.lineHeight)}",style=MaterialTheme.typography.bodyMedium)
        var line by remember(p.lineHeight) {mutableFloatStateOf(p.lineHeight)}
        Slider(value=line,onValueChange={line=it},onValueChangeFinished={save(p.copy(lineHeight=line))},valueRange=1.4f..2.2f,steps=7)
    }
}
