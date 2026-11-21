@file:OptIn(ExperimentalMaterial3Api::class)

package es.collerense.asistencia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.collerense.asistencia.data.CODIGOS_ENTRENO
import es.collerense.asistencia.data.CODIGOS_PARTIDO
import es.collerense.asistencia.data.Repo
import es.collerense.asistencia.data.Sesion
import es.collerense.asistencia.data.fechaCorta
import es.collerense.asistencia.data.significado
import es.collerense.asistencia.logic.Calc
import java.time.LocalDate

/** Sesión por defecto: la de hoy, si no la última pasada, si no la primera. */
fun sesionPorDefecto(lista: List<Sesion>): Sesion? {
    val hoy = LocalDate.now().toString()
    return lista.firstOrNull { it.datos.fecha == hoy }
        ?: lista.lastOrNull { it.datos.fecha < hoy }
        ?: lista.firstOrNull()
}

@Composable
fun PantallaPasarLista(repo: Repo, uidSel: String?, onSel: (String) -> Unit, onNuevaSesion: () -> Unit) {
    val d = repo.data
    val lista = d.sesionesVivas
    if (lista.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No hay sesiones.", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Espacio()
            Text("Importa el Excel en la pestaña Sincronizar o crea una sesión.", color = GrisTexto)
            Espacio()
            Button(onClick = onNuevaSesion) { Text("+ Nueva sesión") }
        }
        return
    }
    val s = lista.firstOrNull { it.uid == uidSel } ?: sesionPorDefecto(lista)!!
    val idx = lista.indexOf(s)
    val fecha = s.datos.localDate
    var cancelar by remember { mutableStateOf(false) }

    val jugadores = d.jugadores.filter { it.activoEn(fecha, d.inicioTemporada) }
        .sortedWith(compareBy({ it.datos.invitado }, { it.datos.dorsal.toIntOrNull() ?: 999 }, { it.id }))

    Column(Modifier.fillMaxSize()) {
        // ---- cabecera de la sesión ----
        Card(
            colors = CardDefaults.cardColors(containerColor = if (s.datos.partido) Color(0xFFF6E7D4) else Color(0xFFEEF2FB)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (idx > 0) onSel(lista[idx - 1].uid) }, enabled = idx > 0) { Icon(Icons.Filled.KeyboardArrowLeft, "Anterior") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fechaCorta(fecha) + " · " + fecha.year, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Etiqueta(if (s.datos.partido) "PARTIDO" else "ENTRENO", if (s.datos.partido) Oro else Navy)
                        if (s.datos.hora.isNotBlank()) Text("  " + s.datos.hora, fontSize = 12.sp, color = GrisTexto, maxLines = 1)
                    }
                }
                IconButton(onClick = { if (idx < lista.size - 1) onSel(lista[idx + 1].uid) }, enabled = idx < lista.size - 1) {
                    Icon(Icons.Filled.KeyboardArrowRight, "Siguiente")
                }
            }
            if (s.datos.cancelada) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFFE5E7EB)).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✕ CANCELADA: " + s.datos.nota, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Color(0xFF374151), fontSize = 13.sp)
                    OutlinedButton(onClick = { repo.actualizarSesion(s.uid) { it.copy(estado = "", nota = "") } }) { Text("Reactivar") }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!s.datos.partido) {
                        Button(onClick = {
                            jugadores.filter { !it.datos.invitado && s.datos.codigo(it.id).isEmpty() }
                                .forEach { j -> repo.ponerCodigo(s.uid, j.id, "A") }
                        }, modifier = Modifier.weight(1f)) { Text("Todos asisten", fontSize = 13.sp) }
                    }
                    OutlinedButton(onClick = { cancelar = true }, modifier = Modifier.weight(1f)) { Text("Cancelar sesión", fontSize = 13.sp, color = Rojo) }
                    OutlinedButton(onClick = onNuevaSesion) { Text("+", fontSize = 16.sp) }
                }
            }
            if (s.datos.partido && !s.datos.cancelada) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = s.datos.nota, onValueChange = { v -> repo.actualizarSesion(s.uid) { it.copy(nota = v) } },
                        label = { Text("Rival") }, singleLine = true, modifier = Modifier.weight(2f))
                    OutlinedTextField(value = s.datos.resultado, onValueChange = { v -> repo.actualizarSesion(s.uid) { it.copy(resultado = v) } },
                        label = { Text("Resultado") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            val n = s.datos.registros.size
            Text(
                "$n de ${jugadores.size} con registro" + if (s.modificada) " · cambios sin exportar" else "",
                fontSize = 11.sp, color = GrisTexto, modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
            )
        }

        // ---- jugadores ----
        LazyColumn(Modifier.fillMaxSize()) {
            items(jugadores, key = { it.id }) { j ->
                val cod = s.datos.codigo(j.id)
                val aviso = Calc.invitadoSinEntreno(j, s, d)
                Column(Modifier.fillMaxWidth().background(if (aviso) Color(0xFFFFE4E6) else Color.Transparent).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(j.datos.dorsal.ifEmpty { "–" }, modifier = Modifier.width(28.dp), fontWeight = FontWeight.Bold, color = Navy)
                        Text(
                            j.datos.nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1,
                            fontStyle = if (j.datos.invitado) FontStyle.Italic else FontStyle.Normal
                        )
                        if (j.datos.invitado) Etiqueta("INV", Oro)
                        if (cod.isNotEmpty()) {
                            Spacer(Modifier.width(6.dp))
                            Text(significado(cod, s.datos.partido), fontSize = 11.sp, color = GrisTexto, maxLines = 1)
                        }
                    }
                    if (aviso) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = Rojo, modifier = Modifier.padding(end = 4.dp))
                            Text("Invitado sin entreno apuntado esta semana", color = Rojo, fontSize = 11.sp)
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(8.dp))) {
                        val codigos = if (s.datos.partido) CODIGOS_PARTIDO else CODIGOS_ENTRENO
                        codigos.forEach { c ->
                            ChipCodigo(c, cod == c, onClick = { if (!s.datos.cancelada) repo.ponerCodigo(s.uid, j.id, c) }, ancho = if (s.datos.partido) 46 else 54)
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
            item { Espacio(80) }
        }
    }
    if (cancelar) {
        DialogoCancelar(d.motivos, "Cancelar ${s.datos.tipo.lowercase()} del ${fechaCorta(fecha)}",
            onCancelar = { m -> repo.actualizarSesion(s.uid) { it.copy(estado = "Cancelada", nota = m) }; cancelar = false },
            onCerrar = { cancelar = false })
    }
}
