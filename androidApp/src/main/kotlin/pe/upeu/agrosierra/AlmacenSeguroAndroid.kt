package pe.upeu.agrosierra

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.upeu.agrosierra.data.local.AlmacenSeguro

actual class AlmacenSeguro(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "agrosierra_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    actual suspend fun guardarToken(token: String) {
        prefs.edit().putString("token", token).apply()
    }

    actual suspend fun obtenerToken(): String? = prefs.getString("token", null)

    actual suspend fun borrarToken() {
        prefs.edit().remove("token").apply()
    }

    actual suspend fun guardarSesion(usuario: String, rol: String, privilegios: Set<String>) {
        prefs.edit().putString("usuario", usuario)
            .putString("rol", rol)
            .putStringSet("privilegios", privilegios)
            .apply()
    }

    actual suspend fun obtenerSesion(): Triple<String?, String?, Set<String>>? {
        val usuario = prefs.getString("usuario", null)
        val rol = prefs.getString("rol", null)
        val privs = prefs.getStringSet("privilegios", emptySet()) ?: emptySet()
        if (usuario == null) return null
        return Triple(usuario, rol, privs)
    }

    actual suspend fun borrarSesion() {
        prefs.edit().remove("usuario").remove("rol").remove("privilegios").apply()
    }
}
