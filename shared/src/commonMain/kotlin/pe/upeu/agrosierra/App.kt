package pe.upeu.agrosierra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.upeu.agrosierra.data.repository.AuthRepositoryImpl
import pe.upeu.agrosierra.data.repository.AgroRepositoryImpl
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.model.TipoLabor

@Composable
fun App() {
    val auth: AuthRepositoryImpl = koinInject()
    val agro: AgroRepositoryImpl = koinInject()
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<Sesion?>(null) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { session = auth.sesionGuardada(); loading = false }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB7F36B), background = Color(0xFF07120F), surface = Color(0xFF11201B))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10251D), Color(0xFF06110E))))) {
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else if (session == null) LoginScreen(onLogin = { user, password -> session = auth.login(user, password) })
            else HomeScreen(session!!, agro, onLogout = { scope.launch { auth.cerrarSesion(); session = null } })
        }
    }
}

@Composable private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x991A3429)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0x88FFFFFF), Color(0x33B7F36B), Color(0x11FFFFFF)))),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(Modifier.background(Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x00FFFFFF))))) {
            Column(Modifier.padding(22.dp), content = content)
        }
    }
}

@Composable private fun LoginScreen(onLogin: suspend (String, String) -> Unit) {
    var user by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center) {
        Text("AGROSIERRA  /  CAMPO", color = Color(0xFFB7F36B), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(18.dp)); Text("La sierra,\nen orden.", color = Color.White, fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp)); Text("Gestión de labores agrícolas · Unidad 2", color = Color(0xFFB5C7BE))
        Spacer(Modifier.height(30.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Ingresar al sistema", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp)); OutlinedTextField(user, { user = it }, Modifier.fillMaxWidth(), label = { Text("Usuario") }, singleLine = true)
            Spacer(Modifier.height(12.dp)); OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Contraseña") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (error.isNotBlank()) Text(error, color = Color(0xFFFF998F), modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(18.dp)); Button(onClick = { scope.launch { busy = true; error = try { onLogin(user.trim(), password); "" } catch (e: Exception) { e.message ?: "No se pudo iniciar sesión" }; busy = false } }, Modifier.fillMaxWidth().height(52.dp), enabled = !busy) { Text(if (busy) "Conectando…" else "Entrar  →") }
        }
        Spacer(Modifier.height(18.dp)); Text("Acceso de demostración: tecnico1 / Agro2026*", color = Color(0xFF84988E), fontSize = 12.sp)
    }
}

