package app.lectio.reader.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NavigateNext
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import app.lectio.reader.BuildConfig
import app.lectio.reader.data.Preferences

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(p: Preferences, translationReady: Boolean, downloading: Boolean, save: ((Preferences) -> Preferences) -> Unit, download: (Boolean) -> Unit, initialPage: String? = null, onExit: (() -> Unit)? = null) {
    var page by rememberSaveable(initialPage) {mutableStateOf(initialPage)}
    BackHandler(enabled=page!=null||onExit!=null) {if(onExit!=null) onExit() else page=null}
    val uri=LocalUriHandler.current
    Scaffold(topBar={TopAppBar(title={Text(page ?: "Ajustes",style=if(page==null) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis)},navigationIcon={if(page!=null||onExit!=null) IconButton(onClick={if(onExit!=null) onExit() else page=null}) {Icon(Icons.AutoMirrored.Outlined.ArrowBack,if(onExit!=null) "Volver al libro" else "Volver a Ajustes")}})},contentWindowInsets=WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            when(page) {
                null -> {
                    Text("Apariencia",style=MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Sistema","Claro","Oscuro").forEach {theme -> FilterChip(selected=p.theme==theme,onClick={save {current -> current.copy(theme=theme)}},label={Text(theme)})}}
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                    SettingsLink("Lectura","Letra, página y controles",Icons.Outlined.TextFields) {page="Lectura"}
                    SettingsLink("Diccionario y traducción",if(translationReady) "Español descargado" else "Idiomas y consultas",Icons.Outlined.MenuBook) {page="Diccionario y traducción"}
                    SettingsLink("Gemini",if(p.key.isBlank()) "Explicaciones opcionales" else "Cuenta conectada",Icons.Outlined.Tune) {page="Gemini"}
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                    Text("Lectio ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.titleSmall)
                    Text("Los libros y el vocabulario se guardan en este dispositivo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "Lectura" -> ReadingPreferences(p,save)
                "Diccionario y traducción" -> {
                    Text("Significado de palabras",style=MaterialTheme.typography.titleMedium)
                    PreferenceSwitch("Consultar el diccionario","Busca la palabra en Wiktionary. No envía la oración ni usa tu saldo de Gemini.",p.dictionaryEnabled) {value -> save {current -> current.copy(dictionaryEnabled=value)}}
                    Text("Las entradas consultadas quedan guardadas para leerlas sin conexión. Las palabras nuevas necesitan internet; no todas tienen una entrada.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick={uri.openUri("https://en.wiktionary.org/wiki/Wiktionary:Main_Page")},contentPadding=PaddingValues(0.dp)) {Text("Acerca del diccionario")}
                    HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
                    Text("Traducción sin conexión",style=MaterialTheme.typography.titleMedium)
                    Text(if(translationReady) "Inglés → español · listo" else "Descarga español para traducir el libro y los significados en este dispositivo.",style=MaterialTheme.typography.bodyLarge)
                    if(!translationReady) {
                        Button(onClick={download(true)},enabled=!downloading,modifier=Modifier.fillMaxWidth()) {Text(if(downloading) "Descargando idioma…" else "Descargar español por Wi-Fi")}
                        if(downloading) LinearProgressIndicator(Modifier.fillMaxWidth()) else TextButton(onClick={download(false)}) {Text("Descargar usando datos móviles")}
                        Text("Alrededor de 30 MB. Sin clave ni cobros por consulta.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick={uri.openUri("https://cloud.google.com/translate")},contentPadding=PaddingValues(0.dp)) {Text("Google Translate")}
                }
                "Gemini" -> GeminiSettings(p,save)
            }
        }
    }
}

@Composable private fun SettingsLink(title: String,summary: String,icon: ImageVector,open: () -> Unit) {
    ListItem(modifier=Modifier.fillMaxWidth().clickable(onClick=open),headlineContent={Text(title)},supportingContent={Text(summary)},leadingContent={Icon(icon,null)},trailingContent={Icon(Icons.AutoMirrored.Outlined.NavigateNext,null)},colors=ListItemDefaults.colors(containerColor=MaterialTheme.colorScheme.background))
}

@Composable internal fun PreferenceSwitch(title: String,summary: String,checked: Boolean,change: (Boolean) -> Unit) {
    // A single switch owns both the row's semantics and its full touch area.
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).toggleable(value=checked,role=Role.Switch,onValueChange=change),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {Text(title,style=MaterialTheme.typography.bodyLarge);Text(summary,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Switch(checked=checked,onCheckedChange=null)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun GeminiSettings(p: Preferences,save: ((Preferences) -> Preferences) -> Unit) {
    var connecting by remember {mutableStateOf(false)}
    var advanced by remember {mutableStateOf(false)}
    var provider by remember(p.provider,p.key.isBlank()) {mutableStateOf(if(p.key.isBlank()) "gemini" else p.provider)}
    var endpoint by remember(p.endpoint,p.key.isBlank()) {mutableStateOf(if(p.key.isBlank()) Preferences().endpoint else p.endpoint)}
    var model by remember(p.model,p.key.isBlank()) {mutableStateOf(if(p.key.isBlank()) Preferences().model else p.model)}
    var key by remember(p.key) {mutableStateOf(p.key)}
    var showKey by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    val uri=LocalUriHandler.current
    Text(if(p.key.isBlank()) "Explicaciones opcionales" else "Cuenta conectada",style=MaterialTheme.typography.titleMedium)
    Text("Para entender expresiones y gramática en su contexto. El diccionario y la traducción funcionan sin conectar una cuenta.",style=MaterialTheme.typography.bodyMedium)
    OutlinedButton(onClick={connecting=!connecting},modifier=Modifier.fillMaxWidth()) {Text(if(connecting) "Cerrar configuración" else if(p.key.isBlank()) "Conectar Gemini" else "Gestionar conexión")}
    if(connecting) {
        Text("Pega la clave de tu cuenta de Google AI Studio. Se guarda cifrada en este dispositivo.",style=MaterialTheme.typography.bodyMedium)
        TextButton(onClick={uri.openUri("https://aistudio.google.com/apikey")}) {Text("Abrir Google AI Studio")}
        OutlinedTextField(key,{key=it;error=null},Modifier.fillMaxWidth(),label={Text(if(provider=="gemini") "Clave de Gemini" else "Clave del proveedor")},visualTransformation=if(showKey) VisualTransformation.None else PasswordVisualTransformation(),singleLine=true,trailingIcon={IconButton(onClick={showKey=!showKey}) {Icon(if(showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(showKey) "Ocultar clave" else "Mostrar clave")}})
        Text("Solo se envían la selección y su oración al pedir una explicación. Puede consumir saldo; se guardan las respuestas para no repetir consultas.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick={uri.openUri("https://ai.google.dev/gemini-api/docs/pricing")}) {Text("Ver precios de Gemini")}
        TextButton(onClick={advanced=!advanced}) {Text(if(advanced) "Ocultar opciones avanzadas" else "Opciones avanzadas")}
        if(advanced) {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("gemini" to "Gemini","compatible" to "Otro proveedor").forEach {(id,label) -> FilterChip(selected=provider==id,onClick={if(provider!=id){provider=id;key="";model=if(id=="gemini") Preferences().model else "";endpoint=if(id=="gemini") Preferences().endpoint else "https://api.openai.com/v1"}},label={Text(label)})}}
            OutlinedTextField(model,{model=it},Modifier.fillMaxWidth(),label={Text("Modelo")},singleLine=true)
            if(provider!="gemini") OutlinedTextField(endpoint,{endpoint=it},Modifier.fillMaxWidth(),label={Text("Dirección del proveedor")},singleLine=true)
        }
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={
            val valid=if(provider=="gemini") Regex("[a-zA-Z0-9._-]+").matches(model.trim()) else runCatching {java.net.URI(endpoint.trim()).let {it.scheme=="https"&&it.host!=null&&it.userInfo==null&&it.query==null&&it.fragment==null}}.getOrDefault(false)&&model.isNotBlank()
            if(key.isBlank()) error="Pega tu clave para conectar la cuenta."
            else if(!valid) error="Revisa el modelo y la dirección en opciones avanzadas."
            else {save {current -> current.copy(provider=provider,endpoint=endpoint.trim().trimEnd('/'),model=model.trim(),key=key.trim())};connecting=false;error=null}
        },Modifier.fillMaxWidth()) {Text("Guardar conexión")}
    }
    if(p.key.isNotBlank()) TextButton(onClick={key="";save {current -> current.copy(key="")};connecting=false},Modifier.fillMaxWidth()) {Text("Desconectar y borrar clave")}
}
