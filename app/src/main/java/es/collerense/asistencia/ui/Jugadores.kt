@file:OptIn(ExperimentalMaterial3Api::class)

package es.collerense.asistencia.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.border
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import es.collerense.asistencia.data.Jugador
import es.collerense.asistencia.data.JugadorDatos
import es.collerense.asistencia.data.MESES
import es.collerense.asistencia.data.Repo
import es.collerense.asistencia.data.fechaCorta
import es.collerense.asistencia.data.fechaLarga
import es.collerense.asistencia.data.parseFecha
import es.collerense.asistencia.logic.Calc
import java.time.LocalDate
import java.time.Period

@Composable
fun FotoJugador(repo: Repo, j: Jugador, tam: Int) {
    val bmp = remember(j.foto) { repo.fotoBitmap(j) }
    Box(Modifier.size(tam.dp).clip(if (tam < 60) CircleShape else CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp))
        .background(Brush.linearGradient(listOf(Morado, Superficie2)))
        .border(BorderStroke(if (tam < 60) 1.5.dp else 2.dp, Brush.linearGradient(listOf(Amarillo, Rojo))), if (tam < 60) CircleShape else CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)),
        contentAlignment = Alignment.Center) {
        if (bmp != null) Image(bmp.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(j.datos.nombre.take(1), fontWeight = FontWeight.Black, color = Texto, fontSize = (tam / 2.5).sp)
    }
}

@Composable
fun PantallaJugadores(repo: Repo, onFicha: (Int) -> Unit) {
    val d = repo.data
    val hoy = LocalDate.now()
    var nuevo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${d.jugadores.count { it.estado(hoy) == "Activo" }} activos · ${d.jugadores.count { it.datos.invitado }} invitados",
                modifier = Modifier.weight(1f), color = GrisTexto)
            Button(onClick = { nuevo = true }, enabled = repo.idLibre() != null) { Text("+ Jugador") }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(d.jugadores.sortedWith(compareBy({ it.estado(hoy) == "Baja" }, { it.datos.invitado }, { it.datos.dorsal.toIntOrNull() ?: 999 })), key = { it.id }) { j ->
                val est = j.estado(hoy)
                val st = Calc.stats(j, d)
                Row(Modifier.fillMaxWidth().clickable { onFicha(j.id) }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FotoJugador(repo, j, 44)
                    Text(j.datos.dorsal.ifEmpty { "–" }, modifier = Modifier.width(38.dp).padding(start = 8.dp), fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 17.sp, color = Amarillo)
                    Column(Modifier.weight(1f)) {
                        Text(j.datos.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, color = if (est == "Baja") GrisTexto else Texto)
                        Row {
                            when (est) {
                                "Baja" -> Etiqueta("BAJA", Rojo)
                                "Invitado" -> Etiqueta("INVITADO", Naranja, Color(0xFF1A0A00))
                                "Próxima alta" -> Etiqueta("PRÓX. ALTA", Morado)
                                else -> {}
                            }
                            if (j.modificado) { Text(" "); Etiqueta("•", Rojo) }
                        }
                    }
                    Text(st.pct?.let { "${(it * 100).toInt()}%" } ?: "", fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 16.sp,
                        color = if ((st.pct ?: 1.0) < d.equipo.umbral) RojoNeon else Verde)
                }
                HorizontalDivider(color = Borde)
            }
            item { Espacio(80) }
        }
    }
    if (nuevo) {
        val id = repo.idLibre()
        if (id != null) DialogoJugador(repo, id, null, onCerrar = { nuevo = false }, onGuardado = { nuevo = false; onFicha(id) })
    }
}

