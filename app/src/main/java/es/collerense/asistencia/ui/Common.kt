@file:OptIn(ExperimentalMaterial3Api::class)

package es.collerense.asistencia.ui

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.collerense.asistencia.data.fechaLarga
import java.time.LocalDate

val Navy = Color(0xFF27438C)
val Rojo = Color(0xFFEE252D)
val Morado = Color(0xFF49298E)
val Oro = Color(0xFFB37D41)
val Amarillo = Color(0xFFFDE926)
val GrisClaro = Color(0xFFF3F4F6)
val GrisTexto = Color(0xFF6B7280)

val EsquemaColores = lightColorScheme(
    primary = Navy, onPrimary = Color.White,
    secondary = Rojo, onSecondary = Color.White,
    tertiary = Oro, surface = Color.White, background = Color.White
)

/** Colores de cada código (relleno, texto), iguales que en el Excel. */
fun colorCodigo(c: String): Pair<Color, Color> = when (c) {
    "A" -> Color(0xFFC6EFCE) to Color(0xFF006100)
    "L" -> Color(0xFFDDEBF7) to Color(0xFF1F4E78)
    "R" -> Color(0xFFFFEB9C) to Color(0xFF9C5700)
    "F" -> Color(0xFFFFC7CE) to Color(0xFF9C0006)
    "J" -> Color(0xFFE4DFEC) to Color(0xFF5B3F86)
    "NC" -> Color(0xFF7F7F7F) to Color.White
    "0" -> Color(0xFFEDEDED) to Color(0xFF404040)
    "1" -> Color(0xFFDCE3F2) to Color(0xFF1F2F5C)
    "2" -> Color(0xFFB4C3E6) to Color(0xFF1F2F5C)
    "3" -> Color(0xFF7A95CF) to Color.White
    "4" -> Navy to Color.White
    else -> GrisClaro to GrisTexto
}

@Composable
fun ChipCodigo(codigo: String, seleccionado: Boolean, onClick: () -> Unit, ancho: Int = 44) {
    val (bg, fg) = colorCodigo(codigo)
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(width = ancho.dp, height = 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (seleccionado) bg else Color.White)
            .border(BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) fg else Color(0xFFD1D5DB)), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(codigo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (seleccionado) fg else GrisTexto)
    }
}

@Composable
fun Etiqueta(texto: String, bg: Color, fg: Color = Color.White) {
    Text(
        texto, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun Tile(titulo: String, valor: String, color: Color = Navy, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(GrisClaro).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(titulo, fontSize = 10.sp, color = GrisTexto, textAlign = TextAlign.Center, maxLines = 1)
        Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

fun elegirFecha(context: Context, inicial: LocalDate, onElegida: (LocalDate) -> Unit) {
    DatePickerDialog(context, { _, y, m, d -> onElegida(LocalDate.of(y, m + 1, d)) }, inicial.year, inicial.monthValue - 1, inicial.dayOfMonth).show()
}

@Composable
fun BotonFecha(etiqueta: String, fecha: LocalDate?, context: Context, permitirVacio: Boolean = false, onCambio: (LocalDate?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(etiqueta, modifier = Modifier.width(110.dp), color = GrisTexto, fontSize = 13.sp)
        OutlinedButton(onClick = { elegirFecha(context, fecha ?: LocalDate.now()) { onCambio(it) } }) {
            Text(fecha?.let { fechaLarga(it) } ?: "—")
        }
        if (permitirVacio && fecha != null) TextButton(onClick = { onCambio(null) }) { Text("Quitar") }
    }
}

@Composable
fun Confirmar(titulo: String, texto: String, si: String = "Sí", no: String = "Cancelar", onSi: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onSi) { Text(si) } },
        dismissButton = { TextButton(onClick = onNo) { Text(no) } }
    )
}

@Composable
fun Aviso(titulo: String, texto: String, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onOk,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onOk) { Text("Aceptar") } }
    )
}

/** Diálogo de cancelación con la lista de motivos. */
@Composable
fun DialogoCancelar(motivos: List<String>, titulo: String, onCancelar: (String) -> Unit, onCerrar: () -> Unit) {
    var sel by remember { mutableStateOf(motivos.firstOrNull() ?: "Otro") }
    var detalle by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(titulo) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                motivos.forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { sel = m }) {
                        RadioButton(selected = sel == m, onClick = { sel = m })
                        Text(m, fontSize = 14.sp)
                    }
                }
                OutlinedTextField(value = detalle, onValueChange = { detalle = it }, label = { Text("Detalle (opcional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = if (sel.equals("Otro", true) && detalle.isNotBlank()) detalle.trim()
                else sel + if (detalle.isNotBlank()) " – " + detalle.trim() else ""
                onCancelar(m)
            }) { Text("Cancelar sesión", color = Rojo) }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Volver") } }
    )
}

@Composable
fun Seccion(texto: String) {
    Text(
        texto.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(6.dp)).background(Navy).padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun Espacio(h: Int = 8) = Box(Modifier.height(h.dp))

@Composable
fun FilaSimple(izq: String, der: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(izq, color = GrisTexto, fontSize = 13.sp)
        Text(der, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun Tema(content: @Composable () -> Unit) = MaterialTheme(colorScheme = EsquemaColores, content = content)
