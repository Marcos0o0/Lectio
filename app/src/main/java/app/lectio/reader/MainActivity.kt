package app.lectio.reader

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lectio.reader.ui.LectioApp
import app.lectio.reader.ui.LectioTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ReaderViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts=TextToSpeech(this) { status ->
            if(status==TextToSpeech.SUCCESS) {
                val result=tts?.setLanguage(Locale.US)
                ttsReady=result!=TextToSpeech.LANG_MISSING_DATA && result!=TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            LectioTheme(state.preferences.theme) {
                LectioApp(viewModel,state) { text ->
                    if(ttsReady) tts?.speak(text,TextToSpeech.QUEUE_FLUSH,null,"lectio-word")
                    else viewModel.message("Instala una voz de inglés en los ajustes de texto a voz de Android.")
                }
            }
        }
    }
    override fun onDestroy() { tts?.stop();tts?.shutdown();super.onDestroy() }
}
