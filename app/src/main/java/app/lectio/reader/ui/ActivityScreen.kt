package app.lectio.reader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.lectio.reader.ReaderState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ActivityScreen(state: ReaderState) {
    val today=LocalDate.now()
    val days=(6 downTo 0).map {today.minusDays(it.toLong())}
    val seconds=state.sessions.values.sum()
    val week=days.sumOf {state.sessions[it.toString()] ?: 0}
    val max=days.maxOf {state.sessions[it.toString()] ?: 0}.coerceAtLeast(60)
    Scaffold(topBar={TopAppBar(title={Text("Tu actividad",style=MaterialTheme.typography.headlineMedium)})},contentWindowInsets=WindowInsets(0,0,0,0)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(28.dp)) {
            Column { Text("Cada página cuenta.",style=MaterialTheme.typography.headlineLarge);Spacer(Modifier.height(12.dp));Text("Tu tiempo de lectura y lo que vas aprendiendo, sin prisa.",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Esta semana",style=MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp));Text("${week/60} minutos de lectura",style=MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(28.dp))
                    Row(Modifier.fillMaxWidth().height(154.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.Bottom) {
                        days.forEach { day -> val value=state.sessions[day.toString()] ?: 0
                            Column(Modifier.weight(1f).semantics {contentDescription="${day}: ${value/60} minutos"},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
                                Text("${value/60}",style=MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp));Box(Modifier.fillMaxWidth(.65f).height((if(value==0) 3f else 12+100f*value/max).dp).background(if(day==today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha=.45f),MaterialTheme.shapes.small))
                                Spacer(Modifier.height(8.dp));Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW,Locale.forLanguageTag("es")),style=MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            SectionHeading("Lo que llevas contigo")
            val rows=listOf("Tiempo de lectura" to "${seconds/60} min", "Días con lectura" to "${state.sessions.count {it.value>0}}", "Libros terminados" to "${state.books.count {it.progress>=.995f}}", "Palabras guardadas" to "${state.words.size}", "Palabras aprendidas" to "${state.words.count {it.learned}}")
            rows.forEach { (label,value) -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(label,style=MaterialTheme.typography.bodyLarge);Text(value,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)} }
            Text("El tiempo se registra mientras el lector está abierto y la app está en primer plano. Los datos permanecen en este dispositivo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