@Composable private fun HomeScreen(session: Sesion, repo: AgroRepositoryImpl, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var valle by remember { mutableStateOf("Todos los valles") }; var soloAbiertos by remember { mutableStateOf(false) }
    var lotes by remember { mutableStateOf<List<Lote>>(emptyList()) }; var selected by remember { mutableStateOf<Lote?>(null) }
    var labores by remember { mutableStateOf<List<Labor>>(emptyList()) }; var message by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }; var showNew by remember { mutableStateOf(false) }; var showProfile by remember { mutableStateOf(false) }
    LaunchedEffect(valle, soloAbiertos) { repo.observarLotes(valle.takeUnless { it.startsWith("Todos") }, soloAbiertos).collect { lotes = it } }
    LaunchedEffect(Unit) { try { repo.refrescar() } catch (e: Exception) { message = "Modo local · ${e.message ?: "sin conexión"}" } }
    LaunchedEffect(selected?.id) { selected?.let { lot ->
        try { repo.cargarLabores(lot.id) } catch (e: Exception) { message = e.message ?: "No se pudieron actualizar las labores" }
        repo.observarLabores(lot.id).collect { labores = it }
    } }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("AGROSIERRA", color = Color(0xFFB7F36B), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Text("Control de campo", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold) }
            Text("◉  ${session.usuario}", color = Color(0xFFD6E9DF), modifier = Modifier.clickable { showProfile = !showProfile })
        }
        if (showProfile) GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) { Text("Perfil de campo", color = Color.White, fontWeight = FontWeight.Bold); Text("${session.usuario} · ${session.rol}", color = Color(0xFFB5C7BE)); Text("${session.privilegios.size} privilegios activos", color = Color(0xFFB7F36B)); TextButton(onClick = onLogout) { Text("Cerrar sesión") } }
        if (selected == null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Padrón de lotes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp)); Text("${lotes.size} lotes en tu vista", color = Color(0xFFB5C7BE))
                Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    Box { OutlinedButton(onClick = { expanded = true }) { Text(valle) }; DropdownMenu(expanded, { expanded = false }) { listOf("Todos los valles", "Valle Norte", "Valle Sur", "Valle Central").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { valle = it; expanded = false }) } } }
                    FilterChip(selected = soloAbiertos, onClick = { soloAbiertos = !soloAbiertos }, label = { Text("Campaña abierta") })
                }
            }
            Spacer(Modifier.height(16.dp)); if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (message.isNotBlank()) Text(message, color = Color(0xFFFFD27C), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
            if (lotes.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sin lotes guardados. Desliza para sincronizar.", color = Color(0xFFB5C7BE)) }
            else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(lotes, key = { it.id }) { lote -> GlassCard(Modifier.fillMaxWidth().clickable { selected = lote }) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(lote.codigo, color = Color(0xFFB7F36B), fontWeight = FontWeight.Bold); Text(if (lote.campaniaCerrada) "CERRADA" else "EN CURSO", color = if (lote.campaniaCerrada) Color(0xFFFFD27C) else Color(0xFFB7F36B), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(8.dp)); Text(lote.nombre, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold); Text("${lote.cultivo}  ·  ${lote.valle}", color = Color(0xFFB5C7BE)); Spacer(Modifier.height(12.dp)); Text("${lote.hectareas} ha     › Ver labores", color = Color(0xFFEAF3EC))
                } }
            }
        } else {
            TextButton(onClick = { selected = null }) { Text("←  Todos los lotes") }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(selected!!.codigo, color = Color(0xFFB7F36B), fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(selected!!.nombre, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("${selected!!.cultivo} · ${selected!!.valle} · ${selected!!.hectareas} ha", color = Color(0xFFB5C7BE)); Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("${labores.size} labores registradas", color = Color.White); if ("LABOR_CREAR" in session.privilegios) Button(onClick = { showNew = true }) { Text("＋ Labor") } }
            }
            Spacer(Modifier.height(12.dp)); LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(labores, key = { it.id }) { labor -> GlassCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(labor.tipo.name, color = Color(0xFFB7F36B), fontWeight = FontWeight.Bold); Text(if (labor.pendiente) "● PENDIENTE" else labor.fecha, color = Color(0xFFB5C7BE), fontSize = 11.sp) }; Text("${labor.cantidad} · ${labor.responsable}", color = Color.White); if (labor.observacion.isNotBlank()) Text(labor.observacion, color = Color(0xFFB5C7BE)) } } }
        }
        Spacer(Modifier.weight(1f)); Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("◈  CAMPO", color = Color(0xFFB7F36B), fontSize = 11.sp); TextButton(onClick = { scope.launch { loading = true; message = try { "Sincronizadas ${repo.refrescar()} lotes" } catch (e: Exception) { e.message ?: "No se pudo sincronizar" }; loading = false } }) { Text("⟳  Sincronizar") }; TextButton(onClick = { showProfile = !showProfile }) { Text("Perfil  ◉") } }
    }
    if (showNew && selected != null) NewLaborDialog(selected!!, onDismiss = { showNew = false }, onSave = { type, amount, note -> scope.launch { try { repo.registrarLabor(selected!!.id, type, "2026-10-27", session.usuario, amount, note); message = "Labor guardada en el dispositivo"; showNew = false } catch (e: Exception) { message = e.message ?: "No se pudo guardar" } } })
}

@Composable private fun NewLaborDialog(lote: Lote, onDismiss: () -> Unit, onSave: (TipoLabor, Double, String) -> Unit) {
    var type by remember { mutableStateOf(TipoLabor.RIEGO) }; var amount by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Registrar labor · ${lote.codigo}") }, text = { Column {
        var expanded by remember { mutableStateOf(false) }; Box { OutlinedButton(onClick = { expanded = true }) { Text(type.name) }; DropdownMenu(expanded, { expanded = false }) { TipoLabor.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { type = it; expanded = false }) } } }
        OutlinedTextField(amount, { amount = it }, label = { Text("Cantidad aplicada") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(note, { note = it }, label = { Text("Observación") })
    } }, confirmButton = { TextButton(onClick = { amount.toDoubleOrNull()?.let { onSave(type, it, note) } }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
