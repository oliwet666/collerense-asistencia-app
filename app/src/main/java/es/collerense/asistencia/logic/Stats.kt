package es.collerense.asistencia.logic

import es.collerense.asistencia.data.AppData
import es.collerense.asistencia.data.Jugador
import es.collerense.asistencia.data.Sesion
import es.collerense.asistencia.data.parseFecha
import java.time.LocalDate

data class Stats(
    val entrenos: Int = 0,      // posibles (sesiones de entreno con registros en su periodo; invitado: las suyas)
    val a: Int = 0, val l: Int = 0, val r: Int = 0, val f: Int = 0, val j: Int = 0,
    val partidos: Int = 0, val conv: Int = 0, val nc: Int = 0, val jugados: Int = 0, val cuartos: Int = 0
) {
    val asistencias get() = a + l + r
    val pct: Double? get() = if (asistencias + f == 0) null else asistencias.toDouble() / (asistencias + f)
    val pctMin: Double? get() = if (conv == 0) null else cuartos.toDouble() / (4 * conv)
    val mediaCuartos: Double? get() = if (conv == 0) null else cuartos.toDouble() / conv
}

object Calc {
    /** Sesiones que cuentan: no canceladas y con al menos un registro. */
    fun cuenta(s: Sesion) = !s.borrada && !s.datos.cancelada && s.datos.registros.isNotEmpty()

    fun periodo(j: Jugador, d: AppData): Pair<LocalDate, LocalDate> {
        val a = j.datos.alta?.let { parseFecha(it) } ?: d.inicioTemporada ?: LocalDate.MIN
        val b = j.datos.baja?.let { parseFecha(it) } ?: LocalDate.MAX
        return a to b
    }

    fun stats(j: Jugador, d: AppData, desde: LocalDate? = null, hasta: LocalDate? = null): Stats {
        val (a0, b0) = periodo(j, d)
        val a = if (desde != null && desde.isAfter(a0)) desde else a0
        val b = if (hasta != null && hasta.isBefore(b0)) hasta else b0
        var st = Stats()
        for (s in d.sesiones) {
            if (!cuenta(s)) continue
            val f = s.datos.localDate
            if (f.isBefore(a) || !f.isBefore(b)) continue
            val c = s.datos.codigo(j.id)
            if (s.datos.partido) {
                st = st.copy(partidos = st.partidos + 1)
                when {
                    c == "NC" -> st = st.copy(nc = st.nc + 1)
                    c.toIntOrNull() != null -> {
                        val q = c.toInt()
                        st = st.copy(conv = st.conv + 1, cuartos = st.cuartos + q, jugados = st.jugados + if (q >= 1) 1 else 0)
                    }
                }
            } else {
                if (!j.datos.invitado || c.isNotEmpty()) st = st.copy(entrenos = st.entrenos + 1)
                st = when (c) {
                    "A" -> st.copy(a = st.a + 1)
                    "L" -> st.copy(l = st.l + 1)
                    "R" -> st.copy(r = st.r + 1)
                    "F" -> st.copy(f = st.f + 1)
                    "J" -> st.copy(j = st.j + 1)
                    else -> st
                }
            }
        }
        return st
    }

    /** Meses de la temporada (primer día de cada mes). */
    fun meses(d: AppData): List<LocalDate> {
        val ini = d.inicioTemporada?.withDayOfMonth(1) ?: return emptyList()
        val fin = parseFecha(d.equipo.fin) ?: ini.plusMonths(10)
        val out = mutableListOf<LocalDate>()
        var m = ini
        while (!m.isAfter(fin) && out.size < 14) { out += m; m = m.plusMonths(1) }
        return out
    }

    /** Invitado que juega sin entreno (A/L/R) esa semana antes del partido. */
    fun invitadoSinEntreno(j: Jugador, partido: Sesion, d: AppData): Boolean {
        if (!j.datos.invitado || !partido.datos.partido) return false
        if (partido.datos.codigo(j.id).toIntOrNull() == null) return false
        val f = partido.datos.localDate
        val lunes = f.minusDays((f.dayOfWeek.value - 1).toLong())
        return d.sesiones.none { s ->
            !s.borrada && !s.datos.partido && !s.datos.localDate.isBefore(lunes) && s.datos.localDate.isBefore(f) &&
                s.datos.codigo(j.id) in setOf("A", "L", "R")
        }
    }

    /** Últimas n sesiones (más recientes primero) en el periodo del jugador. */
    fun ultimas(j: Jugador, d: AppData, n: Int = 10, hoy: LocalDate = LocalDate.now()): List<Sesion> {
        val (a, b) = periodo(j, d)
        return d.sesiones.filter {
            cuenta(it) && !it.datos.localDate.isBefore(a) && it.datos.localDate.isBefore(b) && !it.datos.localDate.isAfter(hoy) &&
                (!j.datos.invitado || it.datos.codigo(j.id).isNotEmpty())
        }.sortedByDescending { it.datos.fecha }.take(n)
    }
}
