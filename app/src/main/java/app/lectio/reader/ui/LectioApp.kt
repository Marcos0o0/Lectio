package app.lectio.reader.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.lectio.reader.ReaderState
import app.lectio.reader.ReaderViewModel

@Composable fun LectioApp(vm: ReaderViewModel, state: ReaderState, pronounce: (String) -> Unit) {
    var destination by rememberSaveable { mutableStateOf(0) }
    var readerSettings by rememberSaveable {mutableStateOf(false)}
    LaunchedEffect(state.current?.id) {if(state.current==null) readerSettings=false}
    val snackbars=remember { SnackbarHostState() }
    val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importBook) }
    val importBook: () -> Unit = { importer.launch(arrayOf("application/epub+zip","application/octet-stream")) }
    LaunchedEffect(vm) { vm.events.collect { snackbars.showSnackbar(it) } }
    BackHandler(enabled=state.current!=null) {
        when {state.selection!=null -> vm.dismissSelection();state.immersive -> vm.immersive(false);else -> vm.closeBook()}
    }
    val labels=listOf("Biblioteca","Vocabulario","Actividad","Ajustes")
    val icons=listOf(Icons.Outlined.AutoStories,Icons.Outlined.Bookmarks,Icons.Outlined.BarChart,Icons.Outlined.Tune)
    BoxWithConstraints {
        val expanded=maxWidth>=600.dp
        Scaffold(
            contentWindowInsets=WindowInsets(0,0,0,0),
            snackbarHost={ if(state.current==null&&destination!=0) SnackbarHost(snackbars) },
            bottomBar={ if(state.current==null && !expanded) NavigationBar(containerColor=MaterialTheme.colorScheme.surface) {
                labels.forEachIndexed { i,label -> NavigationBarItem(selected=destination==i,onClick={destination=i},icon={Icon(icons[i],null)},label={Text(label)},alwaysShowLabel=true) }
            } }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).then(if(state.current==null) Modifier.imePadding() else Modifier)) {
                if(state.current==null && expanded) NavigationRail(Modifier.fillMaxHeight(),containerColor=MaterialTheme.colorScheme.surface) {
                    Spacer(Modifier.height(28.dp))
                    labels.forEachIndexed { i,label -> NavigationRailItem(selected=destination==i,onClick={destination=i},icon={Icon(icons[i],null)},label={Text(label)});Spacer(Modifier.height(12.dp)) }
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if(state.loading) Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
                    else if(state.current!=null) ReaderScreen(vm,state,snackbars)
                    else when(destination) {
                        0 -> LibraryScreen(state,vm::open,importBook,vm::remove,snackbars,vm::favorite) {sort -> vm.preferences {it.copy(librarySort=sort.name)}}
                        1 -> VocabularyScreen(state.words,pronounce,vm::toggleLearned,vm::deleteWord,vm::learn)
                        2 -> ActivityScreen(state)
                        3 -> SettingsScreen(state.preferences,state.translationReady,state.translationDownloading,vm::preferences,vm::downloadTranslation)
                    }
                }
            }
        }
    }
    if(state.selection!=null && state.current!=null) LookupSheet(vm,state,pronounce) {vm.dismissSelection();readerSettings=true}
    if(readerSettings&&state.current!=null) Dialog(onDismissRequest={readerSettings=false},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxSize()) {SettingsScreen(state.preferences,state.translationReady,state.translationDownloading,vm::preferences,vm::downloadTranslation,"Gemini") {readerSettings=false}}
    }
}
