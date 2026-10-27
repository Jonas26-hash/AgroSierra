package pe.upeu.agrosierra

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.TipoLabor

class SyncYErroresTest {
    @Test fun laborPendienteTieneIdNegativo() {
        val labor = Labor(-5, 1, TipoLabor.FUMIGACION, "2026-10-27", "tecnico1", 2.0, "dosis", true)
        assertTrue(labor.id < 0)
        assertTrue(labor.pendiente)
    }

    @Test fun errorApiConservaMensaje() {
        val err = ErrorApi.NoAutorizado("sin permiso")
        assertEquals("sin permiso", err.message)
    }

    @Test fun tipoLaborIncluyeFumigacion() {
        assertTrue(TipoLabor.entries.any { it.name == "FUMIGACION" })
        assertEquals(3, TipoLabor.entries.size)
    }

    @Test fun identificadoresRealesSonPositivos() {
        val labor = Labor(100, 1, TipoLabor.RIEGO, "2026-10-27", "jefe1", 5.0, "", false)
        assertTrue(labor.id > 0)
        assertEquals(false, labor.pendiente)
    }
}