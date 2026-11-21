package es.collerense.asistencia.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Equipo(
    val club: String = "UD Collerense",
    val equipo: String = "",
    val temporada: String = "",
    val inicio: String = "",
    val fin: String = "",
    val umbral: Double = 0.75,
    val dias: String = "",
    val partido: String = "",
    val entrenador: String = ""
)

@Serializable
data class Horario(val nombre: String, val dia: String, val hora: String, val lugar: String = "") {
    fun texto(): String = if (lugar.isBlank()) hora else "$hora · $lugar"
}

@Serializable
data class JugadorDatos(
    val dorsal: String = "",
    val nombre: String = "",
    val tipo: String = "Plantilla",
    val dni: String = "",
    val nacimiento: String? = null,
    val alta: String? = null,
    val baja: String? = null,
    val motivo: String = "",
    val obs: String = ""
) {
    val invitado: Boolean get() = tipo.equals("Invitado", ignoreCase = true)
}

@Serializable
data class Jugador(
    val id: Int,
    val datos: JugadorDatos,
    val base: JugadorDatos? = null,
    val foto: String? = null,
    val fotoNueva: Boolean = false
) {
    val modificado: Boolean get() = base == null || base != datos || fotoNueva

    fun activoEn(fecha: LocalDate, inicioTemporada: LocalDate?): Boolean {
        val a = datos.alta?.let { parseFecha(it) } ?: inicioTemporada ?: LocalDate.MIN
        val b = datos.baja?.let { parseFecha(it) } ?: LocalDate.MAX
        return !fecha.isBefore(a) && fecha.isBefore(b)
    }

    fun estado(hoy: LocalDate): String {
        val b = datos.baja?.let { parseFecha(it) }
        val a = datos.alta?.let { parseFecha(it) }
        return when {
            b != null && !b.isAfter(hoy) -> "Baja"
            a != null && a.isAfter(hoy) -> "Próxima alta"
            datos.invitado -> "Invitado"
            else -> "Activo"
        }
    }
}

@Serializable
data class SesionDatos(
    val fecha: String,
    val tipo: String = "Entreno",
    val hora: String = "",
    val nota: String = "",
    val resultado: String = "",
    val estado: String = "",
    val registros: Map<String, String> = emptyMap()
) {
    val cancelada: Boolean get() = estado.equals("Cancelada", ignoreCase = true)
    val partido: Boolean get() = tipo.equals("Partido", ignoreCase = true)
    val localDate: LocalDate get() = parseFecha(fecha) ?: LocalDate.MIN
    fun codigo(id: Int): String = registros[id.toString()] ?: ""
}

@Serializable
data class Sesion(
    val uid: String = UUID.randomUUID().toString(),
    val datos: SesionDatos,
    val base: SesionDatos? = null,
    val borrada: Boolean = false
) {
    val modificada: Boolean get() = base == null || base != datos || borrada
}

@Serializable
data class AppData(
    val equipo: Equipo = Equipo(),
    val horarios: List<Horario> = emptyList(),
    val motivos: List<String> = listOf(
        "Suspendido por la FFIB (meteorología)", "Descanso de la plantilla", "Partido aplazado",
        "Partido adelantado", "Partido amistoso", "Festivo", "Instalación no disponible", "Otro"
    ),
    val jugadores: List<Jugador> = emptyList(),
    val sesiones: List<Sesion> = emptyList(),
    val origen: String = "",
    val importado: String = "",
    val ultimoExport: String = "",
    val exportaciones: Int = 0
) {
    val sesionesVivas: List<Sesion> get() = sesiones.filter { !it.borrada }.sortedWith(compareBy({ it.datos.fecha }, { it.datos.partido }))
    val pendientes: Int get() = sesiones.count { it.modificada } + jugadores.count { it.modificado }
    fun jugador(id: Int): Jugador? = jugadores.firstOrNull { it.id == id }
    val inicioTemporada: LocalDate? get() = parseFecha(equipo.inicio)
}

val CODIGOS_ENTRENO = listOf("A", "L", "R", "F", "J")
val CODIGOS_PARTIDO = listOf("NC", "0", "1", "2", "3", "4")

fun significado(codigo: String, partido: Boolean): String = when (codigo) {
    "A" -> "Asiste"
    "L" -> "Asiste lesionado"
    "R" -> "Retraso"
    "F" -> "Falta"
    "J" -> "Falta justificada"
    "NC" -> "No convocado"
    "0" -> "Convocado, no juega"
    "1", "2", "3", "4" -> "$codigo cuarto" + (if (codigo == "1") "" else "s")
    else -> if (partido) "" else ""
}

fun parseFecha(s: String?): LocalDate? {
    if (s.isNullOrBlank()) return null
    return try {
        LocalDate.parse(s.take(10))
    } catch (e: Exception) {
        null
    }
}

val DIAS_CORTOS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
val DIAS_LETRA = listOf("L", "M", "X", "J", "V", "S", "D")
val MESES = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

fun fechaCorta(d: LocalDate): String = "%s %02d/%02d".format(DIAS_CORTOS[d.dayOfWeek.value - 1], d.dayOfMonth, d.monthValue)
fun fechaLarga(d: LocalDate): String = "%02d/%02d/%d".format(d.dayOfMonth, d.monthValue, d.year)
