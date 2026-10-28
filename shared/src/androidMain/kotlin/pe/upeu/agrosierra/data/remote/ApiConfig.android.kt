package pe.upeu.agrosierra.data.remote

import android.os.Build

actual fun platformApiBaseUrl(): String {
    val emulator = Build.FINGERPRINT.startsWith("generic") ||
        Build.MODEL.contains("Emulator", ignoreCase = true) ||
        Build.PRODUCT.contains("sdk", ignoreCase = true)
    return if (emulator) "http://10.0.2.2:8080/" else "http://127.0.0.1:8080/"
}
