package es.collerense.asistencia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.collerense.asistencia.data.Repo
import es.collerense.asistencia.logic.Calc
import java.time.LocalDate

@Composable
fun PantallaResumen(repo: Repo, onFicha: (Int) -> Unit) {
    val d = repo.data
    val hoy = LocalDate.now()
    val filas = d.jugadores.map { it to Calc.stats(it, d) }
        .sortedWith(compareBy({ it.first.estado(hoy) == "Baja" }, { it.first.datos.invitado }, { -(it.second.pct ?: -1.0) }))
    val plantilla = filas.filter { !it.first.datos.invitado }
    val asis = plantilla.sumOf { it.second.asistencias }
    val fal = plantilla.sumOf { it.second.f }
    val pctEq = if (asis + fal == 0) null else asis.toDouble() / (asis + fal)
    val entrenos = d.sesionesVivas.count { Calc.cuenta(it) && !it.datos.partido }
    val partidos = d.sesionesVivas.count { Calc.cuenta(it) && it.datos.partido }
    val alertas = plantilla.count { it.first.estado(hoy) == "Activo" && (it.second.pct ?: 1.0) < d.equipo.umbral }

    Column(Modifier.fillMaxSize()) {
        Text(
            "${d.equipo.equipo} · ${d.equipo.temporada}".trim(' ', '·'), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy,
            modifier = Modifier.padding(start = 12.dp, top = 10.dp)
        )
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tile("ACTIVOS", "${d.jugadores.count { it.estado(hoy) == "Activo" }}", Navy, Modifier.weight(1f))
            Tile("ENTRENOS", "$entrenos", Navy, Modifier.weight(1f))
            Tile("PARTIDOS", "$partidos", Oro, Modifier.weight(1f))
            Tile("% EQUIPO", pctEq?.let { "${(it * 100).toInt()}%" } ?: "–", Color(0xFF15803D), Modifier.weight(1f))
            Tile("ALERTAS", "$alertas", Rojo, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().background(Navy).padding(vertical = 6.dp, horizontal = 8.dp)) {
            listOf("Jugador" to 3.2f, "%" to 1f, "Falt" to 0.9f, "Conv" to 0.9f, "¼" to 0.8f, "%Min" to 1f).forEach { (t, w) ->
                Text(t, Modifier.weight(w), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = if (w > 3) TextAlign.Start else TextAlign.Center)
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(filas, key = { it.first.id }) { (j, st) ->
                val baja = j.estado(hoy) == "Baja"
                val alerta = !j.datos.invitado && !baja && (st.pct ?: 1.0) < d.equipo.umbral
                Row(
                    Modifier.fillMaxWidth().clickable { onFicha(j.id) }.background(if (alerta) Color(0xFFFFF1F2) else Color.Transparent)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                ) {
                    Text(
                        (if (j.datos.dorsal.isNotEmpty()) j.datos.dorsal + " " else "") + j.datos.nombre, Modifier.weight(3.2f), fontSize = 12.sp, maxLines = 1,
                        fontWeight = FontWeight.SemiBold, color = if (baja) GrisTexto else Color.Black,
                        fontStyle = if (j.datos.invitado || baja) FontStyle.Italic else FontStyle.Normal
                    )
                    val c = if (baja) GrisTexto else Color.Black
                    Text(if (j.datos.invitado) "inv." else st.pct?.let { "${(it * 100).toInt()}%" } ?: "–", Modifier.weight(1f), fontSize = 12.sp,
                        textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = if (alerta) Rojo else Navy)
                    Text("${st.f}", Modifier.weight(0.9f), fontSize = 12.sp, textAlign = TextAlign.Center, color = c)
                    Text("${st.conv}", Modifier.weight(0.9f), fontSize = 12.sp, textAlign = TextAlign.Center, color = c)
                    Text("${st.cuartos}", Modifier.weight(0.8f), fontSize = 12.sp, textAlign = TextAlign.Center, color = c)
                    Text(st.pctMin?.let { "${(it * 100).toInt()}%" } ?: "–", Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center, color = Oro)
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
            }
        }
    }
}
