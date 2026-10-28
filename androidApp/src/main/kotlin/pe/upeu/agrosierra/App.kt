package pe.upeu.agrosierra

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import android.util.Log
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pe.upeu.agrosierra.data.remote.MapeoErrores
import pe.upeu.agrosierra.data.repository.AuthRepositoryImpl
import pe.upeu.agrosierra.data.repository.AgroRepositoryImpl
import pe.upeu.agrosierra.domain.model.ErrorApi
import pe.upeu.agrosierra.domain.model.Labor
import pe.upeu.agrosierra.domain.model.Lote
import pe.upeu.agrosierra.domain.model.Sesion
import pe.upeu.agrosierra.domain.model.TipoLabor

@Composable
fun App() {
    val auth: AuthRepositoryImpl = koinInject()
    val agro: AgroRepositoryImpl = koinInject()
    SideEffect { android.util.Log.e("AgroCampo", "App() composed, auth=$auth, agro=$agro") }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<Sesion?>(null) }
    var loading by remember { mutableStateOf(true) }
    var authError by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { session = auth.sesionGuardada(); loading = false }
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB7F36B), background = Color(0xFF07120F), surface = Color(0xFF11201B))) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10251D), Color(0xFF06110E))))) {
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else if (session == null) LoginScreen(
                onLogin = { user, password ->
                    scope.launch {
                        loading = true
                        authError = ""
                        try { session = auth.login(user, password) } catch (e: Exception) {
                            authError = when (e) {
                                is ErrorApi.NoAutorizado, is ErrorApi.SinPrivilegio -> "Credenciales incorrectas"
                                is ErrorApi.SinConexion, is ErrorApi.TiempoAgotado -> "Sin conexion al servidor"
                                else -> e.message ?: "No se pudo iniciar sesion"
                            }
                        }
                        loading = false
                    }
                },
                error = authError,
                busy = loading
            )
            else HomeScreen(session!!, agro, onLogout = { scope.launch { auth.cerrarSesion(); session = null } }, onSessionExpired = { scope.launch { auth.cerrarSesion(); session = null } })
        }
    }
}

@Composable
private fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
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

@Composable
private fun LoginScreen(onLogin: (String, String) -> Unit, error: String, busy: Boolean) {
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.Center) {
        Text("AGROSIERRA  /  CAMPO", color = Color(0xFFB7F36B), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(18.dp))
        Text("La sierra,\nen orden.", color = Color.White, fontSize = 42.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text("Gestion de labores agricolas - Unidad 2", color = Color(0xFFB5C7BE))
        Spacer(Modifier.height(30.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Ingresar al sistema", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(user, { user = it }, Modifier.fillMaxWidth(), label = { Text("Usuario") }, singleLine = true)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Contrasena") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (error.isNotBlank()) Text(error, color = Color(0xFFFF998F), modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(18.dp))
            Button(onClick = { onLogin(user.trim(), password) }, Modifier.fillMaxWidth().height(52.dp), enabled = !busy) {
                Text(if (busy) "Conectando..." else "Entrar  ->")
            }
        }
    }
}

