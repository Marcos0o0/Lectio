package app.lectio.reader.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private fun secret(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("lectio-api", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("lectio-api", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun encrypt(text: String): String {
        if (text.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, secret()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(text.toByteArray()), Base64.NO_WRAP)
    }
    private fun decrypt(text: String): String = runCatching {
        if(text.isEmpty()) "" else Base64.decode(text, Base64.NO_WRAP).let { bytes ->
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(128, bytes.copyOfRange(0,12))) }
            String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
        }
    }.getOrDefault("")
    fun load(): Preferences {
        val defaults=Preferences()
        val endpoint=prefs.getString("endpoint",defaults.endpoint)!!
        val provider=prefs.getString("provider",if(endpoint.contains("generativelanguage.googleapis.com")) "gemini" else "compatible")!!
        return Preferences(prefs.getString("theme", "Oscuro")!!,prefs.getInt("fontSize",20),prefs.getFloat("lineHeight",1.8f),prefs.getBoolean("serif",true),endpoint,prefs.getString("model",defaults.model)!!,decrypt(prefs.getString("key","")!!),provider)
    }
    fun save(p: Preferences) { prefs.edit().putString("theme",p.theme).putInt("fontSize",p.fontSize).putFloat("lineHeight",p.lineHeight).putBoolean("serif",p.serif).putString("endpoint",p.endpoint.trim().trimEnd('/')).putString("model",p.model.trim()).putString("key",encrypt(p.key.trim())).putString("provider",p.provider).apply() }
    var seeded: Boolean
        get() = prefs.getBoolean("seeded",false)
        set(value) { prefs.edit().putBoolean("seeded",value).apply() }
}
