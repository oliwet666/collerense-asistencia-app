@file:OptIn(ExperimentalMaterial3Api::class)

package es.collerense.asistencia.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import es.collerense.asistencia.R
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
import androidx.compose.material3.darkColorScheme
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

// ── Paleta corporativa (escudo y equipación): rojo, morado, amarillo, sobre fondo oscuro ──
val Fondo = Color(0xFF0B0716)
val Superficie = Color(0xFF161026)
val Superficie2 = Color(0xFF221839)
val Borde = Color(0xFF33265A)
val Texto = Color(0xFFF4F0FF)
val Rojo = Color(0xFFE8202A)
val RojoNeon = Color(0xFFFF4552)
val Morado = Color(0xFF6A2C91)
val MoradoNeon = Color(0xFFB27BFF)
val Amarillo = Color(0xFFFFD200)
val Naranja = Color(0xFFE58A3A)
val Verde = Color(0xFF3BE37F)
val Celeste = Color(0xFF5CC8FF)
/** Acento para textos destacados (nombres, fechas, cifras). */
val Navy = MoradoNeon
val Oro = Naranja
val GrisClaro = Superficie2
val GrisTexto = Color(0xFFA99DC9)

val EsquemaColores = darkColorScheme(
    primary = Rojo, onPrimary = Color.White,
    primaryContainer = Morado, onPrimaryContainer = Color.White,
    secondary = Amarillo, onSecondary = Color(0xFF1A1200),
    tertiary = MoradoNeon, onTertiary = Color(0xFF14002E),
    background = Fondo, onBackground = Texto,
    surface = Superficie, onSurface = Texto,
    surfaceVariant = Superficie2, onSurfaceVariant = GrisTexto,
    surfaceContainer = Superficie, surfaceContainerHigh = Superficie2, surfaceContainerHighest = Superficie2,
    surfaceContainerLow = Superficie, surfaceContainerLowest = Fondo,
    secondaryContainer = Morado, onSecondaryContainer = Amarillo,
    outline = Borde, outlineVariant = Borde, error = RojoNeon
)

/** Colores de cada código (relleno, texto): mismos significados que en el Excel, en versión neón. */
fun colorCodigo(c: String): Pair<Color, Color> = when (c) {
    "A" -> Verde to Color(0xFF03180B)
    "L" -> Celeste to Color(0xFF021826)
    "R" -> Amarillo to Color(0xFF1A1200)
    "F" -> RojoNeon to Color.White
    "J" -> MoradoNeon to Color(0xFF14002E)
    "NC" -> Color(0xFF5B5570) to Color.White
    "0" -> Color(0xFF3A3152) to Color.White
    "1" -> Color(0xFF4A2B80) to Color.White
    "2" -> Color(0xFF6A35B8) to Color.White
    "3" -> Color(0xFF9A3FD0) to Color.White
    "4" -> Rojo to Color.White
    else -> Superficie2 to GrisTexto
}

@Composable
fun ChipCodigo(codigo: String, seleccionado: Boolean, onClick: () -> Unit, ancho: Int = 44) {
    val (bg, fg) = colorCodigo(codigo)
    Box(
        modifier = Modifier
            .padding(2.dp)
            .size(width = ancho.dp, height = 38.dp)
            .clip(CorteDiagonal)
            .background(if (seleccionado) bg else Superficie2)
            .border(BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) Color.White.copy(alpha = .85f) else Borde), CorteDiagonal)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(codigo, fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (seleccionado) fg else GrisTexto)
    }
}

@Composable
fun Etiqueta(texto: String, bg: Color, fg: Color = Color.White) {
    Text(
        texto, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontStyle = FontStyle.Italic,
        modifier = Modifier.clip(CorteDiagonal).background(bg).padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun Tile(titulo: String, valor: String, color: Color = Navy, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(CorteDiagonal)
            .background(Brush.verticalGradient(listOf(Superficie2, Superficie)))
            .border(BorderStroke(1.dp, Brush.horizontalGradient(listOf(color.copy(alpha = .9f), Borde))), CorteDiagonal)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(titulo, fontSize = 9.sp, color = GrisTexto, textAlign = TextAlign.Center, maxLines = 1, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
        Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, color = color, maxLines = 1)
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
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 5.dp, height = 18.dp).background(Brush.verticalGradient(listOf(Amarillo, Rojo))))
        Text(
            texto.uppercase(), color = Texto, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, fontSize = 13.sp, letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
        Box(Modifier.padding(start = 10.dp).weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(MoradoNeon, Color.Transparent))))
    }
}