@Composable
private fun HomeScreen(session: Sesion, repo: AgroRepositoryImpl, onLogout: () -> Unit, onSessionExpired: () -> Unit) {
    val scope = rememberCoroutineScope()
    var valle by remember { mutableStateOf("Todos los valles") }
    var soloAbiertos by remember { mutableStateOf(false) }
    var busqueda by remember { mutableStateOf("") }
    var lotes by remember { mutableStateOf<List<Lote>>(emptyList()) }
    var selected by remember { mutableStateOf<Lote?>(null) }
    var labores by remember { mutableStateOf<List<Labor>>(emptyList()) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var editLabor by remember { mutableStateOf<Labor?>(null) }
    var deleteLabor by remember { mutableStateOf<Labor?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var lastSync by remember { mutableStateOf<String?>(null) }
    var syncResumen by remember { mutableStateOf("") }

    LaunchedEffect(valle, soloAbiertos, busqueda) {
        repo.observarLotes(valle.takeUnless { it.startsWith("Todos") }, soloAbiertos).collect { rows ->
            lotes = if (busqueda.isBlank()) rows else rows.filter {
                it.nombre.contains(busqueda, true) || it.codigo.contains(busqueda, true)
            }
        }
    }
    LaunchedEffect(Unit) {
        loading = true
        try { repo.refrescar(); lastSync = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date()); message = "Sincronizado" } catch (e: Exception) {
            error = mensajeDe(e, onSessionExpired)
        }
        loading = false
    }
    LaunchedEffect(selected?.id) {
        selected?.let { lot ->
            try { repo.cargarLabores(lot.id) } catch (e: Exception) { error = mensajeDe(e, onSessionExpired) }
            repo.observarLabores(lot.id).collect { labores = it }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF10251D),
                contentColor = Color(0xFFB7F36B),
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = selected == null && !showProfile,
                    onClick = { selected = null; showProfile = false },
                    icon = { Text("🌾", fontSize = 16.sp) },
                    label = { Text("Campo", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFB7F36B),
                        selectedTextColor = Color(0xFFB7F36B),
                        unselectedIconColor = Color(0xFF84988E),
                        unselectedTextColor = Color(0xFF84988E),
                        indicatorColor = Color(0xFF1A3429)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        scope.launch {
                            syncing = true
                            error = ""
                            syncResumen = ""
                            try {
                                val enviados = repo.sincronizar()
                                val recibidos = repo.refrescar()
                                lastSync = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
                                syncResumen = "Enviadas $enviados · Recibidas $recibidos"
                                message = "Sincronizacion completa"
                            } catch (e: Exception) {
                                error = mensajeDe(e, onSessionExpired)
                            }
                            syncing = false
                        }
                    },
                    enabled = !syncing,
                    icon = { Text(if (syncing) "⟳" else "🔄", fontSize = 16.sp) },
                    label = { Text(if (syncing) "Sync..." else "Sincronizar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color(0xFF84988E),
                        unselectedTextColor = Color(0xFF84988E),
                        indicatorColor = Color(0xFF1A3429)
                    )
                )
                NavigationBarItem(
                    selected = showProfile,
                    onClick = { showProfile = true; selected = null },
                    icon = { Text("👤", fontSize = 16.sp) },
                    label = { Text("Perfil", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFB7F36B),
                        selectedTextColor = Color(0xFFB7F36B),
                        unselectedIconColor = Color(0xFF84988E),
                        unselectedTextColor = Color(0xFF84988E),
                        indicatorColor = Color(0xFF1A3429)
                    )
                )
            }
        }
    ) { innerPadding ->
    Column(Modifier.fillMaxSize().padding(innerPadding).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("AGROSIERRA", color = Color(0xFFB7F36B), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Text("Control de campo", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.SemiBold) }
            Text("◉  ${session.usuario}", color = Color(0xFFD6E9DF), modifier = Modifier.clickable { showProfile = !showProfile })
        }
        if (showProfile) {
            GlassCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text("Perfil de campo", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${session.usuario} · ${session.rol}", color = Color(0xFFB5C7BE))
                Text("${session.privilegios.size} privilegios activos", color = Color(0xFFB7F36B))
                TextButton(onClick = onLogout) { Text("Cerrar sesion") }
            }
        } else if (selected == null) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Padron de lotes", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("${lotes.size} lotes en tu vista", color = Color(0xFFB5C7BE))
                if (lastSync != null) Text("Ultima sincronizacion: $lastSync", color = Color(0xFF84988E), fontSize = 11.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }) { Text(valle) }
                        DropdownMenu(expanded, { expanded = false }) {
                            listOf("Todos los valles", "Valle Norte", "Valle Sur", "Valle Central").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { valle = it; expanded = false })
                            }
                        }
                    }
                    FilterChip(selected = soloAbiertos, onClick = { soloAbiertos = !soloAbiertos }, label = { Text("Campania abierta") })
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(busqueda, { busqueda = it }, Modifier.fillMaxWidth(), label = { Text("Buscar por nombre o codigo") }, singleLine = true)
            }
            Spacer(Modifier.height(16.dp))
            if (loading || syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (error.isNotBlank()) Text(error, color = Color(0xFFFF998F), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
            if (syncResumen.isNotBlank()) Text(syncResumen, color = Color(0xFFB7F36B), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
            if (message.isNotBlank()) Text(message, color = Color(0xFFFFD27C), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp))
            if (lotes.isEmpty() && !loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin lotes guardados. Desliza para sincronizar.", color = Color(0xFFB5C7BE))
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(lotes, key = { it.id }) { lote ->
                        GlassCard(Modifier.fillMaxWidth().clickable { selected = lote }) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(lote.codigo, color = Color(0xFFB7F36B), fontWeight = FontWeight.Bold)
                                Text(if (lote.campaniaCerrada) "CERRADA" else "EN CURSO", color = if (lote.campaniaCerrada) Color(0xFFFFD27C) else Color(0xFFB7F36B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(lote.nombre, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                            Text("${lote.cultivo}  ·  ${lote.valle}", color = Color(0xFFB5C7BE))
                            Spacer(Modifier.height(12.dp))
                            Text("${lote.hectareas} ha     › Ver labores", color = Color(0xFFEAF3EC))
                        }
                    }
                }
            }
        } else {
            TextButton(onClick = { selected = null }) { Text("←  Todos los lotes") }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(selected!!.codigo, color = Color(0xFFB7F36B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(selected!!.nombre, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                Text("${selected!!.cultivo} · ${selected!!.valle} · ${selected!!.hectareas} ha", color = Color(0xFFB5C7BE))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${labores.size} labores registradas", color = Color.White)
                    if ("LABOR_CREAR" in session.privilegios) Button(onClick = { showNew = true }) { Text("＋ Labor") }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (labores.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("Sin labores registradas", color = Color(0xFFB5C7BE)) }
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(labores, key = { it.id }) { labor ->
                        GlassCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(labor.tipo.name, color = Color(0xFFB7F36B), fontWeight = FontWeight.Bold)
                                Text(if (labor.pendiente) "● PENDIENTE" else labor.fecha, color = if (labor.pendiente) Color(0xFFFFD27C) else Color(0xFFB5C7BE), fontSize = 11.sp)
                            }
                            Text("${labor.cantidad} · ${labor.responsable}", color = Color.White)
                            if (labor.observacion.isNotBlank()) Text(labor.observacion, color = Color(0xFFB5C7BE))
                            if ("LABOR_EDITAR" in session.privilegios && !labor.pendiente) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { editLabor = labor }) { Text("Editar") }
                                    if ("LABOR_ELIMINAR" in session.privilegios) {
                                        TextButton(onClick = { deleteLabor = labor }) { Text("Eliminar") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
    if (showNew && selected != null) NewLaborDialog(selected!!, onDismiss = { showNew = false }, onSave = { type, amount, note ->
        scope.launch {
            try {
                repo.registrarLabor(selected!!.id, type, "2026-10-27", session.usuario, amount, note)
                message = "Labor guardada en el dispositivo"
                showNew = false
            } catch (e: Exception) {
                error = mensajeDe(e, onSessionExpired)
            }
        }
    })
    editLabor?.let { labor ->
        EditLaborDialog(labor, onDismiss = { editLabor = null }, onSave = { type, amount, note ->
            scope.launch {
                try {
                    repo.actualizarLabor(labor.id, labor.loteId, type, labor.fecha, labor.responsable, amount, note)
                    message = "Labor actualizada"
                    editLabor = null
                    error = ""
                } catch (e: Exception) {
                    error = mensajeDe(e, onSessionExpired)
                }
            }
        })
    }
    deleteLabor?.let { labor ->
        AlertDialog(
            onDismissRequest = { deleteLabor = null },
            title = { Text("Eliminar labor") },
            text = { Text("¿Eliminar la labor ${labor.tipo.name} de ${labor.cantidad}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            repo.eliminarLabor(labor.id)
                            message = "Labor eliminada"
                            deleteLabor = null
                            error = ""
                        } catch (e: Exception) {
                            error = mensajeDe(e, onSessionExpired)
                        }
                    }
                }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { deleteLabor = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun NewLaborDialog(lote: Lote, onDismiss: () -> Unit, onSave: (TipoLabor, Double, String) -> Unit) {
    var type by remember { mutableStateOf(TipoLabor.RIEGO) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar labor · ${lote.codigo}") },
        text = {
            Column {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(type.name) }
                    DropdownMenu(expanded, { expanded = false }) {
                        TipoLabor.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { type = it; expanded = false }) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(amount, { amount = it; validationError = "" }, label = { Text("Cantidad aplicada") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = validationError.isNotBlank())
                if (validationError.isNotBlank()) Text(validationError, color = Color(0xFFFF998F), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, label = { Text("Observacion") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = amount.toDoubleOrNull()
                if (v == null) { validationError = "Cantidad invalida"; return@TextButton }
                onSave(type, v, note)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun EditLaborDialog(labor: Labor, onDismiss: () -> Unit, onSave: (TipoLabor, Double, String) -> Unit) {
    var type by remember { mutableStateOf(labor.tipo) }
    var amount by remember { mutableStateOf(labor.cantidad.toString()) }
    var note by remember { mutableStateOf(labor.observacion) }
    var validationError by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar labor") },
        text = {
            Column {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) { Text(type.name) }
                    DropdownMenu(expanded, { expanded = false }) {
                        TipoLabor.entries.forEach { DropdownMenuItem(text = { Text(it.name) }, onClick = { type = it; expanded = false }) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(amount, { amount = it; validationError = "" }, label = { Text("Cantidad aplicada") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = validationError.isNotBlank())
                if (validationError.isNotBlank()) Text(validationError, color = Color(0xFFFF998F), fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(note, { note = it }, label = { Text("Observacion") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = amount.toDoubleOrNull()
                if (v == null) { validationError = "Cantidad invalida"; return@TextButton }
                onSave(type, v, note)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private fun mensajeDe(e: Exception, onSessionExpired: () -> Unit): String = when (e) {
    is ErrorApi.NoAutorizado -> { onSessionExpired(); "Sesion vencida, vuelve a iniciar sesion" }
    is ErrorApi.SinPrivilegio -> "Operacion no autorizada"
    is ErrorApi.Validacion -> e.errores.entries.joinToString { "${it.key}: ${it.value}" }
    is ErrorApi.NoEncontrado -> "Recurso no encontrado"
    is ErrorApi.Conflicto -> e.motivo
    is ErrorApi.Servidor -> "Error del servidor, reintenta"
    is ErrorApi.SinConexion -> "Sin conexion"
    is ErrorApi.TiempoAgotado -> "Tiempo agotado"
    is ErrorApi.Desconocido -> e.mensaje
    else -> e.message ?: "Error desconocido"
}