package pe.upeu.agrosierra

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import pe.upeu.agrosierra.data.remote.dto.LoteDto
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.TipoLabor

class DominioTest {
    @Test fun loteConservaLosDatosAlMapearse() {
        val dto = LoteDto(7, "LOT-007", "Lote Alto", 2.5, "Papa", "Valle Norte", false, 1.5)
        val model = Lote(dto.id, dto.codigo, dto.nombre, dto.hectareas, dto.cultivo, dto.valle, dto.campaniaCerrada, dto.dosisMaximaPorHectarea)
        assertEquals("LOT-007", model.codigo)
        assertEquals("Valle Norte", model.valle)
        assertEquals(1.5, model.dosisMaximaPorHectarea)
    }

    @Test fun identificadoresNegativosIdentificanPendientes() {
        val pending = Labor(-1, 2, TipoLabor.RIEGO, "2026-10-27", "tecnico1", 5.0, "Turno mañana", true)
        assertTrue(pending.id < 0)
        assertTrue(pending.pendiente)
    }

    @Test fun laborConfirmadaNoQuedaPendiente() {
        val saved = Labor(42, 2, TipoLabor.COSECHA, "2026-10-27", "jefe1", 30.0, "Cosecha", false)
        assertFalse(saved.pendiente)
        assertEquals(42L, saved.id)
    }

    @Test fun enumeracionDefineLosTresTiposDeLabor() {
        assertEquals(listOf("RIEGO", "FUMIGACION", "COSECHA"), TipoLabor.entries.map { it.name })
    }
}
