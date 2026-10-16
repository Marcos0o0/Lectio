package app.lectio.reader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.lectio.reader.BuildConfig
import app.lectio.reader.data.Preferences

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun SettingsScreen(p: Preferences, translationReady: Boolean, downloading: Boolean, save: (Preferences) -> Unit, download: (Boolean) -> Unit) {
    var connecting by remember {mutableStateOf(false)}
    var advanced by remember {mutableStateOf(false)}
    var provider by remember(p.provider,p.key.isBlank()) {mutableStateOf(if(p.key.isBlank()) "gemini" else p.provider)}
    var endpoint by remember(p.endpoint,p.key.isBlank()) {mutableStateOf(if(p.key.isBlank()) Preferences().endpoint else p.endpoint)}
    var model by remember(p.model,p.key.isBlank()) {mutableStateOf(if(p.key.isBlank()) Preferences().model else p.model)}
    // Credentials stay out of saved instance state and source control.
    var key by remember(p.key) {mutableStateOf(p.key)}
    var showKey by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    val uriHandler=LocalUriHandler.current
    Scaffold(topBar={TopAppBar(title={Text("Ajustes",style=MaterialTheme.typography.headlineMedium)})},contentWindowInsets=WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text("Apariencia",style=MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Sistema","Claro","Oscuro").forEach {theme -> FilterChip(selected=p.theme==theme,onClick={save(p.copy(theme=theme))},label={Text(theme)})}}
            ReadingPreferences(p,save)
            HorizontalDivider(Modifier.padding(vertical=8.dp),color=MaterialTheme.colorScheme.outlineVariant)
            SectionHeading("Traducción")
            Text(if(translationReady) "Inglés → español · disponible sin conexión" else "Descarga español una vez para traducir palabras y frases en el teléfono.",style=MaterialTheme.typography.bodyMedium)
            if(!translationReady) {
                Button(onClick={download(true)},enabled=!downloading,modifier=Modifier.fillMaxWidth()) {Text(if(downloading) "Descargando idioma…" else "Descargar español por Wi-Fi")}
                if(downloading) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Alrededor de 30 MB. Sin clave ni cobro por consulta.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(Modifier.padding(vertical=8.dp),color=MaterialTheme.colorScheme.outlineVariant)
            SectionHeading(if(p.key.isBlank()||p.provider=="gemini") "Explicaciones con Gemini" else "Explicaciones con IA")
            Text(if(p.key.isBlank()) "Opcional. Para explicar expresiones, usos y gramática. Traducir en el teléfono no lo necesita." else "Conectado. Solo se usa cuando pides una explicación.",style=MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick={connecting=!connecting},modifier=Modifier.fillMaxWidth()) {Text(if(connecting) "Cerrar configuración" else if(p.key.isBlank()) "Conectar Gemini" else "Gestionar conexión")}
            if(connecting) {
                Text("Para uso personal, pega la clave de tu cuenta de Google AI Studio. Se guarda cifrada en este dispositivo.",style=MaterialTheme.typography.bodyMedium)
                TextButton(onClick={uriHandler.openUri("https://aistudio.google.com/apikey")}) {Text("Abrir Google AI Studio")}
                OutlinedTextField(key,{key=it;error=null},Modifier.fillMaxWidth(),label={Text(if(provider=="gemini") "Clave de Gemini" else "Clave del proveedor")},visualTransformation=if(showKey) VisualTransformation.None else PasswordVisualTransformation(),singleLine=true,trailingIcon={IconButton(onClick={showKey=!showKey}) {Icon(if(showKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,if(showKey) "Ocultar clave" else "Mostrar clave")}})
                Text("Cada petición envía solo la selección y su oración de contexto. Puede consumir saldo de tu cuenta. Lectio guarda las respuestas para no repetir la misma consulta.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick={uriHandler.openUri("https://ai.google.dev/gemini-api/docs/pricing")}) {Text("Ver precios de Gemini")}
                TextButton(onClick={advanced=!advanced}) {Text(if(advanced) "Ocultar opciones avanzadas" else "Opciones avanzadas")}
                if(advanced) {
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf("gemini" to "Gemini","compatible" to "Otro proveedor").forEach { (id,label) -> FilterChip(selected=provider==id,onClick={if(provider!=id){provider=id;key="";model=if(id=="gemini") Preferences().model else "";endpoint=if(id=="gemini") Preferences().endpoint else "https://api.openai.com/v1"}},label={Text(label)}) }
                    }
                    OutlinedTextField(model,{model=it},Modifier.fillMaxWidth(),label={Text("Modelo")},singleLine=true)
                    if(provider!="gemini") OutlinedTextField(endpoint,{endpoint=it},Modifier.fillMaxWidth(),label={Text("Dirección del proveedor")},singleLine=true)
                }
                error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
                Button(onClick={
                    val valid=if(provider=="gemini") Regex("[a-zA-Z0-9._-]+").matches(model.trim()) else runCatching {java.net.URI(endpoint.trim()).let {it.scheme=="https"&&it.host!=null&&it.userInfo==null&&it.query==null&&it.fragment==null}}.getOrDefault(false)&&model.isNotBlank()
                    if(key.isBlank()) error="Pega tu clave para conectar la cuenta."
                    else if(!valid) error="Revisa el modelo y la dirección en opciones avanzadas."
                    else {save(p.copy(provider=provider,endpoint=endpoint.trim().trimEnd('/'),model=model.trim(),key=key.trim()));connecting=false;error=null}
                },Modifier.fillMaxWidth()) {Text("Guardar conexión")}
            }
            if(p.key.isNotBlank()) TextButton(onClick={key="";save(p.copy(key=""));connecting=false},Modifier.fillMaxWidth()) {Text("Desconectar y borrar clave")}
            HorizontalDivider(Modifier.padding(vertical=8.dp),color=MaterialTheme.colorScheme.outlineVariant)
            Text("Lectio ${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.titleMedium)
            Text("Los libros y el vocabulario se guardan en este dispositivo. Desinstalar la app elimina esos datos.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick={uriHandler.openUri("https://cloud.google.com/translate")}) {Text("Traducción automática de Google Translate")}
        }
    }
}

@Composable fun ReadingPreferences(p: Preferences, save: (Preferences) -> Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Tipografía de lectura",style=MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(selected=p.serif,onClick={save(p.copy(serif=true))},label={Text("Serif")});FilterChip(selected=!p.serif,onClick={save(p.copy(serif=false))},label={Text("Sans serif")})}
        Text("Tamaño de letra · ${p.fontSize}",style=MaterialTheme.typography.bodyMedium)
        var size by remember(p.fontSize) {mutableFloatStateOf(p.fontSize.toFloat())}
        Slider(value=size,onValueChange={size=it},onValueChangeFinished={save(p.copy(fontSize=size.toInt()))},valueRange=16f..32f,steps=15)
        Text("Interlineado · ${"%.1f".format(p.lineHeight)}",style=MaterialTheme.typography.bodyMedium)
        var line by remember(p.lineHeight) {mutableFloatStateOf(p.lineHeight)}
        Slider(value=line,onValueChange={line=it},onValueChangeFinished={save(p.copy(lineHeight=line))},valueRange=1.4f..2.2f,steps=7)
    }
}
