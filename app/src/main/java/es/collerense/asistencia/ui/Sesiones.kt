@file:OptIn(ExperimentalMaterial3Api::class)

package es.collerense.asistencia.ui

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.collerense.asistencia.data.DIAS_LETRA
import es.collerense.asistencia.data.MESES
import es.collerense.asistencia.data.Repo
import es.collerense.asistencia.data.Sesion
import es.collerense.asistencia.data.fechaCorta
import java.time.LocalDate

@Composable
fun PantallaSesiones(repo: Repo, onAbrir: (String) -> Unit, onNuevaSesion: () -> Unit) {
    val d = repo.data
    var mes by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var horario by remember { mutableStateOf(false) }
    var cancelarUid by remember { mutableStateOf<String?>(null) }
    var borrarUid by remember { mutableStateOf<String?>(null) }
    val delMes = d.sesionesVivas.filter { val f = it.datos.localDate; f.year == mes.year && f.monthValue == mes.monthValue }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { mes = mes.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior") }
            Text("${MESES[mes.monthValue - 1]} ${mes.year}", fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 20.sp, color = Texto, modifier = Modifier.weight(1f))
            IconButton(onClick = { mes = mes.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onNuevaSesion, modifier = Modifier.weight(1f)) { Text("+ Nueva sesión") }
            OutlinedButton(onClick = { horario = true }, modifier = Modifier.weight(1f), enabled = d.horarios.isNotEmpty()) { Text("Horario semana") }
        }
        Espacio()
        if (delMes.isEmpty()) {
            Text("Sin sesiones este mes.", color = GrisTexto, modifier = Modifier.padding(16.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(delMes, key = { it.uid }) { s ->
                FilaSesion(s, onClick = { onAbrir(s.uid) },
                    onCancelar = { if (s.datos.cancelada) repo.actualizarSesion(s.uid) { it.copy(estado = "", nota = "") } else cancelarUid = s.uid },
                    onBorrar = { borrarUid = s.uid })
                HorizontalDivider(color = Borde)
            }
            item { Espacio(80) }
        }
    }
    if (horario) DialogoHorarioSemana(repo, onCerrar = { horario = false })
    cancelarUid?.let { uid ->
        val s = d.sesiones.first { it.uid == uid }
        DialogoCancelar(d.motivos, "Cancelar ${s.datos.tipo.lowercase()} del ${fechaCorta(s.datos.localDate)}",
            onCancelar = { m -> repo.actualizarSesion(uid) { it.copy(estado = "Cancelada", nota = m) }; cancelarUid = null },
            onCerrar = { cancelarUid = null })
    }
    borrarUid?.let { uid ->
        val s = d.sesiones.first { it.uid == uid }
        Confirmar("Eliminar sesión",
            "¿Eliminar el ${s.datos.tipo.lowercase()} del ${fechaCorta(s.datos.localDate)}? Se borran sus ${s.datos.registros.size} registros.\n\nSi solo no se hizo, es mejor cancelarla.",
            si = "Eliminar", onSi = { repo.borrarSesion(uid); borrarUid = null }, onNo = { borrarUid = null })
    }
}

@Composable
private fun FilaSesion(s: Sesion, onClick: () -> Unit, onCancelar: () -> Unit, onBorrar: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val f = s.datos.localDate
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }
            .background(if (s.datos.cancelada) Superficie else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(70.dp)) {
            Text(fechaCorta(f).uppercase(), fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, color = if (f == LocalDate.now()) Amarillo else Texto)
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etiqueta(if (s.datos.partido) "PARTIDO" else "ENTRENO", if (s.datos.partido) Amarillo else Morado, if (s.datos.partido) Color(0xFF1A1200) else Color.White)
                if (s.datos.cancelada) { Text("  "); Etiqueta("CANCELADA", Rojo.copy(alpha = .35f), RojoNeon) }
                if (s.modificada) { Text("  "); Etiqueta("•", Rojo) }
            }
            val sub = listOf(s.datos.hora, s.datos.nota, s.datos.resultado).filter { it.isNotBlank() }.joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp, color = GrisTexto, maxLines = 2)
        }
        Text("${s.datos.registros.size}", fontWeight = FontWeight.Bold, color = if (s.datos.registros.isEmpty()) GrisTexto else Verde)
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opciones") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(if (s.datos.cancelada) "Quitar cancelación" else "Cancelar…") }, onClick = { menu = false; onCancelar() })
                DropdownMenuItem(text = { Text("Eliminar", color = Rojo) }, onClick = { menu = false; onBorrar() })
            }
        }
    }
}

@Composable
fun DialogoNuevaSesion(repo: Repo, onCreada: (String) -> Unit, onCerrar: () -> Unit) {
    val ctx = LocalContext.current
    val d = repo.data
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    var partido by remember { mutableStateOf(false) }
    var hora by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    val letra = DIAS_LETRA[fecha.dayOfWeek.value - 1]
    val sugerencias = d.horarios.filter { it.dia.equals(letra, true) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nueva sesión") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BotonFecha("Fecha", fecha, ctx) { if (it != null) fecha = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !partido, onClick = { partido = false }); Text("Entreno")
                    RadioButton(selected = partido, onClick = { partido = true }); Text("Partido")
                }
                OutlinedTextField(hora, { hora = it }, label = { Text("Hora (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (!partido && sugerencias.isNotEmpty()) {
                    sugerencias.forEach { h ->
                        TextButton(onClick = { hora = h.texto() }) { Text("${h.nombre}: ${h.texto()}", fontSize = 12.sp) }
                    }
                }
                OutlinedTextField(nota, { nota = it }, label = { Text(if (partido) "Rival (opcional)" else "Nota (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                val existe = repo.buscarSesion(fecha, if (partido) "Partido" else "Entreno")
                if (existe != null) Text("Ya existe un ${if (partido) "partido" else "entreno"} ese día.", color = Rojo, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreada(repo.nuevaSesion(fecha, if (partido) "Partido" else "Entreno", hora.trim(), nota.trim())) }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoHorarioSemana(repo: Repo, onCerrar: () -> Unit) {
    val ctx = LocalContext.current
    val d = repo.data
    val nombres = d.horarios.map { it.nombre }.distinct()
    var fecha by remember { mutableStateOf(LocalDate.now().let { it.plusDays((8 - it.dayOfWeek.value).toLong() % 7) }) }
    var sel by remember { mutableStateOf(nombres.firstOrNull() ?: "") }
    var hecho by remember { mutableStateOf<String?>(null) }
    val lunes = fecha.minusDays((fecha.dayOfWeek.value - 1).toLong())
    if (hecho != null) {
        Aviso("Horario aplicado", hecho!!) { hecho = null; onCerrar() }
        return
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Horario de la semana") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BotonFecha("Semana del", fecha, ctx) { if (it != null) fecha = it }
                Text("Del ${fechaCorta(lunes)} al ${fechaCorta(lunes.plusDays(6))}", fontSize = 12.sp, color = GrisTexto)
                Espacio()
                nombres.forEach { n ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { sel = n }) {
                        RadioButton(selected = sel == n, onClick = { sel = n })
                        Column {
                            Text(n, fontWeight = FontWeight.Bold)
                            d.horarios.filter { it.nombre == n }.forEach { h -> Text("${h.dia}  ${h.texto()}", fontSize = 12.sp, color = GrisTexto) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val n = repo.horarioSemana(fecha, sel)
                hecho = "$sel aplicado a $n entrenos de la semana del ${fechaCorta(lunes)}."
            }, enabled = sel.isNotEmpty()) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
