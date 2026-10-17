package app.lectio.reader.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lectio.reader.data.Preferences

@Composable internal fun readingColors(p: Preferences): ColorScheme = when(p.readingTheme) {
    "Claro" -> LectioLight
    "Oscuro" -> LectioDark
    "Papel" -> LectioPaper
    else -> MaterialTheme.colorScheme
}

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun ReadingPreferences(p: Preferences, save: ((Preferences) -> Preferences) -> Unit) {
    var tab by remember {mutableIntStateOf(0)}
    Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
        PrimaryTabRow(selectedTabIndex=tab) {listOf("Texto","Página","Controles").forEachIndexed {i,title -> Tab(selected=tab==i,onClick={tab=i},text={Text(title)})}}
        val colors=readingColors(p)
        Surface(color=colors.background,contentColor=colors.onBackground,shape=MaterialTheme.shapes.medium,border=androidx.compose.foundation.BorderStroke(1.dp,colors.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(horizontal=p.margin.dp,vertical=20.dp)) {
                Text("Vista previa",style=MaterialTheme.typography.labelMedium,color=colors.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("For a few precious minutes, the world could wait.",style=MaterialTheme.typography.bodyLarge.copy(fontFamily=if(p.serif) Literary else FontFamily.SansSerif,fontSize=p.fontSize.sp,lineHeight=(p.fontSize*p.lineHeight).sp,textAlign=if(p.justified) TextAlign.Justify else TextAlign.Start))
                Spacer(Modifier.height((p.fontSize*p.paragraphSpacing).dp))
                Text("I opened my book and began to read.",style=MaterialTheme.typography.bodyLarge.copy(fontFamily=if(p.serif) Literary else FontFamily.SansSerif,fontSize=p.fontSize.sp,lineHeight=(p.fontSize*p.lineHeight).sp,textAlign=if(p.justified) TextAlign.Justify else TextAlign.Start))
            }
        }
        when(tab) {
            0 -> {
                Text("Tipografía",style=MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {FilterChip(selected=p.serif,onClick={save {current -> current.copy(serif=true)}},label={Text("Literata")});FilterChip(selected=!p.serif,onClick={save {current -> current.copy(serif=false)}},label={Text("Sin serifas")})}
                Text("Tamaño de letra · ${p.fontSize}",style=MaterialTheme.typography.bodyLarge)
                var size by remember(p.fontSize) {mutableFloatStateOf(p.fontSize.toFloat())}
                Slider(value=size,onValueChange={size=it},onValueChangeFinished={save {current -> current.copy(fontSize=size.toInt())}},valueRange=16f..32f,steps=15,modifier=Modifier.semantics {contentDescription="Tamaño de letra"})
                Text("Interlineado · ${"%.1f".format(p.lineHeight)}",style=MaterialTheme.typography.bodyLarge)
                var line by remember(p.lineHeight) {mutableFloatStateOf(p.lineHeight)}
                Slider(value=line,onValueChange={line=it},onValueChangeFinished={save {current -> current.copy(lineHeight=line)}},valueRange=1.4f..2.2f,steps=7,modifier=Modifier.semantics {contentDescription="Interlineado"})
            }
            1 -> {
                Text("Fondo del libro",style=MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Aplicación","Papel","Claro","Oscuro").forEach {theme -> FilterChip(selected=p.readingTheme==theme,onClick={save {current -> current.copy(readingTheme=theme)}},label={Text(if(theme=="Aplicación") "Como la app" else theme)})}}
                Text("Papel usa un fondo crema suave para leer con menos blanco en pantalla.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Márgenes laterales",style=MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf(12 to "Estrechos",28 to "Normales",44 to "Amplios").forEach {(value,label) -> FilterChip(selected=p.margin==value,onClick={save {current -> current.copy(margin=value)}},label={Text(label)})}}
                Text("Entre párrafos",style=MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf(.5f to "Poco",1f to "Normal",1.5f to "Más").forEach {(value,label) -> FilterChip(selected=p.paragraphSpacing==value,onClick={save {current -> current.copy(paragraphSpacing=value)}},label={Text(label)})}}
                PreferenceSwitch("Texto justificado","Alinea ambos bordes de los párrafos.",p.justified) {value -> save {current -> current.copy(justified=value)}}
            }
            2 -> {
                PreferenceSwitch("Consultar al tocar","Un toque abre la palabra. Si lo desactivas, mantén pulsado para consultar.",p.tapToTranslate) {value -> save {current -> current.copy(tapToTranslate=value)}}
                PreferenceSwitch("Mantener pantalla encendida","Mientras el libro esté abierto.",p.keepScreenOn) {value -> save {current -> current.copy(keepScreenOn=value)}}
                PreferenceSwitch("Mostrar ayuda de gestos","Recuerda cómo pasar páginas y consultar palabras.",p.showReadingHint) {value -> save {current -> current.copy(showReadingHint=value)}}
            }
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        TextButton(onClick={save {current -> current.copy(fontSize=20,lineHeight=1.8f,serif=true,readingTheme="Aplicación",margin=28,paragraphSpacing=1f,justified=false,keepScreenOn=false,tapToTranslate=true,showReadingHint=true)}},modifier=Modifier.fillMaxWidth()) {Text("Restablecer lectura")}
    }
}
