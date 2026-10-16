package app.lectio.reader.data

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationModelRequired : IllegalStateException("Descarga el idioma español para traducir sin conexión.")

/** The language model, rather than a word list, translates arbitrary book text. */
class LocalTranslation : AutoCloseable {
    private val translatorDelegate=lazy {
        Translation.getClient(TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(TranslateLanguage.SPANISH).build())
    }
    private val translator get()=translatorDelegate.value
    suspend fun ready(): Boolean = RemoteModelManager.getInstance()
        .getDownloadedModels(TranslateRemoteModel::class.java).resultAwait()
        .any { it.language==TranslateLanguage.SPANISH }

    suspend fun download(wifiOnly: Boolean) {
        val conditions=DownloadConditions.Builder().apply {if(wifiOnly) requireWifi()}.build()
        translator.downloadModelIfNeeded(conditions).resultAwait()
    }

    suspend fun lookup(selection: Selection): Lookup {
        if(!ready()) throw TranslationModelRequired()
        val translated=translator.translate(selection.text).resultAwait()
        val context=selection.context.take(1200).trim()
        val contextTranslation=if(context.isNotBlank()&&context!=selection.text.trim()) translator.translate(context).resultAwait() else ""
        return Lookup(selection.text,translated,contextTranslation=contextTranslation)
    }
    override fun close() {if(translatorDelegate.isInitialized()) translator.close()}
}

private suspend fun <T> Task<T>.resultAwait(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener {if(continuation.isActive) continuation.resume(it)}
    addOnFailureListener {if(continuation.isActive) continuation.resumeWithException(it)}
    addOnCanceledListener {continuation.cancel()}
}
