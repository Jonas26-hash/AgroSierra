package pe.upeu.agrosierra

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.upeu.agrosierra.AgroDb
import pe.upeu.agrosierra.data.local.BaseLocal

class BaseLocalAndroid(private val context: Context) {
    fun crearDriver() = AndroidSqliteDriver(AgroDb.Schema, context, "agro.db")
    fun crearBaseLocal(): BaseLocalAndroidDb {
        val driver = crearDriver()
        return BaseLocalAndroidDb(BaseLocal(driver), driver)
    }
}

data class BaseLocalAndroidDb(val base: BaseLocal)