@Composable
fun DialogoJugador(repo: Repo, id: Int, actual: JugadorDatos?, onCerrar: () -> Unit, onGuardado: () -> Unit) {
    val ctx = LocalContext.current
    var nombre by remember { mutableStateOf(actual?.nombre ?: "") }
    var dorsal by remember { mutableStateOf(actual?.dorsal ?: "") }
    var invitado by remember { mutableStateOf(actual?.invitado ?: false) }
    var alta by remember { mutableStateOf(parseFecha(actual?.alta) ?: if (actual == null) LocalDate.now() else null) }
    var nac by remember { mutableStateOf(parseFecha(actual?.nacimiento)) }
    var dni by remember { mutableStateOf(actual?.dni ?: "") }
    var obs by remember { mutableStateOf(actual?.obs ?: "") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (actual == null) "Nuevo jugador" else "Editar jugador") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("APELLIDOS , NOMBRE") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(dorsal, { dorsal = it }, label = { Text("Dorsal") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !invitado, onClick = { invitado = false }); Text("Plantilla")
                    RadioButton(selected = invitado, onClick = { invitado = true }); Text("Invitado")
                }
                BotonFecha("Fecha de alta", alta, ctx, permitirVacio = actual != null) { alta = it }
                BotonFecha("Nacimiento", nac, ctx, permitirVacio = true) { nac = it }
                OutlinedTextField(dni, { dni = it }, label = { Text("DNI/NIE (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(obs, { obs = it }, label = { Text(if (invitado) "Equipo de origen / observaciones" else "Observaciones") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = nombre.isNotBlank(), onClick = {
                val base = actual ?: JugadorDatos()
                repo.guardarJugador(id, base.copy(
                    nombre = nombre.trim().uppercase(), dorsal = dorsal.trim(), tipo = if (invitado) "Invitado" else "Plantilla",
                    alta = alta?.toString(), nacimiento = nac?.toString(), dni = dni.trim(), obs = obs.trim()
                ))
                onGuardado()
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
fun PantallaFicha(repo: Repo, id: Int, onVolver: () -> Unit) {
    val d = repo.data
    val j = d.jugador(id)
    if (j == null) { onVolver(); return }
    val ctx = LocalContext.current
    val hoy = LocalDate.now()
    var editar by remember { mutableStateOf(false) }
    var baja by remember { mutableStateOf(false) }
    var fotoMenu by remember { mutableStateOf(false) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp -> if (bmp != null) repo.ponerFoto(id, bmp) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            if (bmp != null) repo.ponerFoto(id, bmp)
        }
    }
    val st = Calc.stats(j, d)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
      FondoEquipacion(Modifier.fillMaxWidth()) {
        // dorsal gigante de fondo, como en la camiseta
        Text(j.datos.dorsal, fontSize = 130.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, color = Color.White.copy(alpha = .16f),
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp), maxLines = 1)
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { fotoMenu = true }) { FotoJugador(repo, j, 104) }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(j.datos.nombre.uppercase(), fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 21.sp, color = Color.White, lineHeight = 23.sp)
                Text("#${j.datos.dorsal.ifEmpty { "–" }} · ${j.estado(hoy)}" + if (j.datos.invitado) " (invitado)" else "", color = Amarillo, fontWeight = FontWeight.Bold)
                parseFecha(j.datos.nacimiento)?.let { Text("${Period.between(it, hoy).years} años · ${fechaLarga(it)}", color = Color.White.copy(alpha = .85f), fontSize = 13.sp) }
                if (j.datos.obs.isNotBlank()) Text(j.datos.obs, fontSize = 12.sp, color = Color.White.copy(alpha = .8f))
            }
        }
      }
      Column(Modifier.padding(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { editar = true }, modifier = Modifier.weight(1f)) { Text("Editar") }
            OutlinedButton(onClick = { fotoMenu = true }, modifier = Modifier.weight(1f)) { Text("Foto") }
            OutlinedButton(onClick = { baja = true }, modifier = Modifier.weight(1f)) { Text(if (j.datos.baja != null) "Reactivar" else "Baja", color = RojoNeon) }
        }
        FilaSimple("Alta", parseFecha(j.datos.alta)?.let { fechaLarga(it) } ?: "inicio de temporada")
        if (j.datos.baja != null) FilaSimple("Baja", (parseFecha(j.datos.baja)?.let { fechaLarga(it) } ?: "") + if (j.datos.motivo.isNotBlank()) " · ${j.datos.motivo}" else "")

        Seccion("Entrenamientos")
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tile("% ASIST.", if (j.datos.invitado) "Inv." else st.pct?.let { "${(it * 100).toInt()}%" } ?: "–",
                if ((st.pct ?: 1.0) < d.equipo.umbral) RojoNeon else Verde, Modifier.weight(1f))
            Tile("ASISTENCIAS", if (j.datos.invitado) "${st.asistencias}" else "${st.asistencias}/${st.asistencias + st.f}", Navy, Modifier.weight(1f))
            Tile("FALTAS", "${st.f}", RojoNeon, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tile("JUSTIF.", "${st.j}", MoradoNeon, Modifier.weight(1f))
            Tile("LESIONADO", "${st.l}", Celeste, Modifier.weight(1f))
            Tile("RETRASOS", "${st.r}", Amarillo, Modifier.weight(1f))
        }
        Seccion("Partidos")
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tile("CONVOC.", "${st.conv}/${st.conv + st.nc}", Oro, Modifier.weight(1f))
            Tile("JUGADOS", "${st.jugados}", Oro, Modifier.weight(1f))
            Tile("CUARTOS", "${st.cuartos}", Oro, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tile("% MINUTOS", st.pctMin?.let { "${(it * 100).toInt()}%" } ?: "–", Oro, Modifier.weight(1f))
            Tile("MEDIA ¼", st.mediaCuartos?.let { "%.1f".format(it) } ?: "–", Oro, Modifier.weight(1f))
            Tile("NO CONV.", "${st.nc}", GrisTexto, Modifier.weight(1f))
        }
        Seccion("Últimas sesiones")
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Calc.ultimas(j, d, 8).reversed().forEach { s ->
                val c = s.datos.codigo(j.id)
                val (bg, fg) = colorCodigo(c)
                Column(Modifier.weight(1f).padding(1.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(fechaCorta(s.datos.localDate).substringAfter(' ') + if (s.datos.partido) "P" else "", fontSize = 9.sp, color = GrisTexto)
                    Text(c.ifEmpty { "–" }, color = fg, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(CorteDiagonal).background(bg).padding(vertical = 6.dp))
                }
            }
        }
        Seccion("Por meses")
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf("Mes" to 1.4f, "Entr." to 1f, "Asis." to 1f, "Falt." to 1f, "%" to 1f, "Part." to 1f, "¼" to 1f).forEach { (t, w) ->
                Text(t, Modifier.weight(w), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Amarillo, textAlign = TextAlign.Center)
            }
        }
        Calc.meses(d).forEach { m ->
            val sm = Calc.stats(j, d, m, m.plusMonths(1))
            if (sm.entrenos + sm.partidos > 0) {
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(MESES[m.monthValue - 1].take(3) + " " + (m.year % 100), Modifier.weight(1.4f), fontSize = 12.sp)
                    listOf("${sm.entrenos}", "${sm.asistencias}", "${sm.f}", sm.pct?.let { "${(it * 100).toInt()}%" } ?: "–", "${sm.conv}", "${sm.cuartos}")
                        .forEach { Text(it, Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center) }
                }
            }
        }
        Espacio(40)
        TextButton(onClick = onVolver) { Text("‹ Volver") }
      }
    }
    if (editar) DialogoJugador(repo, id, j.datos, onCerrar = { editar = false }, onGuardado = { editar = false })
    if (fotoMenu) {
        AlertDialog(onDismissRequest = { fotoMenu = false }, title = { Text("Foto de ${j.datos.nombre}") },
            text = { Text("Haz una foto o elígela de la galería.") },
            confirmButton = { TextButton(onClick = { fotoMenu = false; camara.launch(null) }) { Text("Cámara") } },
            dismissButton = { TextButton(onClick = { fotoMenu = false; galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Galería") } })
    }
    if (baja) {
        if (j.datos.baja != null) {
            Confirmar("Reactivar", "¿Quitar la baja de ${j.datos.nombre}?", onSi = {
                repo.guardarJugador(id, j.datos.copy(baja = null, motivo = "")); baja = false
            }, onNo = { baja = false })
        } else {
            var fecha by remember { mutableStateOf(hoy) }
            var motivo by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = { baja = false }, title = { Text("Dar de baja") },
                text = {
                    Column {
                        Text("Desde esta fecha ya no cuenta en sus estadísticas. La ficha se conserva.", fontSize = 13.sp, color = GrisTexto)
                        BotonFecha("Fecha de baja", fecha, ctx) { if (it != null) fecha = it }
                        OutlinedTextField(motivo, { motivo = it }, label = { Text("Motivo (opcional)") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = { repo.guardarJugador(id, j.datos.copy(baja = fecha.toString(), motivo = motivo.trim())); baja = false }) { Text("Dar de baja", color = Rojo) } },
                dismissButton = { TextButton(onClick = { baja = false }) { Text("Cancelar") } })
        }
    }
}