@Composable
fun Espacio(h: Int = 8) = Box(Modifier.height(h.dp))

@Composable
fun FilaSimple(izq: String, der: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(izq, color = GrisTexto, fontSize = 13.sp)
        Text(der, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Texto)
    }
}

@Composable
fun Tema(content: @Composable () -> Unit) = MaterialTheme(colorScheme = EsquemaColores, shapes = Formas, content = content)

/** Esquina recortada en diagonal: aspecto deportivo/futurista. */
val CorteDiagonal = CutCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomEnd = 0.dp, bottomStart = 10.dp)
val Formas = Shapes(
    extraSmall = CutCornerShape(4.dp), small = CutCornerShape(6.dp), medium = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp),
    large = CutCornerShape(topEnd = 18.dp, bottomStart = 18.dp), extraLarge = CutCornerShape(topEnd = 22.dp, bottomStart = 22.dp)
)

/** Escudo del club. */
@Composable
fun Escudo(alto: Int, modifier: Modifier = Modifier) =
    Image(painterResource(R.drawable.escudo), "Escudo UD Collerense", modifier.height(alto.dp), contentScale = ContentScale.Fit)

/** Franjas "goteadas" como las de la equipación: morado a la izquierda, amarillo a la derecha (patrón fijo). */
@Composable
fun FranjasEquipacion(modifier: Modifier = Modifier, alpha: Float = 1f) {
    Canvas(modifier) {
        val rnd = java.util.Random(1967)
        val n = 46
        for (i in 0 until n) {
            val t = i / (n - 1f)
            val x = size.width * (0.02f + 0.96f * t) + (rnd.nextFloat() - .5f) * 10f
            val ancho = 2f + rnd.nextFloat() * (if (t < .35f) 9f else 6f)
            val largo = size.height * (0.25f + rnd.nextFloat() * (if (t < .35f) .75f else .6f))
            val color = when {
                t < .38f -> Morado
                t < .5f -> if (rnd.nextBoolean()) Morado else Amarillo
                else -> Amarillo
            }
            drawLine(color.copy(alpha = alpha * (0.55f + rnd.nextFloat() * .45f)), Offset(x, -4f), Offset(x - largo * .08f, largo), ancho, StrokeCap.Round)
            if (rnd.nextFloat() < .35f) drawCircle(color.copy(alpha = alpha * .8f), ancho * .6f, Offset(x - largo * .1f, largo + ancho * 2.2f))
        }
    }
}

/** Fondo rojo de la equipación con las franjas encima. */
@Composable
fun FondoEquipacion(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.background(Brush.verticalGradient(listOf(Color(0xFFF0242E), Color(0xFFB5101A))))) {
        FranjasEquipacion(Modifier.matchParentSize(), alpha = .9f)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0x55000000)))))
        content()
    }
}

/** Botón principal: degradado rojo→morado con esquinas cortadas. */
@Composable
fun BotonNeon(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val forma = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)
    Box(
        modifier.height(52.dp).clip(forma)
            .background(if (enabled) Brush.horizontalGradient(listOf(Rojo, Color(0xFFB0186E), Morado)) else Brush.horizontalGradient(listOf(Superficie2, Superficie2)))
            .border(BorderStroke(1.dp, if (enabled) Amarillo.copy(alpha = .7f) else Borde), forma)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(texto.uppercase(), color = if (enabled) Color.White else GrisTexto, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
            letterSpacing = 1.2.sp, fontSize = 15.sp)
    }
}

/** Tarjeta oscura con borde degradado. */
@Composable
fun Panel(modifier: Modifier = Modifier, acento: Color = MoradoNeon, content: @Composable () -> Unit) {
    val forma = CutCornerShape(topEnd = 16.dp, bottomStart = 16.dp)
    Box(
        modifier.clip(forma).background(Brush.linearGradient(listOf(Superficie2, Superficie)))
            .border(BorderStroke(1.dp, Brush.linearGradient(listOf(acento, Borde, Borde))), forma)
    ) { content() }
}
