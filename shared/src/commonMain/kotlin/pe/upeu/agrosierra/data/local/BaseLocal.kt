package pe.upeu.agrosierra.data.local

import app.cash.sqldelight.db.SqlDriver
import pe.upeu.agrosierra.AgroDb

class BaseLocal(driver: SqlDriver) {
    val db = AgroDb(driver)
}
